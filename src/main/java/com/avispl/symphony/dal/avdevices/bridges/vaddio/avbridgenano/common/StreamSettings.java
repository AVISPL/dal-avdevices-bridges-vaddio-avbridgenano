/*
 * Copyright (c) 2023 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common;

/**
 * StreamSettings class provides all regex and name of Streaming settings
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 11/6/2023
 * @since 1.0.0
 */
public enum StreamSettings {

	IP_PRESET_RESOLUTION(VaddioNanoConstant.IP_PRESET_RESOLUTION, "IP Preset_Resolution(.*?)\r\n", true),
	IP_BIT_RATE_MODE(VaddioNanoConstant.IP_BIT_RATE_MODE, "IP Bit_Rate_Mode(.*?)\r\n", true),
	IP_MAX_BANDWIDTH(VaddioNanoConstant.IP_MAX_BANDWIDTH, "IP Max_Bandwidth(.*?)\r\n", true),
	IP_RTMP_PORT(VaddioNanoConstant.IP_RTMP_PORT, "IP RTMP_Port(.*?)\r\n", true),
	IP_RTMP_SERVICE(VaddioNanoConstant.IP_RTMP_SERVICE, "IP RTMP_SERVICE(.*?)\r\n", true),
	IP_RTSP_MTU(VaddioNanoConstant.IP_RTSP_MTU, "IP RTSP_MTU(.*?)\r\n", true),
	IP_RTSP_PORT(VaddioNanoConstant.IP_RTSP_PORT, "IP RTSP_Port(.*?)\r\n", true),
	IP_RTSP_URL(VaddioNanoConstant.IP_RTSP_URL, "IP RTSP_URL(.*?)\r\n", true),
	IP_STREAMING_ENABLED(VaddioNanoConstant.IP_STREAMING_ENABLED, "IP Streaming_Enabled(.*?)\r\n", true),
	HID_AUDIO_CONTROLS_ENABLED(VaddioNanoConstant.HID_AUDIO_CONTROLS_ENABLED, "HID Audio_Controls_Enabled(.*?)\r\n", false),
	USB_DEVICE(VaddioNanoConstant.USB_DEVICE, "USB Device(.*?)\r\n", false),
	IP_CUSTOM_RESOLUTION(VaddioNanoConstant.IP_CUSTOM_RESOLUTION, "IP Custom_Resolution(.*?)\r\n", true),
	IP_VIDEO_QUALITY(VaddioNanoConstant.IP_VIDEO_QUALITY, "IP Video_Quality(.*?)\r\n", true),
	IP_PROTOCOL(VaddioNanoConstant.IP_PROTOCOL, "IP Protocol(.*?)\r\n", true),
	;

	/**
	 * Constructor Instance
	 *
	 * @param name of {@link #name}
	 * @command value of {@link #value}
	 * @command isIPStreaming of {@link #isIPStreaming}
	 */
	StreamSettings(String name, String value, boolean isIPStreaming) {
		this.name = name;
		this.value = value;
		this.isIPStreaming = isIPStreaming;
	}

	private String name;
	private String value;
	private boolean isIPStreaming;

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
	 * Retrieves {@link #isIPStreaming}
	 *
	 * @return value of {@link #isIPStreaming}
	 */
	public boolean isIPStreaming() {
		return isIPStreaming;
	}
}