/*
 * Copyright (c) 2023 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

import javax.security.auth.login.FailedLoginException;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common.AudioCrosspointGainCommand;
import com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common.AudioInputEnum;
import com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common.EnumTypeHandler;
import com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common.NetworkInformation;
import com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common.StreamSettings;
import com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common.VaddioCommand;
import com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common.VaddioNanoConstant;
import com.avispl.symphony.dal.communicator.SshCommunicator;
import com.avispl.symphony.dal.util.StringUtils;

/**
 * VaddioBridgeNanoCommunicator An implementation of SshCommunicator to provide communication and interaction with Vaddio Bridge Nano device
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/14/2023
 * @since 1.0.0
 */
public class VaddioBridgeNanoCommunicator extends SshCommunicator implements Monitorable, Controller {

	/**
	 * cache to store key and value
	 */
	Map<String, String> cacheKeyAndValue = new HashMap<>();

	/**
	 * count the failed command
	 */
	private final Map<String, String> failedMonitor = new HashMap<>();

	/**
	 * Prevent case where {@link VaddioBridgeNanoCommunicator#controlProperty(ControllableProperty)} slow down -
	 * the getMultipleStatistics interval if it's fail to doGet the cmd
	 */
	private static final int controlSSHTimeout = 3000;
	/**
	 * Set back to default timeout value in {@link SshCommunicator}
	 */
	private static final int statisticsSSHTimeout = 30000;

	/**
	 * ReentrantLock to prevent telnet session is closed when adapter is retrieving statistics from the device.
	 */
	private final ReentrantLock reentrantLock = new ReentrantLock();

	/**
	 * Store previous/current ExtendedStatistics
	 */
	private ExtendedStatistics localExtendedStatistics;

	/**
	 * To check controlling process
	 */
	private boolean isEmergencyDelivery;

	/**
	 * Constructor for VaddioBridgeNanoCommunicator class
	 */
	public VaddioBridgeNanoCommunicator() {
		this.setCommandErrorList(Collections.singletonList("Error: response error"));
		this.setCommandSuccessList(Collections.singletonList("> "));
		this.setLoginSuccessList(Collections.singletonList("********************************************\r\n        \r\nWelcome admin\r\n> "));
		this.setLoginErrorList(Collections.singletonList("Permission denied, please try again."));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Statistics> getMultipleStatistics() throws Exception {
		ExtendedStatistics extendedStatistics = new ExtendedStatistics();
		List<AdvancedControllableProperty> advancedControllableProperty = new ArrayList<>();
		Map<String, String> stats = new HashMap<>();
		reentrantLock.lock();
		try {
			if (!isEmergencyDelivery) {
				retrieveMonitoring();
				populateMonitoringAndControlling(stats, advancedControllableProperty);
				populateCrosspointGain(stats, advancedControllableProperty);
				populateAudioInput(stats, advancedControllableProperty);
				extendedStatistics.setStatistics(stats);
				extendedStatistics.setControllableProperties(advancedControllableProperty);
				localExtendedStatistics = extendedStatistics;
			}
		} finally {
			reentrantLock.unlock();
		}
		return Collections.singletonList(localExtendedStatistics);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalDestroy() {
		if (localExtendedStatistics != null && localExtendedStatistics.getStatistics() != null && localExtendedStatistics.getControllableProperties() != null) {
			localExtendedStatistics.getStatistics().clear();
			localExtendedStatistics.getControllableProperties().clear();
		}
		super.internalDestroy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void controlProperty(ControllableProperty controllableProperty) {
		reentrantLock.lock();
		try {
			this.timeout = controlSSHTimeout;
			if (localExtendedStatistics == null || localExtendedStatistics.getStatistics().isEmpty() || localExtendedStatistics.getControllableProperties().isEmpty()) {
				return;
			}
			isEmergencyDelivery = true;
			Map<String, String> stats = this.localExtendedStatistics.getStatistics();
			List<AdvancedControllableProperty> advancedControllableProperties = this.localExtendedStatistics.getControllableProperties();
			String value = String.valueOf(controllableProperty.getValue());
			String property = controllableProperty.getProperty();
			String keyName = property;
			String groupName = property;

			boolean isCrosspoitControl = handleCrosspoitGainControl(stats, advancedControllableProperties, value, property);
			if (isCrosspoitControl) {
				return;
			}
			if (property.contains(VaddioNanoConstant.HASH)) {
				String[] group = property.split(VaddioNanoConstant.HASH);
				groupName = group[0];
				keyName = group[1];
			}
			String group = AudioInputEnum.getValueByName(groupName);
			if (StringUtils.isNullOrEmpty(group)) {
				group = AudioCrosspointGainCommand.getValueByName(AudioCrosspointGainCommand.class, groupName);
			}
			switch (keyName) {
				case "VideoMute":
					String videoMute = VaddioNanoConstant.OFF;
					if (String.valueOf(VaddioNanoConstant.NUMBER_ONE).equalsIgnoreCase(value)) {
						videoMute = VaddioNanoConstant.ON;
					}
					controlVideoMute(group, videoMute.toLowerCase(Locale.ROOT));
					break;
				case "Mute":
					String muteValue = VaddioNanoConstant.OFF;
					if (String.valueOf(VaddioNanoConstant.NUMBER_ONE).equalsIgnoreCase(value)) {
						muteValue = VaddioNanoConstant.ON;
					}
					controlMute(group, muteValue.toLowerCase(Locale.ROOT));
					break;
				case "Volume(dB)":
					controlVolume(group, value);
					stats.put(groupName + VaddioNanoConstant.HASH + VaddioNanoConstant.VOLUME_CURRENT_VALUE, value);
					break;
				case "StreamingMode":
					String streamMode = VaddioNanoConstant.USB;
					if (String.valueOf(VaddioNanoConstant.NUMBER_ONE).equalsIgnoreCase(value)) {
						streamMode = VaddioNanoConstant.IP;
					}
					controlStreamingMode(groupName, streamMode.toLowerCase(Locale.ROOT));
					break;
				case "SystemReboot":
					controlSystemReboot(groupName, value, true);
					break;
				case "SystemRebootDelay(s)":
					controlSystemReboot(groupName, value, false);
					break;
				default:
					logger.debug("the property doesn't support" + keyName);
					break;
			}
			updateLocalControlValue(stats, advancedControllableProperties, property, value);
		} finally {
			this.timeout = statisticsSSHTimeout;
			reentrantLock.unlock();
		}

	}

	private boolean handleCrosspoitGainControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String value, String property) {
		if (!property.contains(VaddioNanoConstant.CROSSPOINT_GAIN)) {
			return false;
		}

		String[] group = property.split(VaddioNanoConstant.HASH);
		String groupName = group[0].replace(VaddioNanoConstant.CROSSPOINT_GAIN, VaddioNanoConstant.EMPTY);
		String keyName = group[1].replace(VaddioNanoConstant.GAIN, VaddioNanoConstant.EMPTY);
		String command = AudioCrosspointGainCommand.getCommandByValue(groupName) + AudioInputEnum.getValueByName(keyName) + AudioCrosspointGainCommand.SET + value;
		controlCrosspointGain(command, group[1]);
		updateLocalControlValue(stats, advancedControllableProperties, property, value);
		stats.put(group[0] + VaddioNanoConstant.HASH + keyName + VaddioNanoConstant.GAIN_CURRENT_VALUE, value);
		return true;
	}

	/**
	 * Updates cached devices' control value, after the control command was executed with the specified value.
	 * It is done in order for aggregator to populate the latest control values, after the control command has been executed,
	 * but before the next devices details polling cycle was addressed.
	 *
	 * @param stats The updated device properties.
	 * @param advancedControllableProperties The updated list of advanced controllable properties.
	 * @param name of the control property
	 * @param value to set to the control property
	 */
	private void updateLocalControlValue(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String name, String value) {
		stats.put(name, value);
		advancedControllableProperties.stream().filter(advancedControllableProperty ->
				name.equals(advancedControllableProperty.getName())).findFirst().ifPresent(advancedControllableProperty ->
				advancedControllableProperty.setValue(value));
	}

	/**
	 * Control mute
	 *
	 * @param groupName the groupName is name of command
	 * @param value the value is value to send the command
	 */
	private void controlMute(String groupName, String value) {
		try {
			String command = AudioCrosspointGainCommand.AUDIO_COMMAND + groupName + VaddioNanoConstant.SPACE + AudioCrosspointGainCommand.MUTE.trim() + VaddioNanoConstant.SPACE + value;
			String response = send(command.contains("\r") ? command : command.concat("\r"));
			if (StringUtils.isNullOrEmpty(response) || response.contains(VaddioNanoConstant.ERROR_RESPONSE)) {
				throw new IllegalArgumentException(String.format("Error when control mute, Syntax error command: %s", response));
			}
		} catch (Exception e) {
			throw new IllegalArgumentException(String.format("Can't control %s with %s value.", groupName, value), e);
		}
	}

	/**
	 * Control mute
	 *
	 * @param groupName the groupName is name of command
	 * @param value the value is value to send the command
	 */
	private void controlVideoMute(String groupName, String value) {
		try {
			String command = VaddioCommand.VIDEO_COMMAND + value;
			String response = send(command.contains("\r") ? command : command.concat("\r"));
			if (StringUtils.isNullOrEmpty(response) || response.contains(VaddioNanoConstant.ERROR_RESPONSE)) {
				throw new IllegalArgumentException(String.format("Error when control mute, Syntax error command: %s", response));
			}
		} catch (Exception e) {
			throw new IllegalArgumentException(String.format("Can't control %s with %s value.", groupName, value), e);
		}
	}

	/**
	 * Control volume
	 *
	 * @param groupName the groupName is name of command
	 * @param value the value is value to send the command
	 */
	private void controlVolume(String groupName, String value) {
		try {
			String command = AudioCrosspointGainCommand.AUDIO_COMMAND + groupName + VaddioNanoConstant.SPACE + AudioCrosspointGainCommand.VOLUME.trim() + AudioCrosspointGainCommand.SET + value;
			String response = send(command.contains("\r") ? command : command.concat("\r"));
			if (StringUtils.isNullOrEmpty(response) || response.contains(VaddioNanoConstant.ERROR_RESPONSE)) {
				throw new IllegalArgumentException(String.format("Error when control mute, Syntax error command: %s", response));
			}
		} catch (Exception e) {
			throw new IllegalArgumentException(String.format("Can't control %s with %s value.", groupName, value), e);
		}
	}

	/**
	 * Control volume
	 *
	 * @param command the command is command to control device
	 * @param propertyName the propertyName is name of command
	 */
	private void controlCrosspointGain(String command, String propertyName) {
		try {
			String response = send(command.contains("\r") ? command : command.concat("\r"));
			if (StringUtils.isNullOrEmpty(response) || response.contains(VaddioNanoConstant.ERROR_RESPONSE)) {
				throw new IllegalArgumentException(String.format("Error when control crosspoint gain, Syntax error command: %s", response));
			}
		} catch (Exception e) {
			throw new IllegalArgumentException(String.format("Can't control %s.", propertyName), e);
		}
	}

	/**
	 * Control StreamingMode
	 *
	 * @param groupName the groupName is name of command
	 * @param value the value is value to send the command
	 */
	private void controlStreamingMode(String groupName, String value) {
		try {
			String command = VaddioCommand.STREAMING_MODE + value;
			String response = send(command.contains("\r") ? command : command.concat("\r"));
			if (StringUtils.isNullOrEmpty(response) || response.contains(VaddioNanoConstant.ERROR_RESPONSE)) {
				throw new IllegalArgumentException(String.format("Error when control streaming mode, Syntax error command: %s", response));
			}
		} catch (Exception e) {
			throw new IllegalArgumentException(String.format("Can't control %s with %s value.", groupName, value), e);
		}
	}

	/**
	 * Control StreamingMode
	 *
	 * @param groupName the groupName is name of command
	 * @param isRebootTime is boolean type (true/false) if reboot time delay
	 */
	private void controlSystemReboot(String groupName, String value, boolean isRebootTime) {
		try {
			String command = VaddioCommand.SYSTEM_REBOOT;
			if (isRebootTime) {
				command = command + VaddioNanoConstant.SPACE + value;
			}
			String response = send(command.contains("\r") ? command : command.concat("\r"));
			if (StringUtils.isNullOrEmpty(response) || response.contains(VaddioNanoConstant.ERROR_RESPONSE)) {
				throw new IllegalArgumentException(String.format("Error when control streaming mode, Syntax error command: %s", response));
			}
		} catch (Exception e) {
			throw new IllegalArgumentException(String.format("Can't control %s with %s value.", groupName, value), e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void controlProperties(List<ControllableProperty> list) throws Exception {
		if (CollectionUtils.isEmpty(list)) {
			throw new IllegalArgumentException("ControllableProperties can not be null or empty");
		}
		for (ControllableProperty p : list) {
			try {
				controlProperty(p);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Populate Audio input detail
	 *
	 * @param stats the stats are list of Statistics
	 * @param advancedControllableProperty the advancedControllableProperty are AdvancedControllableProperty instance
	 */
	private void populateAudioInput(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperty) {
		for (AudioInputEnum audioInputEnum : AudioInputEnum.values()) {
			String key = audioInputEnum.getPropertyName() + VaddioNanoConstant.HASH + VaddioNanoConstant.VOLUME;
			String data = StringUtils.isNullOrEmpty(cacheKeyAndValue.get(key)) ? VaddioNanoConstant.NONE : cacheKeyAndValue.get(key);
			switch (audioInputEnum) {
				case HDMI_IN_RIGHT:
				case HDMI_IN_LEFT:
				case LINE_IN_LEFT:
				case LINE_IN_RIGHT:
				case USB_PLAYBACK_LEFT:
				case USB_PLAYBACK_RIGHT:
					data = extractValue(data, VaddioNanoConstant.VOLUME_REGEX).split(VaddioNanoConstant.SPACE)[0];
					populateVolumeControl(stats, advancedControllableProperty, audioInputEnum.getPropertyName(), key, data);
					populateMuteControl(stats, advancedControllableProperty, audioInputEnum.getPropertyName() + VaddioNanoConstant.HASH + VaddioNanoConstant.MUTE);
					break;
				default:
					logger.debug(String.format("the command %s doesn't support", audioInputEnum.getName()));
					break;
			}
		}
	}

	/**
	 * Populate Mute control detail
	 *
	 * @param stats the stats are list of Statistics
	 * @param advancedControllableProperty the advancedControllableProperty are AdvancedControllableProperty instance
	 * @param key the key is group of properties
	 */
	private void populateMuteControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperty, String key) {
		String data = StringUtils.isNullOrEmpty(cacheKeyAndValue.get(key)) ? VaddioNanoConstant.NONE : cacheKeyAndValue.get(key);
		stats.put(key, VaddioNanoConstant.EMPTY);
		int value = 0;
		if (VaddioNanoConstant.ON.equalsIgnoreCase(extractValue(data, VaddioNanoConstant.MUTE_REGEX))) {
			value = 1;
		}
		AdvancedControllableProperty videoMuteControl = createSwitch(key, value, VaddioNanoConstant.OFF, VaddioNanoConstant.ON);
		advancedControllableProperty.add(videoMuteControl);
	}

	/**
	 * Populate Volume control detail
	 *
	 * @param stats the stats are list of Statistics
	 * @param advancedControllableProperty the advancedControllableProperty are AdvancedControllableProperty instance
	 * @param groupName the groupName is group of properties
	 * @param key the key is key name
	 * @param data the data is data of properties
	 */
	private void populateVolumeControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperty, String groupName, String key, String data) {
		stats.put(key, VaddioNanoConstant.EMPTY);
		float volumeValue = Float.parseFloat(data);
		String volumeCurrentKey = groupName + VaddioNanoConstant.HASH + VaddioNanoConstant.VOLUME_CURRENT_VALUE;
		stats.put(volumeCurrentKey, String.valueOf(volumeValue));

		float minVolume = VaddioNanoConstant.MIN_VOLUME_LINE;
		if (key.contains(AudioInputEnum.LINE_IN_LEFT.getPropertyName()) || key.contains(AudioInputEnum.LINE_IN_RIGHT.getPropertyName()) ||
				key.contains(AudioCrosspointGainCommand.GAIN_LINE_OUT_LEFT.getName()) || key.contains(AudioCrosspointGainCommand.GAIN_LINE_OUT_RIGHT.getName())) {
			minVolume = VaddioNanoConstant.MIN_VOLUME;
		}
		AdvancedControllableProperty volumeControl = createSlider(stats, key, String.valueOf(minVolume), String.valueOf(VaddioNanoConstant.MAX_VOLUME),
				minVolume, VaddioNanoConstant.MAX_VOLUME, volumeValue);
		advancedControllableProperty.add(volumeControl);
	}

	/**
	 * Populate Crosspoint Gain details
	 *
	 * @param stats the stats are list of Statistics
	 * @param advancedControllableProperty the advancedControllableProperty are AdvancedControllableProperty instance
	 */
	private void populateCrosspointGain(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperty) {
		String key;
		for (AudioCrosspointGainCommand command : AudioCrosspointGainCommand.values()) {
			String volume = command.getName() + VaddioNanoConstant.HASH + VaddioNanoConstant.VOLUME;
			String data = StringUtils.isNullOrEmpty(cacheKeyAndValue.get(volume)) ? VaddioNanoConstant.NONE : cacheKeyAndValue.get(volume);
			switch (command) {
				case GAIN_HDMI_OUT_LEFT:
				case GAIN_HDMI_OUT_RIGHT:
				case GAIN_IP_OUT_LEFT:
				case GAIN_IP_OUT_RIGHT:
				case GAIN_LINE_OUT_LEFT:
				case GAIN_LINE_OUT_RIGHT:
				case GAIN_RECORD_OUT_LEFT:
				case GAIN_RECORD_OUT_RIGHT:
					data = extractValue(data, VaddioNanoConstant.VOLUME_REGEX).split(VaddioNanoConstant.SPACE)[0];
					populateVolumeControl(stats, advancedControllableProperty, command.getName(), volume, data);
					populateMuteControl(stats, advancedControllableProperty, command.getName() + VaddioNanoConstant.HASH + VaddioNanoConstant.MUTE);
					String crosspointGainGroup = VaddioNanoConstant.CROSSPOINT_GAIN + command.getName() + VaddioNanoConstant.HASH;
					for (AudioInputEnum audioInputEnum : AudioInputEnum.values()) {
						switch (audioInputEnum) {
							case USB_PLAYBACK_LEFT:
							case USB_PLAYBACK_RIGHT:
							case HDMI_IN_LEFT:
							case HDMI_IN_RIGHT:
							case LINE_IN_LEFT:
							case LINE_IN_RIGHT:
								key = crosspointGainGroup + audioInputEnum.getPropertyName() + VaddioNanoConstant.GAIN;
								String crosspointGainCurrentKey = crosspointGainGroup + audioInputEnum.getPropertyName() + VaddioNanoConstant.GAIN_CURRENT_VALUE;
								data = StringUtils.isNullOrEmpty(cacheKeyAndValue.get(key)) ? VaddioNanoConstant.NONE : cacheKeyAndValue.get(key);
								if (VaddioNanoConstant.NONE.equalsIgnoreCase(data)) {
									stats.put(key, VaddioNanoConstant.NONE);
									break;
								}
								populateGainControl(stats, advancedControllableProperty, key, crosspointGainCurrentKey, parseResponseByCommandGain(data));
								break;
							default:
								break;
						}
					}
					key = VaddioNanoConstant.CROSSPOINT_GAIN + command.getName() + VaddioNanoConstant.HASH + VaddioNanoConstant.ENABLED_ROUTES;
					data = StringUtils.isNullOrEmpty(cacheKeyAndValue.get(key)) ? VaddioNanoConstant.NONE : cacheKeyAndValue.get(key);
					data = parseResponseByCommandGain(data).replace("]", VaddioNanoConstant.EMPTY);
					if (StringUtils.isNullOrEmpty(data) || VaddioNanoConstant.NONE.equals(data)) {
						stats.put(key, VaddioNanoConstant.NONE);
						break;
					}
					List<String> itemList = Arrays.stream(data.split(VaddioNanoConstant.SPACE))
							.map(audioValue -> EnumTypeHandler.getNameByValue(AudioInputEnum.class, audioValue.trim()))
							.collect(Collectors.toList());
					String route = itemList.isEmpty() ? VaddioNanoConstant.NONE : String.join(", ", itemList);
					stats.put(key, route);
					break;
				default:
					logger.debug(String.format("the command %s doesn't support", command.getName()));
					break;
			}
		}
	}

	/**
	 * Populate crosspoint gain control
	 *
	 * @param stats the stats are list of Statistics
	 * @param advancedControllableProperty the advancedControllableProperty are AdvancedControllableProperty instance
	 * @param key the key is key name off properties
	 * @param currentKey the currentKey is current key name
	 * @param value the value is value of gain
	 */
	private void populateGainControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperty, String key, String currentKey, String value) {
		stats.put(key, VaddioNanoConstant.EMPTY);
		float crosspointGainValue = Float.parseFloat(value);
		AdvancedControllableProperty crosspointGain = createSlider(stats, key, String.valueOf(VaddioNanoConstant.MIN_GAIN), String.valueOf(VaddioNanoConstant.MAX_GAIN),
				VaddioNanoConstant.MIN_GAIN, VaddioNanoConstant.MAX_GAIN, crosspointGainValue);
		advancedControllableProperty.add(crosspointGain);
		stats.put(currentKey, String.valueOf(crosspointGainValue));
	}

	/**
	 * Populate monitoring and controlling data
	 *
	 * @param stats the stats are list of statistics
	 * @param advancedControllableProperty the advancedControllableProperty are AdvancedControllableProperty instance
	 */
	private void populateMonitoringAndControlling(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperty) {
		for (VaddioCommand command : VaddioCommand.values()) {
			String key = command.getName();
			String data = StringUtils.isNullOrEmpty(cacheKeyAndValue.get(command.getName())) ? VaddioNanoConstant.NONE : cacheKeyAndValue.get(command.getName());
			switch (command) {
				case VIDEO_MUTE:
					data = parseResponseByCommand(data);
					stats.put(key, VaddioNanoConstant.EMPTY);
					int value = VaddioNanoConstant.ON.equalsIgnoreCase(data) ? 1 : 0;
					AdvancedControllableProperty videoMuteControl = createSwitch(key, value, VaddioNanoConstant.OFF, VaddioNanoConstant.ON);
					advancedControllableProperty.add(videoMuteControl);
					break;
				case NETWORK_INFO:
					populateNetworkSettings(data, stats);
					break;
				case STREAM_SETTINGS:
					populateStreamingSettings(data, stats);
					break;
				case STREAM_MODE:
					boolean isIPStreaming = cacheKeyAndValue.get(VaddioCommand.STREAM_MODE.getName()).contains(VaddioNanoConstant.IP_STREAM_MODE);
					int streamValue = isIPStreaming ? 1 : 0;
					stats.put(key, VaddioNanoConstant.EMPTY);
					AdvancedControllableProperty streamModeControl = createSwitch(key, streamValue, VaddioNanoConstant.USB, VaddioNanoConstant.IP);
					advancedControllableProperty.add(streamModeControl);
					break;
				case VERSION:
					String audioVersion = extractValue(data, VaddioNanoConstant.AUDIO_REGEX);
					String systemVersion = extractValue(data, VaddioNanoConstant.SYSTEM_VERSION_REGEX);
					stats.put(VaddioNanoConstant.AUDIO_VERSION, StringUtils.isNullOrEmpty(audioVersion) ? VaddioNanoConstant.NONE : audioVersion);
					stats.put(key, StringUtils.isNullOrEmpty(systemVersion) ? VaddioNanoConstant.NONE : systemVersion);
					break;
				default:
					logger.debug(String.format("the command %s doesn't support", command.getName()));
					break;
			}
			stats.put(VaddioNanoConstant.SYSTEM_REBOOT, VaddioNanoConstant.EMPTY);
			advancedControllableProperty.add(createButton(VaddioNanoConstant.SYSTEM_REBOOT, VaddioNanoConstant.REBOOT, VaddioNanoConstant.REBOOTING, 0L));

			stats.put(VaddioNanoConstant.SYSTEM_REBOOT_DELAY, VaddioNanoConstant.EMPTY);
			AdvancedControllableProperty systemRebootDelay = createNumeric(VaddioNanoConstant.SYSTEM_REBOOT_DELAY, "30");
			advancedControllableProperty.add(systemRebootDelay);
		}
	}

	/**
	 * Populates streaming settings based on the device response.
	 *
	 * @param response the response from the device
	 * @param stats the map containing statistics
	 */
	private void populateStreamingSettings(String response, Map<String, String> stats) {
		try {
			boolean isIPStreaming = cacheKeyAndValue.get(VaddioCommand.STREAM_MODE.getName()).contains(VaddioNanoConstant.IP_STREAM_MODE);
			for (StreamSettings streamSettings : StreamSettings.values()) {
				String key = VaddioNanoConstant.STREAMING_SETTINGS + VaddioNanoConstant.HASH + streamSettings.getName();
				if (isIPStreaming && streamSettings.isIPStreaming()) {
					String value = extractValue(response, streamSettings.getValue());
					switch (streamSettings) {
						case IP_VIDEO_QUALITY:
							handleIPVideoQuality(value, stats);
							break;
						case IP_PROTOCOL:
							handleIPProtocol(value, stats);
							break;
						default:
							break;
					}
					stats.put(key, value);
					continue;
				}
				if (!isIPStreaming && !streamSettings.isIPStreaming()) {
					stats.put(key, extractValue(response, streamSettings.getValue()));
				}
			}
		} catch (Exception e) {
			setDefaultNetworkInformation(stats);
		}
	}

	/**
	 * Handles the IP video quality setting.
	 *
	 * @param value the value of the IP video quality setting
	 * @param stats the stats are list of statistics
	 */
	private void handleIPVideoQuality(String value, Map<String, String> stats) {
		String group = VaddioNanoConstant.STREAMING_SETTINGS + VaddioNanoConstant.HASH;
		if (VaddioNanoConstant.CUSTOM.equalsIgnoreCase(value)) {
			stats.remove(group + VaddioNanoConstant.IP_PRESET_RESOLUTION);
		} else {
			stats.remove(group + VaddioNanoConstant.IP_MAX_BANDWIDTH);
			stats.remove(group + VaddioNanoConstant.IP_CUSTOM_RESOLUTION);
		}
		stats.put(group + StreamSettings.IP_VIDEO_QUALITY.getName(), value);
	}

	/**
	 * Handles the IP protocol setting.
	 *
	 * @param value the value of the IP protocol setting
	 * @param stats the stats are list of statistics
	 */
	private void handleIPProtocol(String value, Map<String, String> stats) {
		String group = VaddioNanoConstant.STREAMING_SETTINGS + VaddioNanoConstant.HASH;
		if (VaddioNanoConstant.FALSE.equalsIgnoreCase(value)) {
			stats.remove(group + VaddioNanoConstant.IP_RTMP_PORT);
			stats.remove(group + VaddioNanoConstant.IP_RTMP_SERVICE);
		} else {
			stats.remove(group + VaddioNanoConstant.IP_RTSP_PORT);
			stats.remove(group + VaddioNanoConstant.IP_RTSP_URL);
			stats.remove(group + VaddioNanoConstant.IP_RTSP_MTU);
		}
		stats.put(group + StreamSettings.IP_PROTOCOL.getName(), value);
	}

	/**
	 * Sets default network information in case of an exception.
	 *
	 * @param stats the stats are list of statistics
	 */
	private void setDefaultNetworkInformation(Map<String, String> stats) {
		for (NetworkInformation networkInfo : NetworkInformation.values()) {
			stats.put(VaddioNanoConstant.STREAMING_SETTINGS + networkInfo.getName(), VaddioNanoConstant.NONE);
		}
	}


	/**
	 * Populate network information
	 *
	 * @param response the response is response of the device
	 * @param stats the stats are list of statistics
	 */
	private void populateNetworkSettings(String response, Map<String, String> stats) {
		try {
			for (NetworkInformation network : NetworkInformation.values()) {
				stats.put(VaddioNanoConstant.NETWORK_SETTINGS + VaddioNanoConstant.HASH + network.getName(), extractValue(response, network.getValue()));
			}
		} catch (Exception e) {
			for (NetworkInformation network : NetworkInformation.values()) {
				stats.put(VaddioNanoConstant.NETWORK_SETTINGS + VaddioNanoConstant.HASH + network.getName(), VaddioNanoConstant.NONE);
			}
		}
	}

	/**
	 * Extract value received from device
	 *
	 * @param response the response is response of device
	 * @param regex the regex is regex to extract the response value
	 * @return String is value of the device
	 */
	private static String extractValue(String response, String regex) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(response);

		if (matcher.find()) {
			return matcher.group(1).trim();
		}

		return VaddioNanoConstant.NONE;
	}

	/**
	 * Retrieve monitoring data
	 *
	 * @throws FailedLoginException if get the FailedLoginException
	 */
	private void retrieveMonitoring() throws FailedLoginException {
		for (VaddioCommand command : VaddioCommand.values()) {
			sendCommandDetails(command.getCommand(), command.getName());
		}
		for (AudioCrosspointGainCommand command : AudioCrosspointGainCommand.values()) {
			String group = command.getName() + VaddioNanoConstant.HASH;
			String commandItem = command.getCommand();
			sendCommandDetails(commandItem + AudioCrosspointGainCommand.MUTE_COMMAND, group + VaddioNanoConstant.MUTE);
			sendCommandDetails(commandItem + AudioCrosspointGainCommand.ROUTES_COMMAND, VaddioNanoConstant.CROSSPOINT_GAIN + group + VaddioNanoConstant.ENABLED_ROUTES);
			sendCommandDetails(commandItem + AudioCrosspointGainCommand.VOLUME_COMMAND, group + VaddioNanoConstant.VOLUME);
			for (AudioInputEnum audioInputEnum : AudioInputEnum.values()) {
				group = VaddioNanoConstant.CROSSPOINT_GAIN + command.getName() + VaddioNanoConstant.HASH + audioInputEnum.getPropertyName() + VaddioNanoConstant.GAIN;
				sendCommandDetails(commandItem + AudioCrosspointGainCommand.GAIN_COMMAND + audioInputEnum.getValue() + AudioCrosspointGainCommand.GET, group);
			}
		}
		for (AudioInputEnum audioInputEnum : AudioInputEnum.values()) {
			String group = audioInputEnum.getPropertyName() + VaddioNanoConstant.HASH;
			sendCommandDetails(AudioCrosspointGainCommand.AUDIO_COMMAND + audioInputEnum.getValue() + AudioCrosspointGainCommand.VOLUME_COMMAND, group + VaddioNanoConstant.VOLUME);
			sendCommandDetails(AudioCrosspointGainCommand.AUDIO_COMMAND + audioInputEnum.getValue() + AudioCrosspointGainCommand.MUTE_COMMAND, group + VaddioNanoConstant.MUTE);
		}
	}

	/**
	 * Send command detail to get the data from device
	 *
	 * @param command the command is command to get data
	 * @param group the group is group name of properties
	 * @throws FailedLoginException if authentication fails
	 */
	private void sendCommandDetails(String command, String group) throws FailedLoginException {
		try {
			String response = send(command.contains("\r") ? command : command.concat("\r"));
			cacheKeyAndValue.put(group, response.replaceAll(VaddioNanoConstant.REGEX_RESPONSE, VaddioNanoConstant.EMPTY));
		} catch (FailedLoginException e) {
			throw new FailedLoginException("Login failed: " + e);
		} catch (Exception ex) {
			logger.error(String.format("Error when get command: %s", command), ex);
			failedMonitor.put(command, ex.getMessage());
		}
	}

	/**
	 * Parse response data by command
	 *
	 * @param response the response is response received from device
	 */
	private String parseResponseByCommandGain(String response) {
		if (VaddioNanoConstant.NONE.equalsIgnoreCase(response)) {
			return VaddioNanoConstant.NONE;
		}
		try {
			String value = response.substring(response.indexOf(VaddioNanoConstant.SPACE) + 1);
			String[] arrayValue = value.split("\r\n");
			return arrayValue[1].trim();
		} catch (Exception e) {
			return VaddioNanoConstant.NONE;
		}
	}


	/**
	 * Parse response data by command
	 *
	 * @param response the response is response received from device
	 */
	private String parseResponseByCommand(String response) {
		if (VaddioNanoConstant.NONE.equalsIgnoreCase(response)) {
			return VaddioNanoConstant.NONE;
		}
		try {
			String value = response.substring(response.indexOf(VaddioNanoConstant.SPACE) + 1);
			String[] arrayValue = value.split("\r\n");
			return arrayValue[0];
		} catch (Exception e) {
			return VaddioNanoConstant.NONE;
		}
	}

	/***
	 * Create AdvancedControllableProperty slider instance
	 *
	 * @param stats extended statistics
	 * @param name name of the control
	 * @param initialValue initial value of the control
	 * @return AdvancedControllableProperty slider instance
	 */
	private AdvancedControllableProperty createSlider(Map<String, String> stats, String name, String labelStart, String labelEnd, Float rangeStart, Float rangeEnd, Float initialValue) {
		stats.put(name, initialValue.toString());
		AdvancedControllableProperty.Slider slider = new AdvancedControllableProperty.Slider();
		slider.setLabelStart(labelStart);
		slider.setLabelEnd(labelEnd);
		slider.setRangeStart(rangeStart);
		slider.setRangeEnd(rangeEnd);

		return new AdvancedControllableProperty(name, new Date(), slider, initialValue);
	}

	/**
	 * Create Numeric is control property for metric
	 *
	 * @param name the name of the property
	 * @param initialValue the initialValue is number
	 * @return AdvancedControllableProperty Numeric instance
	 */
	private AdvancedControllableProperty createNumeric(String name, String initialValue) {
		AdvancedControllableProperty.Numeric numeric = new AdvancedControllableProperty.Numeric();

		return new AdvancedControllableProperty(name, new Date(), numeric, initialValue);
	}

	/**
	 * Create a button.
	 *
	 * @param name name of the button
	 * @param label label of the button
	 * @param labelPressed label of the button after pressing it
	 * @param gracePeriod grace period of button
	 * @return This returns the instance of {@link AdvancedControllableProperty} type Button.
	 */
	private AdvancedControllableProperty createButton(String name, String label, String labelPressed, long gracePeriod) {
		AdvancedControllableProperty.Button button = new AdvancedControllableProperty.Button();
		button.setLabel(label);
		button.setLabelPressed(labelPressed);
		button.setGracePeriod(gracePeriod);
		return new AdvancedControllableProperty(name, new Date(), button, VaddioNanoConstant.EMPTY);
	}

	/**
	 * Create switch is control property for metric
	 *
	 * @param name the name of property
	 * @param status initial status (0|1)
	 * @return AdvancedControllableProperty switch instance
	 */
	private AdvancedControllableProperty createSwitch(String name, int status, String labelOff, String labelOn) {
		AdvancedControllableProperty.Switch toggle = new AdvancedControllableProperty.Switch();
		toggle.setLabelOff(labelOff);
		toggle.setLabelOn(labelOn);

		AdvancedControllableProperty advancedControllableProperty = new AdvancedControllableProperty();
		advancedControllableProperty.setName(name);
		advancedControllableProperty.setValue(status);
		advancedControllableProperty.setType(toggle);
		advancedControllableProperty.setTimestamp(new Date());

		return advancedControllableProperty;
	}
}