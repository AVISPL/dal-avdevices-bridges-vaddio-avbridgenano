/*
 * Copyright (c) 2023 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano;

import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
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
import com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common.AudioCrosspoint;
import com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common.AudioInput;
import com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common.EnumTypeHandler;
import com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common.NetworkInformation;
import com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common.PropertiesControlList;
import com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common.StreamSettings;
import com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common.VaddioCommand;
import com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common.VaddioNanoConstant;
import com.avispl.symphony.dal.communicator.SshCommunicator;
import com.avispl.symphony.dal.util.StringUtils;

/**
 * VaddioBridgeNanoCommunicator An implementation of SshCommunicator to provide communication and interaction with Vaddio Bridge Nano device
 *
 * Monitoring
 * Network information
 * Streaming Settings
 *
 * Controlling
 * Mute
 * Volume(dB)
 * Gain(dB)
 * Reboot
 * Video Mute
 * Audio Mute
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/14/2023
 * @since 1.0.0
 */
public class VaddioBridgeNanoCommunicator extends SshCommunicator implements Monitorable, Controller {

	/**
	 * cache to store key and value
	 */
	private final Map<String, String> cacheKeyAndValue = new HashMap<>();

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
	 * configManagement imported from the user interface
	 */
	private String configManagement;

	/**
	 * configManagement in boolean value
	 */
	private boolean isConfigManagement;
	private boolean isEmergencyDelivery;
	private boolean isNextPollingInterval;

	/**
	 * Retrieves {@link #configManagement}
	 *
	 * @return value of {@link #configManagement}
	 */
	public String getConfigManagement() {
		return configManagement;
	}

	/**
	 * Sets {@link #configManagement} value
	 *
	 * @param configManagement new value of {@link #configManagement}
	 */
	public void setConfigManagement(String configManagement) {
		this.configManagement = configManagement;
	}

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
	 * <p>
	 *
	 * Check for available devices before retrieving the value
	 * ping latency information to Symphony
	 */
	@Override
	public int ping() throws Exception {
		if (isInitialized()) {
			long pingResultTotal = 0L;

			for (int i = 0; i < this.getPingAttempts(); i++) {
				long startTime = System.currentTimeMillis();

				try (Socket puSocketConnection = new Socket(this.host, this.getPort())) {
					puSocketConnection.setSoTimeout(this.getPingTimeout());
					if (puSocketConnection.isConnected()) {
						long pingResult = System.currentTimeMillis() - startTime;
						pingResultTotal += pingResult;
						if (this.logger.isTraceEnabled()) {
							this.logger.trace(String.format("PING OK: Attempt #%s to connect to %s on port %s succeeded in %s ms", i + 1, host, this.getPort(), pingResult));
						}
					} else {
						if (this.logger.isDebugEnabled()) {
							logger.debug(String.format("PING DISCONNECTED: Connection to %s did not succeed within the timeout period of %sms", host, this.getPingTimeout()));
						}
						return this.getPingTimeout();
					}
				} catch (SocketTimeoutException | ConnectException tex) {
					throw new SocketTimeoutException("Socket connection timed out");
				} catch (UnknownHostException tex) {
					throw new SocketTimeoutException("Socket connection timed out" + tex.getMessage());
				} catch (Exception e) {
					if (this.logger.isWarnEnabled()) {
						this.logger.warn(String.format("PING TIMEOUT: Connection to %s did not succeed, UNKNOWN ERROR %s: ", host, e.getMessage()));
					}
					return this.getPingTimeout();
				}
			}
			return Math.max(1, Math.toIntExact(pingResultTotal / this.getPingAttempts()));
		} else {
			throw new IllegalStateException("Cannot use device class without calling init() first");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Statistics> getMultipleStatistics() throws Exception {
		ExtendedStatistics extendedStatistics = new ExtendedStatistics();
		List<AdvancedControllableProperty> advancedControllableProperty = new ArrayList<>();
		Map<String, String> stats = new HashMap<>();
		Map<String, String> controlStats = new HashMap<>();
		reentrantLock.lock();
		try {
			if (!isEmergencyDelivery) {
				convertConfigManagement();
				retrieveMonitoring();
				if (localExtendedStatistics != null && localExtendedStatistics.getStatistics() == null && !isNextPollingInterval || !isConfigManagement) {
					populateMonitoringAndControlling(stats, controlStats, advancedControllableProperty);
					populateAudioInput(controlStats, advancedControllableProperty);
					populateOutputControl(controlStats, advancedControllableProperty);
					populateCrosspointGain(stats, controlStats, advancedControllableProperty);
					if (isConfigManagement) {
						stats.putAll(controlStats);
						extendedStatistics.setControllableProperties(advancedControllableProperty);
					}
					extendedStatistics.setStatistics(stats);
				}
				if (localExtendedStatistics != null && localExtendedStatistics.getStatistics() != null && !localExtendedStatistics.getStatistics().isEmpty()) {
					updateLocalExtendedStatisticsByPolingInterval(extendedStatistics, stats, controlStats, advancedControllableProperty);
				}
				localExtendedStatistics = extendedStatistics;
			}
			isEmergencyDelivery = false;
		} finally {
			reentrantLock.unlock();
		}
		return Collections.singletonList(localExtendedStatistics);
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
	 * {@inheritDoc}
	 */
	@Override
	public void controlProperty(ControllableProperty controllableProperty) throws Exception {
		reentrantLock.lock();
		try {
			this.timeout = controlSSHTimeout;
			if (localExtendedStatistics == null || localExtendedStatistics.getStatistics().isEmpty()) {
				return;
			}
			isEmergencyDelivery = true;
			Map<String, String> stats = this.localExtendedStatistics.getStatistics();
			List<AdvancedControllableProperty> advancedControllableProperties = this.localExtendedStatistics.getControllableProperties();
			String value = String.valueOf(controllableProperty.getValue());
			String property = controllableProperty.getProperty();
			String keyName = property;
			String groupName = property;

			boolean isCrosspointControl = handleCrosspointGainControl(stats, advancedControllableProperties, value, property);
			if (isCrosspointControl) {
				return;
			}
			if (property.contains(VaddioNanoConstant.HASH)) {
				String[] group = property.split(VaddioNanoConstant.HASH);
				groupName = group[0];
				keyName = group[1];
			}
			String group = AudioInput.getValueByName(groupName);
			if (StringUtils.isNullOrEmpty(group)) {
				group = EnumTypeHandler.getCommandByValue(AudioCrosspoint.class, groupName);
				if (StringUtils.isNotNullOrEmpty(group)) {
					group = group.replace("audio ", VaddioNanoConstant.EMPTY);
				}
			}
			PropertiesControlList propertyControl = PropertiesControlList.getControlGroupNameByValue(keyName);
			switch (propertyControl) {
				case VIDEO_MUTE:
					String videoMute = VaddioNanoConstant.OFF;
					if (String.valueOf(VaddioNanoConstant.NUMBER_ONE).equalsIgnoreCase(value)) {
						videoMute = VaddioNanoConstant.ON;
					}
					String videoMuteCommand = VaddioCommand.VIDEO_COMMAND;
					sendCommandToControlDevice(videoMuteCommand, videoMute, groupName);
					break;
				case AUDIO_MUTE:
					String audioMute = VaddioNanoConstant.OFF;
					if (String.valueOf(VaddioNanoConstant.NUMBER_ONE).equalsIgnoreCase(value)) {
						audioMute = VaddioNanoConstant.ON;
					}
					String audioMuteControl = VaddioCommand.AUDIO_COMMAND;
					sendCommandToControlDevice(audioMuteControl, audioMute, groupName);
					updateMasterMuteControl(stats, advancedControllableProperties, audioMute);
					break;
				case MUTE:
					String muteValue = VaddioNanoConstant.OFF;
					if (String.valueOf(VaddioNanoConstant.NUMBER_ONE).equalsIgnoreCase(value)) {
						muteValue = VaddioNanoConstant.ON;
					}
					String muteCommand = VaddioNanoConstant.AUDIO_COMMAND + group + VaddioNanoConstant.SPACE + VaddioNanoConstant.MUTE_CONTROL.trim() + VaddioNanoConstant.SPACE;
					sendCommandToControlDevice(muteCommand, muteValue, group);
					break;
				case VOLUME:
					String volumeControl = VaddioNanoConstant.AUDIO_COMMAND + group + VaddioNanoConstant.SPACE + VaddioNanoConstant.VOLUME_CONTROL.trim() + VaddioNanoConstant.SET;
					sendCommandToControlDevice(volumeControl, value, group);
					stats.put(groupName + VaddioNanoConstant.HASH + VaddioNanoConstant.VOLUME_CURRENT_VALUE, String.valueOf((int) Float.parseFloat(value)));
					break;
				case STREAM_MODE:
					String streamMode = VaddioNanoConstant.USB;
					if (String.valueOf(VaddioNanoConstant.NUMBER_ONE).equalsIgnoreCase(value)) {
						streamMode = VaddioNanoConstant.IP;
					}
					String streamingModeCommand = VaddioCommand.STREAMING_MODE;
					sendCommandToControlDevice(streamingModeCommand, streamMode, groupName);
					break;
				case SYSTEM_REBOOT:
					controlSystemReboot(groupName, value, true);
					break;
				case SYSTEM_REBOOT_DELAY:
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalDestroy() {
		if (localExtendedStatistics != null && localExtendedStatistics.getStatistics() != null && localExtendedStatistics.getControllableProperties() != null) {
			localExtendedStatistics = null;
		}
		isNextPollingInterval = false;
		cacheKeyAndValue.clear();

		super.internalDestroy();
	}

	/**
	 * Update Master mute control
	 *
	 * @param stats the stats are list of Statistics
	 * @param advancedControllableProperties the advancedControllableProperty are AdvancedControllableProperty instance
	 * @param muteValue the muteValue is value of mute
	 * @throws FailedLoginException if login error
	 */
	private void updateMasterMuteControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String muteValue) throws FailedLoginException {
		for (AudioCrosspoint audioCrosspoint : AudioCrosspoint.values()) {
			String key = audioCrosspoint.getName() + VaddioNanoConstant.HASH + VaddioNanoConstant.MUTE;
			if (VaddioNanoConstant.ON.equalsIgnoreCase(muteValue)) {
				stats.put(key, VaddioNanoConstant.ON);
				advancedControllableProperties.removeIf(item -> item.getName().equalsIgnoreCase(key));
				continue;
			}
			String group = audioCrosspoint.getName() + VaddioNanoConstant.HASH;
			String commandItem = audioCrosspoint.getCommand();
			sendCommandDetails(commandItem + VaddioNanoConstant.VOLUME_COMMAND, group + VaddioNanoConstant.VOLUME);
			populateMuteControl(stats, advancedControllableProperties, audioCrosspoint.getName() + VaddioNanoConstant.HASH + VaddioNanoConstant.MUTE);
		}
	}

	/**
	 * Update localExtendedStatistics by current polling interval
	 *
	 * @param extendedStatistics are ExtendedStatistics instance
	 * @param stats the stats are list of Statistics
	 * @param controlStats the controlStats are list of Statistics
	 * @param advancedControllableProperty the advancedControllableProperty are AdvancedControllableProperty instance
	 */
	private void updateLocalExtendedStatisticsByPolingInterval(ExtendedStatistics extendedStatistics, Map<String, String> stats, Map<String, String> controlStats,
			List<AdvancedControllableProperty> advancedControllableProperty) {
		if (!isConfigManagement) {
			return;
		}
		List<AdvancedControllableProperty> newAdvancedControllableProperty = localExtendedStatistics.getControllableProperties();
		Map<String, String> newStats = localExtendedStatistics.getStatistics();
		if (!isNextPollingInterval) {
			populateCrosspointGain(stats, controlStats, advancedControllableProperty);
			newStats = newStats.entrySet().stream()
					.filter(entry -> !entry.getKey().startsWith(VaddioNanoConstant.CROSSPOINT_GAIN))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		} else {
			populateMonitoringAndControlling(stats, controlStats, advancedControllableProperty);
			populateAudioInput(stats, advancedControllableProperty);
			populateOutputControl(stats, advancedControllableProperty);
			newStats = newStats.entrySet().stream()
					.filter(entry -> entry.getKey().startsWith(VaddioNanoConstant.CROSSPOINT_GAIN))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		}
		stats.putAll(controlStats);
		newAdvancedControllableProperty.removeIf(item -> advancedControllableProperty.stream().anyMatch(it -> it.getName().equals(item.getName())));
		stats.putAll(newStats);
		newAdvancedControllableProperty.addAll(advancedControllableProperty);
		extendedStatistics.setStatistics(stats);
		extendedStatistics.setControllableProperties(newAdvancedControllableProperty);
	}

	/**
	 * Handles crosspoint gain control detail
	 *
	 * @param stats the stats are list of Statistics
	 * @param advancedControllableProperties the advancedControllableProperty are AdvancedControllableProperty instance
	 * @param value the value is value to send the command
	 * @param property the property  is name of command
	 * @return boolean (true/false)
	 */
	private boolean handleCrosspointGainControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String value, String property) {
		if (!property.contains(VaddioNanoConstant.CROSSPOINT_GAIN)) {
			return false;
		}

		String[] group = property.split(VaddioNanoConstant.HASH);
		String groupName = group[0].replace(VaddioNanoConstant.CROSSPOINT_GAIN, VaddioNanoConstant.EMPTY);
		String keyName = group[1].replace(VaddioNanoConstant.GAIN, VaddioNanoConstant.EMPTY);
		String command =
				EnumTypeHandler.getCommandByValue(AudioCrosspoint.class, groupName) + VaddioNanoConstant.GAIN_COMMAND + AudioInput.getValueByName(keyName) + VaddioNanoConstant.SET;
		sendCommandToControlDevice(command, value, group[1]);
		updateLocalControlValue(stats, advancedControllableProperties, property, value);
		stats.put(group[0] + VaddioNanoConstant.HASH + keyName + VaddioNanoConstant.GAIN_CURRENT_VALUE, value);
		return true;
	}

	/**
	 * Updates cached devices' control value, after the control command was executed with the specified value.
	 * It is done in order for aggregator to populate the latest control values, after the control command has been executed,
	 * but before the next devices details polling cycle was addressed.
	 *
	 * @param stats the stats are list of Statistics
	 * @param advancedControllableProperties the advancedControllableProperty are AdvancedControllableProperty instance
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
	 * Send command to control device by value
	 *
	 * @param command the command is command to send to the device
	 * @param value the value is value of the command
	 * @param name the name is group name
	 */
	private void sendCommandToControlDevice(String command, String value, String name) {
		try {
			command = (command.trim() + VaddioNanoConstant.SPACE + value).toLowerCase(Locale.ROOT);
			String response = send(command.contains("\r") ? command : command.concat("\r"));
			if (StringUtils.isNullOrEmpty(response) || response.contains(VaddioNanoConstant.ERROR_RESPONSE) || !response.contains(VaddioNanoConstant.OK)) {
				throw new IllegalArgumentException(String.format("Error when control %s, Syntax error command: %s", name, response));
			}
		} catch (Exception e) {
			throw new IllegalArgumentException(String.format("Can't control %s with %s value.", name, value), e);
		}
	}

	/**
	 * Control SystemReboot
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
	 * Populate Audio input detail
	 *
	 * @param controlStats the controlStats are list of Statistics
	 * @param advancedControllableProperty the advancedControllableProperty are AdvancedControllableProperty instance
	 */
	private void populateAudioInput(Map<String, String> controlStats, List<AdvancedControllableProperty> advancedControllableProperty) {
		for (AudioInput audioInputEnum : AudioInput.values()) {
			String key = audioInputEnum.getPropertyName() + VaddioNanoConstant.HASH + VaddioNanoConstant.VOLUME;
			String data = StringUtils.isNullOrEmpty(cacheKeyAndValue.get(key)) ? VaddioNanoConstant.NONE : cacheKeyAndValue.get(key);
			switch (audioInputEnum) {
				case HDMI_IN_RIGHT:
				case HDMI_IN_LEFT:
				case LINE_IN_LEFT:
				case LINE_IN_RIGHT:
				case USB_PLAYBACK_LEFT:
				case USB_PLAYBACK_RIGHT:
					data = extractResponseValue(data, VaddioNanoConstant.VOLUME_REGEX).split(VaddioNanoConstant.SPACE)[0];
					populateVolumeControl(controlStats, advancedControllableProperty, audioInputEnum.getPropertyName(), key, data);
					populateMuteControl(controlStats, advancedControllableProperty, audioInputEnum.getPropertyName() + VaddioNanoConstant.HASH + VaddioNanoConstant.MUTE);
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

		if (VaddioNanoConstant.NONE.equalsIgnoreCase(data)) {
			stats.put(key, VaddioNanoConstant.NONE);
			return;
		}
		stats.put(key, VaddioNanoConstant.EMPTY);
		int value = 0;
		if (VaddioNanoConstant.ON.equalsIgnoreCase(extractResponseValue(data, VaddioNanoConstant.MUTE_REGEX))) {
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
		if (VaddioNanoConstant.NONE.equalsIgnoreCase(data)) {
			stats.put(key, VaddioNanoConstant.NONE);
			return;
		}
		stats.put(key, VaddioNanoConstant.EMPTY);
		float volumeValue = Float.parseFloat(data);
		String volumeCurrentKey = groupName + VaddioNanoConstant.HASH + VaddioNanoConstant.VOLUME_CURRENT_VALUE;
		stats.put(volumeCurrentKey, String.valueOf((int) Float.parseFloat(data)));

		String minVolume = VaddioNanoConstant.MIN_VOLUME_LINE;
		if (key.contains(AudioInput.LINE_IN_LEFT.getPropertyName()) || key.contains(AudioInput.LINE_IN_RIGHT.getPropertyName()) ||
				key.contains(AudioCrosspoint.GAIN_LINE_OUT_LEFT.getName()) || key.contains(AudioCrosspoint.GAIN_LINE_OUT_RIGHT.getName())) {
			minVolume = VaddioNanoConstant.MIN_VOLUME;
		}
		AdvancedControllableProperty volumeControl = createSlider(stats, key, minVolume, VaddioNanoConstant.MAX_VOLUME,
				Float.valueOf(minVolume), Float.valueOf(VaddioNanoConstant.MAX_VOLUME), volumeValue);
		advancedControllableProperty.add(volumeControl);
	}

	/**
	 * Populate Audio output control
	 *
	 * @param controlStats the controlStats are list of Statistics
	 * @param advancedControllableProperty the advancedControllableProperty are AdvancedControllableProperty instance
	 */
	private void populateOutputControl(Map<String, String> controlStats, List<AdvancedControllableProperty> advancedControllableProperty) {
		for (AudioCrosspoint command : AudioCrosspoint.values()) {
			String key = command.getName() + VaddioNanoConstant.HASH + VaddioNanoConstant.VOLUME;
			String data = StringUtils.isNullOrEmpty(cacheKeyAndValue.get(key)) ? VaddioNanoConstant.NONE : cacheKeyAndValue.get(key);
			switch (command) {
				case GAIN_HDMI_OUT_LEFT:
				case GAIN_HDMI_OUT_RIGHT:
				case GAIN_IP_OUT_LEFT:
				case GAIN_IP_OUT_RIGHT:
				case GAIN_LINE_OUT_LEFT:
				case GAIN_LINE_OUT_RIGHT:
				case GAIN_RECORD_OUT_LEFT:
				case GAIN_RECORD_OUT_RIGHT:
					data = extractResponseValue(data, VaddioNanoConstant.VOLUME_REGEX).split(VaddioNanoConstant.SPACE)[0];
					populateVolumeControl(controlStats, advancedControllableProperty, command.getName(), key, data);
					key = command.getName() + VaddioNanoConstant.HASH + VaddioNanoConstant.MUTE;
					String audioKey = VaddioCommand.AUDIO_MUTE.getName();
					String muteValue = StringUtils.isNullOrEmpty(cacheKeyAndValue.get(audioKey)) ? VaddioNanoConstant.NONE : cacheKeyAndValue.get(audioKey);
					if (VaddioNanoConstant.ON.equalsIgnoreCase(extractResponseValue(muteValue, VaddioNanoConstant.MUTE_REGEX))) {
						controlStats.put(key, VaddioNanoConstant.ON);
						String finalKey = key;
						advancedControllableProperty.removeIf(item -> item.getName().equalsIgnoreCase(finalKey));
						break;
					}
					populateMuteControl(controlStats, advancedControllableProperty, key);
					break;
				default:
					logger.debug(String.format("the command %s doesn't support", command.getName()));
					break;
			}
		}
	}

	/**
	 * Populate Crosspoint Gain details
	 *
	 * @param stats the stats are list of Statistics
	 * @param controlStats the controlStats are list of Statistics
	 * @param advancedControllableProperty the advancedControllableProperty are AdvancedControllableProperty instance
	 */
	private void populateCrosspointGain(Map<String, String> stats, Map<String, String> controlStats, List<AdvancedControllableProperty> advancedControllableProperty) {
		String key;
		for (AudioCrosspoint command : AudioCrosspoint.values()) {
			switch (command) {
				case GAIN_HDMI_OUT_LEFT:
				case GAIN_HDMI_OUT_RIGHT:
				case GAIN_IP_OUT_LEFT:
				case GAIN_IP_OUT_RIGHT:
				case GAIN_LINE_OUT_LEFT:
				case GAIN_LINE_OUT_RIGHT:
				case GAIN_RECORD_OUT_LEFT:
				case GAIN_RECORD_OUT_RIGHT:
					String crosspointGainGroup = VaddioNanoConstant.CROSSPOINT_GAIN + command.getName() + VaddioNanoConstant.HASH;
					for (AudioInput audioInputEnum : AudioInput.values()) {
						switch (audioInputEnum) {
							case USB_PLAYBACK_LEFT:
							case USB_PLAYBACK_RIGHT:
							case HDMI_IN_LEFT:
							case HDMI_IN_RIGHT:
							case LINE_IN_LEFT:
							case LINE_IN_RIGHT:
								key = crosspointGainGroup + audioInputEnum.getPropertyName() + VaddioNanoConstant.GAIN;
								String crosspointGainCurrentKey = crosspointGainGroup + audioInputEnum.getPropertyName() + VaddioNanoConstant.GAIN_CURRENT_VALUE;
								String data = StringUtils.isNullOrEmpty(cacheKeyAndValue.get(key)) ? VaddioNanoConstant.NONE : cacheKeyAndValue.get(key);
								if (VaddioNanoConstant.NONE.equalsIgnoreCase(data)) {
									controlStats.put(key, VaddioNanoConstant.NONE);
									break;
								}
								populateGainControl(controlStats, advancedControllableProperty, key, crosspointGainCurrentKey, parseResponseByCommandGain(data));
								break;
							default:
								break;
						}
					}
					key = VaddioNanoConstant.CROSSPOINT_GAIN + command.getName() + VaddioNanoConstant.HASH + VaddioNanoConstant.ENABLED_ROUTES;
					String data = StringUtils.isNullOrEmpty(cacheKeyAndValue.get(key)) ? VaddioNanoConstant.NONE : cacheKeyAndValue.get(key);
					data = parseResponseByCommandGain(data).replace("]", VaddioNanoConstant.EMPTY);
					if (StringUtils.isNullOrEmpty(data) || VaddioNanoConstant.NONE.equals(data)) {
						stats.put(key, VaddioNanoConstant.NONE);
						break;
					}
					List<String> itemList = Arrays.stream(data.split(VaddioNanoConstant.SPACE))
							.map(audioValue -> EnumTypeHandler.getNameByValue(AudioInput.class, audioValue.trim()))
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
		AdvancedControllableProperty crosspointGain = createSlider(stats, key, VaddioNanoConstant.MIN_GAIN, VaddioNanoConstant.MAX_GAIN,
				Float.parseFloat(VaddioNanoConstant.MIN_GAIN), Float.valueOf(VaddioNanoConstant.MAX_GAIN), Float.valueOf(value));
		advancedControllableProperty.add(crosspointGain);
		stats.put(currentKey, String.valueOf((int) Float.parseFloat(value)));
	}

	/**
	 * Populate monitoring and controlling data
	 *
	 * @param stats the stats are list of statistics
	 * @param advancedControllableProperty the advancedControllableProperty are AdvancedControllableProperty instance
	 */
	private void populateMonitoringAndControlling(Map<String, String> stats, Map<String, String> controlStats,
			List<AdvancedControllableProperty> advancedControllableProperty) {
		for (VaddioCommand command : VaddioCommand.values()) {
			String key = command.getName();
			String data = StringUtils.isNullOrEmpty(cacheKeyAndValue.get(command.getName())) ? VaddioNanoConstant.NONE : cacheKeyAndValue.get(command.getName());
			switch (command) {
				case VIDEO_MUTE:
				case AUDIO_MUTE:
					data = extractResponseValue(data, VaddioNanoConstant.VIDEO_MUTE_REGEX);
					controlStats.put(key, VaddioNanoConstant.EMPTY);
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
					controlStats.put(key, VaddioNanoConstant.EMPTY);
					AdvancedControllableProperty streamModeControl = createSwitch(key, isIPStreaming ? 1 : 0, VaddioNanoConstant.USB, VaddioNanoConstant.IP);
					advancedControllableProperty.add(streamModeControl);
					break;
				case VERSION:
					String audioVersion = extractResponseValue(data, VaddioNanoConstant.AUDIO_REGEX);
					String systemVersion = extractResponseValue(data, VaddioNanoConstant.SYSTEM_VERSION_REGEX);
					stats.put(VaddioNanoConstant.AUDIO_VERSION, StringUtils.isNullOrEmpty(audioVersion) ? VaddioNanoConstant.NONE : audioVersion);
					stats.put(key, StringUtils.isNullOrEmpty(systemVersion) ? VaddioNanoConstant.NONE : systemVersion);
					break;
				default:
					logger.debug(String.format("the command %s doesn't support", command.getName()));
					break;
			}
			controlStats.put(VaddioNanoConstant.SYSTEM_REBOOT, VaddioNanoConstant.EMPTY);
			advancedControllableProperty.add(createButton(VaddioNanoConstant.SYSTEM_REBOOT, VaddioNanoConstant.REBOOT, VaddioNanoConstant.REBOOTING, 0L));

			controlStats.put(VaddioNanoConstant.SYSTEM_REBOOT_DELAY, VaddioNanoConstant.DEFAULT_REBOOT);
			AdvancedControllableProperty systemRebootDelay = createNumeric(VaddioNanoConstant.SYSTEM_REBOOT_DELAY, VaddioNanoConstant.DEFAULT_REBOOT);
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
					String value = extractResponseValue(response, streamSettings.getValue());
					switch (streamSettings) {
						case IP_VIDEO_QUALITY:
							handleIPVideoQuality(value, stats);
							break;
						case IP_PROTOCOL:
							handleIPProtocol(value, stats);
							break;
						case IP_STREAMING_ENABLED:
							String ipEnable = VaddioNanoConstant.DISABLE;
							if (VaddioNanoConstant.TRUE.equalsIgnoreCase(value)) {
								ipEnable = VaddioNanoConstant.ENABLE;
							}
							value = ipEnable;
							break;
						default:
							break;
					}
					if (streamSettings.getName().equals(StreamSettings.IP_PROTOCOL.getName())) {
						continue;
					}
					stats.put(key, value);
				}
				if (!isIPStreaming && !streamSettings.isIPStreaming()) {
					String value = extractResponseValue(response, streamSettings.getValue());

					if (streamSettings.getName().equals(StreamSettings.HID_AUDIO_CONTROLS_ENABLED.getName())) {
						stats.put(key, VaddioNanoConstant.TRUE.equalsIgnoreCase(value) ? VaddioNanoConstant.ENABLE : VaddioNanoConstant.DISABLE);
					} else {
						stats.put(key, value);
					}
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
			stats.remove(group + VaddioNanoConstant.IP_BIT_RATE_MODE);
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
		String ipProtocol = VaddioNanoConstant.RTSP;
		if (VaddioNanoConstant.FALSE.equalsIgnoreCase(value)) {
			stats.remove(group + VaddioNanoConstant.IP_RTMP_PORT);
			stats.remove(group + VaddioNanoConstant.IP_RTMP_SERVICE);
		} else {
			stats.remove(group + VaddioNanoConstant.IP_RTSP_PORT);
			stats.remove(group + VaddioNanoConstant.IP_RTSP_URL);
			stats.remove(group + VaddioNanoConstant.IP_RTSP_MTU);
			ipProtocol = VaddioNanoConstant.RTMP;
		}
		value = ipProtocol;
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
				stats.put(VaddioNanoConstant.NETWORK_SETTINGS + VaddioNanoConstant.HASH + network.getName(), extractResponseValue(response, network.getValue()));
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
	private static String extractResponseValue(String response, String regex) {
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
		if (!isNextPollingInterval) {
			for (VaddioCommand command : VaddioCommand.values()) {
				if (isConfigManagement || command.isMonitoring()) {
					sendCommandDetails(command.getCommand(), command.getName());
				}
			}
			if (isConfigManagement) {
				retrieveMonitoringDataWithConfigManagement();
			} else {
				retrieveMonitoringDataWithDefaultConfigManagement();
			}
			isNextPollingInterval = true;
		} else {
			if (isConfigManagement) {
				for (AudioCrosspoint command : AudioCrosspoint.values()) {
					String commandItem = command.getCommand();
					for (AudioInput audioInputEnum : AudioInput.values()) {
						String group = VaddioNanoConstant.CROSSPOINT_GAIN + command.getName() + VaddioNanoConstant.HASH + audioInputEnum.getPropertyName() + VaddioNanoConstant.GAIN;
						sendCommandDetails(commandItem + VaddioNanoConstant.GAIN_COMMAND + audioInputEnum.getValue() + VaddioNanoConstant.GET, group);
					}
				}
				isNextPollingInterval = false;
			}
		}
	}

	/**
	 * Retrieve monitoring Data With ConfigManagement
	 *
	 * @throws FailedLoginException if get the FailedLoginException
	 */
	private void retrieveMonitoringDataWithConfigManagement() throws FailedLoginException {
		for (AudioInput audioInputEnum : AudioInput.values()) {
			String group = audioInputEnum.getPropertyName() + VaddioNanoConstant.HASH;
			sendCommandDetails(VaddioNanoConstant.AUDIO_COMMAND + audioInputEnum.getValue() + VaddioNanoConstant.VOLUME_COMMAND, group + VaddioNanoConstant.VOLUME);
			sendCommandDetails(VaddioNanoConstant.AUDIO_COMMAND + audioInputEnum.getValue() + VaddioNanoConstant.MUTE_COMMAND, group + VaddioNanoConstant.MUTE);
		}
		for (AudioCrosspoint command : AudioCrosspoint.values()) {
			String group = command.getName() + VaddioNanoConstant.HASH;
			String commandItem = command.getCommand();
			sendCommandDetails(commandItem + VaddioNanoConstant.ROUTES_COMMAND, VaddioNanoConstant.CROSSPOINT_GAIN + group + VaddioNanoConstant.ENABLED_ROUTES);
			sendCommandDetails(commandItem + VaddioNanoConstant.MUTE_COMMAND, group + VaddioNanoConstant.MUTE);
			sendCommandDetails(commandItem + VaddioNanoConstant.VOLUME_COMMAND, group + VaddioNanoConstant.VOLUME);
		}
	}

	/**
	 * Retrieve monitoring Data With Default ConfigManagement
	 *
	 * @throws FailedLoginException if get the FailedLoginException
	 */
	private void retrieveMonitoringDataWithDefaultConfigManagement() throws FailedLoginException {
		for (AudioCrosspoint command : AudioCrosspoint.values()) {
			sendCommandDetails(command.getCommand() + VaddioNanoConstant.ROUTES_COMMAND,
					VaddioNanoConstant.CROSSPOINT_GAIN + command.getName() + VaddioNanoConstant.HASH + VaddioNanoConstant.ENABLED_ROUTES);
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
	 * @return String the string is the extracted response
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
	 * This method is used to validate input config management from user
	 */
	private void convertConfigManagement() {
		isConfigManagement = StringUtils.isNotNullOrEmpty(this.configManagement) && this.configManagement.equalsIgnoreCase(VaddioNanoConstant.TRUE);
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