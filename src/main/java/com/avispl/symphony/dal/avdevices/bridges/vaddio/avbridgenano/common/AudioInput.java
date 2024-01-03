/*
 * Copyright (c) 2023 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common;

/**
 * AudioInput class defined the enum contains all command of audio input
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 10/30/2023
 * @since 1.0.0
 */
public enum AudioInput {

	LINE_IN_LEFT("Line In Left", "line_in_left", "LineInLeft"),
	LINE_IN_RIGHT("Line In Right", "line_in_right", "LineInRight"),
	USB_PLAYBACK_LEFT("USB Playback Left", "usb_playback_left", "USBPlaybackLeft"),
	USB_PLAYBACK_RIGHT("USB Playback Right", "usb_playback_right", "USBPlaybackRight"),
	HDMI_IN_LEFT("HDMI In Left", "hdmi_in_left", "HDMIInLeft"),
	HDMI_IN_RIGHT("HDMI In Right", "hdmi_in_right", "HDMIInRight"),
	;

	/**
	 * AudioInput constructor
	 *
	 * @name name of {@link #name}
	 * @command command of {@link #value}
	 * @command propertyName of {@link #propertyName}
	 */
	AudioInput(String name, String value, String propertyName) {
		this.name = name;
		this.value = value;
		this.propertyName = propertyName;
	}

	private String name;
	private String value;
	private String propertyName;

	/**
	 * Retrieves {@link #name}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves {@link #value}
	 *
	 * @return value of {@link #value}
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Retrieves {@link #propertyName}
	 *
	 * @return value of {@link #propertyName}
	 */
	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * Get value by name
	 *
	 * @param name is String
	 * @return T is metric instance
	 */
	public static <T extends Enum<T>> String getValueByName(String name) {
		try {
			for (AudioInput audioInputEnum : AudioInput.values()) {
				if (audioInputEnum.getPropertyName().equalsIgnoreCase(name)) {
					return audioInputEnum.getValue();
				}
			}
			return VaddioNanoConstant.EMPTY;
		} catch (Exception e) {
			return VaddioNanoConstant.EMPTY;
		}
	}
}