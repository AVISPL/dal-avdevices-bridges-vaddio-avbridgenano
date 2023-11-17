/*
 * Copyright (c) 2023 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common;

/**
 * NetworkEnum class provides all regex and name of network interface
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 11/6/2023
 * @since 1.0.0
 */
public enum NetworkInformation {

	INTERFACE_NAME(VaddioNanoConstant.INTERFACE_NAME, "Name(.*?)\r\n"),
	MAC_ADDRESS(VaddioNanoConstant.MAC_ADDRESS, "MAC Address(.*?)\r\n"),
	IP_ADDRESS(VaddioNanoConstant.IP_ADDRESS, "IP Address(.*?)\r\n"),
	SUBNET_MASK(VaddioNanoConstant.SUBNET_MASK, "Netmask(.*?)\r\n"),
	VLAN(VaddioNanoConstant.VLAN, "VLAN(.*?)\r\n"),
	GATEWAY(VaddioNanoConstant.GATEWAY, "Gateway(.*?)\r\n"),
	HOSTNAME(VaddioNanoConstant.HOSTNAME, "Hostname(.*?)\r\n");

	/**
	 * Constructor Instance
	 *
	 * @param name of {@link #name}
	 * @command value of {@link #value}
	 */
	NetworkInformation(String name, String value) {
		this.name = name;
		this.value = value;
	}

	final private String name;
	final private String value;

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
}