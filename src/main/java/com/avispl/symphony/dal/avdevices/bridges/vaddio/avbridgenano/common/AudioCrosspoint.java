/*
 * Copyright (c) 2023 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common;

/**
 * AudioCrosspoint class defined the enum contains all command of audio crosspoint matrix
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 10/30/2023
 * @since 1.0.0
 */
public enum AudioCrosspoint {

	GAIN_LINE_OUT_LEFT("LineOutLeft", "audio line_out_left"),
	GAIN_LINE_OUT_RIGHT("LineOutRight", "audio line_out_right"),
	GAIN_RECORD_OUT_LEFT("USBRecordLeft", "audio usb_record_left"),
	GAIN_RECORD_OUT_RIGHT("USBRecordRight", "audio usb_record_right"),
	GAIN_IP_OUT_LEFT("IPStreamLeft", "audio ip_out_left"),
	GAIN_IP_OUT_RIGHT("IPStreamRight", "audio ip_out_right"),
	GAIN_HDMI_OUT_LEFT("HDMIOutLeft", "audio hdmi_out_left"),
	GAIN_HDMI_OUT_RIGHT("HDMIOutRight", "audio hdmi_out_right"),
	;

	/**
	 * AudioCrosspoint constructor
	 *
	 * @name name of {@link #name}
	 * @command command of {@link #command}
	 */
	AudioCrosspoint(String name, String command) {
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
}