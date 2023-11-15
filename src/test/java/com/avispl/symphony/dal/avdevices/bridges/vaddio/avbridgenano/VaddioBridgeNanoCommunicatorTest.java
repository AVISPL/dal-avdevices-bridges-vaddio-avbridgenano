/*
 *  Copyright (c) 2023 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common.VaddioNanoConstant;

/**
 * VaddioBridgeNanoCommunicatorTest class
 */
public class VaddioBridgeNanoCommunicatorTest {
	private VaddioBridgeNanoCommunicator vaddioBridgeNanoCommunicator;

	@BeforeEach()
	public void setUp() throws Exception {
		vaddioBridgeNanoCommunicator = new VaddioBridgeNanoCommunicator();
		vaddioBridgeNanoCommunicator.setHost("172.31.254.176");
		vaddioBridgeNanoCommunicator.setPort(22);
		vaddioBridgeNanoCommunicator.setLogin("");
		vaddioBridgeNanoCommunicator.setPassword("");
		vaddioBridgeNanoCommunicator.init();
		vaddioBridgeNanoCommunicator.connect();
	}

	@AfterEach()
	public void destroy() throws Exception {
		vaddioBridgeNanoCommunicator.disconnect();
	}

	/**
	 * Test Monitoring network settings
	 *
	 * Expect get network successfully
	 */
	@Test
	void testNetworkSettings() throws Exception {
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) vaddioBridgeNanoCommunicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		Assertions.assertEquals("vaddio-avbn-FC-69-47-E9-35-6B", stats.get(VaddioNanoConstant.NETWORK_SETTINGS + "#HostName"));
		Assertions.assertEquals("eth0:WAN", stats.get(VaddioNanoConstant.NETWORK_SETTINGS + "#InterfaceName"));
		Assertions.assertEquals("172.31.254.176", stats.get(VaddioNanoConstant.NETWORK_SETTINGS + "#IPAddress"));
		Assertions.assertEquals("FC:69:47:E9:35:6B", stats.get(VaddioNanoConstant.NETWORK_SETTINGS + "#MACAddress"));
		Assertions.assertEquals("255.255.255.0", stats.get(VaddioNanoConstant.NETWORK_SETTINGS + "#SubnetMask"));
		Assertions.assertEquals("Disabled", stats.get(VaddioNanoConstant.NETWORK_SETTINGS + "#VLAN"));
	}

	/**
	 * Test Streaming Settings with USB port
	 * Expect get network successfully
	 */
	@Test
	void testStreamingSettingsWithUSBStreaming() throws Exception {
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) vaddioBridgeNanoCommunicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
	}

	/**
	 * Test Streaming Settings with USB port
	 * Expect get network successfully
	 */
	@Test
	void testStreamingSettingsWithIPStreaming() throws Exception {
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) vaddioBridgeNanoCommunicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
	}
}
