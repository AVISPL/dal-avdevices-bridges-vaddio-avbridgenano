/*
 * Copyright (c) 2023 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common;

/**
 * VaddioCommand  class defined the enum contains all command off the device
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 10/30/2023
 * @since 1.0.0
 */
public enum VaddioCommand {

	STREAM_MODE("StreamingMode", "streaming mode get"),
	NETWORK_INFO("NetworkSettings", "network settings get"),
	VERSION("SystemVersion", "version"),
	VIDEO_MUTE("VideoMute", "video mute get"),
	STREAM_SETTINGS("StreamingSettings", "streaming settings get"),
	;
	public static final String STREAMING_MODE = "streaming mode set ";
	public static final String SYSTEM_REBOOT = "system reboot";
	public static final String VIDEO_COMMAND = "video mute ";

	/**
	 * VaddioCommand
	 *
	 * @name name of {@link #name}
	 * @command command of {@link #command}
	 */
	VaddioCommand(String name, String command) {
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