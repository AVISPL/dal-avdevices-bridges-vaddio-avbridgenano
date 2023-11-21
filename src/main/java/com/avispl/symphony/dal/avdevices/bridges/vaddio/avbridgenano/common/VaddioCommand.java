/*
 * Copyright (c) 2023 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common;

/**
 * VaddioCommand class defined the enum contains all overall command of the device
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 10/30/2023
 * @since 1.0.0
 */
public enum VaddioCommand {

	STREAM_MODE("StreamingMode", "streaming mode get", true),
	NETWORK_INFO("NetworkSettings", "network settings get", true),
	VERSION("SystemVersion", "version", true),
	VIDEO_MUTE("VideoMute", "video mute get", false),
	AUDIO_MUTE("AudioMute", "audio master mute get", false),
	STREAM_SETTINGS("StreamingSettings", "streaming settings get", true),
	;
	public static final String STREAMING_MODE = "streaming mode set ";
	public static final String SYSTEM_REBOOT = "system reboot";
	public static final String VIDEO_COMMAND = "video mute ";
	public static final String AUDIO_COMMAND = "audio master mute ";

	/**
	 * VaddioCommand
	 *
	 * @name name of {@link #name}
	 * @command command of {@link #command}
	 */
	VaddioCommand(String name, String command, boolean isMonitoring) {
		this.name = name;
		this.command = command;
		this.isMonitoring = isMonitoring;
	}

	private String name;
	private String command;
	private boolean isMonitoring;

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
	 * Retrieves {@link #isMonitoring}
	 *
	 * @return value of {@link #isMonitoring}
	 */
	public boolean isMonitoring() {
		return isMonitoring;
	}
}