/*
 * Copyright (c) 2023 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common;

/**
 * ControlGroup class defined list of control properties
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 11/17/2023
 * @since 1.0.0
 */
public enum PropertiesControlList {

	MUTE(VaddioNanoConstant.MUTE),
	VOLUME(VaddioNanoConstant.VOLUME),
	VIDEO_MUTE(VaddioNanoConstant.VIDEO_MUTE),
	SYSTEM_REBOOT(VaddioNanoConstant.SYSTEM_REBOOT),
	STREAM_MODE(VaddioNanoConstant.STREAM_MODE),
	AUDIO_MUTE(VaddioNanoConstant.AUDIO_MUTE);

	/**
	 * PropertiesControlList constructor
	 *
	 * @name name of {@link #name}
	 */
	PropertiesControlList(String name) {
		this.name = name;
	}

	private String name;

	/**
	 * Retrieves {@link #name}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get control name by value
	 *
	 * @param value the value is name of ControlGroupName
	 * @return PropertiesControlList is PropertiesControlList enum instance
	 */
	public static PropertiesControlList getControlGroupNameByValue(String value) {
		for (PropertiesControlList controlGroup : PropertiesControlList.values()) {
			if (controlGroup.getName().equalsIgnoreCase(value)) {
				return controlGroup;
			}
		}
		return null;
	}
}