/*
 * Copyright (c) 2023 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common;

import java.lang.reflect.Method;

/**
 * CrosspointGainCommand  class defined the enum contains all command off the device
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 10/30/2023
 * @since 1.0.0
 */
public enum AudioCrosspointGainCommand {

	GAIN_LINE_OUT_LEFT("LineOutLeft", "audio line_out_left"),
	GAIN_LINE_OUT_RIGHT("LineOutRight", "audio line_out_right"),
	GAIN_RECORD_OUT_LEFT("USBRecordLeft", "audio usb_record_left"),
	GAIN_RECORD_OUT_RIGHT("USBRecordRight", "audio usb_record_right"),
	GAIN_IP_OUT_LEFT("IPStreamLeft", "audio ip_out_left"),
	GAIN_IP_OUT_RIGHT("IPStreamRight", "audio ip_out_right"),
	GAIN_HDMI_OUT_LEFT("HDMIOutLeft", "audio hdmi_out_left"),
	GAIN_HDMI_OUT_RIGHT("HDMIOutRight", "audio hdmi_out_right"),
	;
	public static final String GAIN_COMMAND = " crosspoint-gain ";
	public static final String ROUTES_COMMAND = " route get";
	public static final String MUTE_COMMAND = " mute get";
	public static final String VOLUME_COMMAND = " volume get";
	public static final String VOLUME = " volume ";
	public static final String MUTE = " mute ";
	public static final String GET = " get";
	public static final String SET = " set ";
	public static final String AUDIO_COMMAND = "audio ";

	/**
	 * CrosspointGainCommand constructor
	 *
	 * @name name of {@link #name}
	 * @command command of {@link #command}
	 */
	AudioCrosspointGainCommand(String name, String command) {
		this.name = name;
		this.command = command;
	}

	private String name;
	private String command;

	/**
	 * Retrieves {@link #name}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves {@link #command}
	 *
	 * @return value of {@link #command}
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * Get value by name
	 *
	 * @param enumType the enum type is enum class
	 * @param name is String
	 * @return T is metric instance
	 */
	public static <T extends Enum<T>> String getValueByName(Class<T> enumType, String name) {
		try {
			for (T metric : enumType.getEnumConstants()) {
				Method methodName = metric.getClass().getMethod("getName");
				String nameMetric = (String) methodName.invoke(metric); // getName executed
				if (name.equals(nameMetric)) {
					Method methodValue = metric.getClass().getMethod("getCommand");
					return methodValue.invoke(metric).toString().replace("audio ", "");
				}
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Get value by name
	 *
	 * @param name is String
	 * @return T is metric instance
	 */
	public static <T extends Enum<T>> String getCommandByValue(String name) {
		try {
			for (AudioCrosspointGainCommand gainCommand : AudioCrosspointGainCommand.values()) {
				if (gainCommand.getName().equals(name)) {
					return gainCommand.getCommand() + GAIN_COMMAND;
				}
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}
}