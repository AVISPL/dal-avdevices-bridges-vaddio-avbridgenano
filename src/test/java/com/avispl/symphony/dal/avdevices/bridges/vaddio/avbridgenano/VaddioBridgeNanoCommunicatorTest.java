/*
 *  Copyright (c) 2023 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common.VaddioNanoConstant;

/**
 * VaddioBridgeNanoCommunicatorTest class
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/14/2023
 * @since 1.0.0
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
	 * Test default config management
	 *
	 * Expect default config management successfully
	 */
	@Test
	void testDefaultManagement() throws Exception {
		vaddioBridgeNanoCommunicator.setConfigManagement("false");
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) vaddioBridgeNanoCommunicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		Assertions.assertEquals(25, stats.size());
	}

	/**
	 * Test version for adapter
	 *
	 * Expect verify version for audio and system successfully
	 */
	@Test
	void testPollingInterval() throws Exception {
		vaddioBridgeNanoCommunicator.setConfigManagement("true");
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) vaddioBridgeNanoCommunicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		Assertions.assertNull(stats);
		extendedStatistics = (ExtendedStatistics) vaddioBridgeNanoCommunicator.getMultipleStatistics().get(0);
		stats = extendedStatistics.getStatistics();
		Assertions.assertEquals(168, stats.size());
	}

	/**
	 * Test version for adapter
	 *
	 * Expect verify version for audio and system successfully
	 */
	@Test
	void testVersion() throws Exception {
		vaddioBridgeNanoCommunicator.setConfigManagement("true");
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) vaddioBridgeNanoCommunicator.getMultipleStatistics().get(0);

		extendedStatistics = (ExtendedStatistics) vaddioBridgeNanoCommunicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		Assertions.assertEquals("AV Bridge Nano 1.0.1", stats.get("SystemVersion"));
		Assertions.assertEquals("1.02", stats.get("AudioVersion"));
	}

	/**
	 * Test Monitoring network settings
	 *
	 * Expect get network successfully
	 */
	@Test
	void testNetworkSettings() throws Exception {
		vaddioBridgeNanoCommunicator.setConfigManagement("true");
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
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
	 * Expect get Streaming Settings with USB port successfully
	 */
	@Test
	void testStreamingSettingsWithUSBStreaming() throws Exception {
		vaddioBridgeNanoCommunicator.setConfigManagement("true");
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();
		String key = "StreamingMode";
		String value = "0";
		controllableProperty.setValue(value);
		controllableProperty.setProperty(key);
		vaddioBridgeNanoCommunicator.controlProperty(controllableProperty);
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) vaddioBridgeNanoCommunicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		String group = "StreamingSettings#";
		Assertions.assertEquals("Enabled", stats.get(group + "HIDAudioControls"));
		Assertions.assertEquals("AV Bridge Nano", stats.get(group + "USBDeviceName"));
	}

	/**
	 * Test Streaming Settings with IP Streaming custom resolution
	 * Expect get IP Streaming custom resolution successfully
	 */
	@Test
	void testStreamingSettingsWithIPStreamingCustom() throws Exception {
		vaddioBridgeNanoCommunicator.setConfigManagement("true");
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();
		String key = "StreamingMode";
		String value = "1";
		controllableProperty.setValue(value);
		controllableProperty.setProperty(key);
		vaddioBridgeNanoCommunicator.controlProperty(controllableProperty);
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) vaddioBridgeNanoCommunicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		String group = "StreamingSettings#";
		Assertions.assertEquals("360p", stats.get(group + "IPPresetResolution"));
		Assertions.assertEquals("RTMP", stats.get(group + "IPProtocol"));
		Assertions.assertEquals("High Quality (Best)", stats.get(group + "IPVideoQuality"));
		Assertions.assertEquals("1935", stats.get(group + "IPRTMPPort"));
		Assertions.assertEquals("Service 1", stats.get(group + "IPRTMPService"));
		Assertions.assertEquals("true", stats.get(group + "IPStreaming"));
		Assertions.assertNull(stats.get(group + "IPBitrateMode"));
		Assertions.assertNull(stats.get(group + "IPRTSPMTU(bytes)"));
		Assertions.assertNull(stats.get(group + "IPRTSPURL"));
		Assertions.assertNull(stats.get(group + "IPRTSPPort"));
		Assertions.assertNull(stats.get(group + "IPCustomResolution"));
	}

	/**
	 * Test Streaming Settings with IP Streaming port
	 * Expect get network successfully
	 */
	@Test
	void testStreamingSettingsWithIPStreamingWithIPStreamingPort() throws Exception {
		vaddioBridgeNanoCommunicator.setConfigManagement("true");
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();
		String key = "StreamingMode";
		String value = "1";
		controllableProperty.setValue(value);
		controllableProperty.setProperty(key);
		vaddioBridgeNanoCommunicator.controlProperty(controllableProperty);
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) vaddioBridgeNanoCommunicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		String group = "StreamingSettings#";
		Assertions.assertEquals("360p", stats.get(group + "IPPresetResolution"));
		Assertions.assertEquals("true", stats.get(group + "IPStreaming"));
		Assertions.assertEquals("RTSP", stats.get(group + "IPProtocol"));
		Assertions.assertEquals("High Quality (Best)", stats.get(group + "IPVideoQuality"));
		Assertions.assertEquals("1400", stats.get(group + "IPRTSPMTU(bytes)"));
		Assertions.assertEquals("vaddio-avb-nano-stream", stats.get(group + "IPRTSPURL"));
		Assertions.assertEquals("554", stats.get(group + "IPRTSPPort"));
		Assertions.assertNull(stats.get(group + "IPRTMPPort"));
		Assertions.assertNull(stats.get(group + "IPRTMPService"));
		Assertions.assertNull(stats.get(group + "IPCustomResolution"));
		Assertions.assertNull(stats.get(group + "IPBitrateMode"));
	}


	/**
	 * Test Streaming Settings with IP Streaming Custom mode
	 * Expect get Streaming Settings with IP Streaming Custom mode successfully
	 */
	@Test
	void testStreamingSettingsWithIPStreamingWithCustom() throws Exception {
		vaddioBridgeNanoCommunicator.setConfigManagement("true");
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();
		String key = "StreamingMode";
		String value = "1";
		controllableProperty.setValue(value);
		controllableProperty.setProperty(key);
		vaddioBridgeNanoCommunicator.controlProperty(controllableProperty);
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) vaddioBridgeNanoCommunicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		String group = "StreamingSettings#";
		Assertions.assertNull(stats.get(group + "IPPresetResolution"));
		Assertions.assertEquals("Enabled", stats.get(group + "IPStreaming"));
		Assertions.assertEquals("RTSP", stats.get(group + "IPProtocol"));
		Assertions.assertEquals("Custom", stats.get(group + "IPVideoQuality"));
		Assertions.assertEquals("1400", stats.get(group + "IPRTSPMTU(bytes)"));
		Assertions.assertEquals("vaddio-avb-nano-stream", stats.get(group + "IPRTSPURL"));
		Assertions.assertEquals("554", stats.get(group + "IPRTSPPort"));
		Assertions.assertEquals("480p/25", stats.get(group + "IPCustomResolution"));
		Assertions.assertEquals("Variable", stats.get(group + "IPBitrateMode"));
		Assertions.assertNull(stats.get(group + "IPRTMPPort"));
		Assertions.assertNull(stats.get(group + "IPRTMPService"));
	}

	/**
	 * Test Control Video Mute
	 *
	 * Expect control video mute successfully
	 */
	@Test
	void testVideoMute() throws Exception {
		vaddioBridgeNanoCommunicator.setConfigManagement("true");
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();
		String key = "VideoMute";
		String value = "1";
		controllableProperty.setValue(value);
		controllableProperty.setProperty(key);
		vaddioBridgeNanoCommunicator.controlProperty(controllableProperty);
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) vaddioBridgeNanoCommunicator.getMultipleStatistics().get(0);
		List<AdvancedControllableProperty> advancedControllableProperty = extendedStatistics.getControllableProperties();
		String currentValue = String.valueOf(advancedControllableProperty.stream().filter(item -> item.getName().equals(key)).findFirst().get().getValue());
		Assertions.assertEquals("1", currentValue);
	}

	/**
	 * Test Control mute off
	 *
	 * Expect mute audio input successfully
	 */
	@Test
	void testAudioInputMuteOFF() throws Exception {
		vaddioBridgeNanoCommunicator.setConfigManagement("true");
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();
		String key = "LineInLeft#Mute";
		String value = "0";
		controllableProperty.setValue(value);
		controllableProperty.setProperty(key);
		vaddioBridgeNanoCommunicator.controlProperty(controllableProperty);
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) vaddioBridgeNanoCommunicator.getMultipleStatistics().get(0);
		List<AdvancedControllableProperty> advancedControllableProperty = extendedStatistics.getControllableProperties();
		String currentValue = String.valueOf(advancedControllableProperty.stream().filter(item -> item.getName().equals(key)).findFirst().get().getValue());
		Assertions.assertEquals("0", currentValue);
	}

	/**
	 * Test Control mute on
	 *
	 * Expect mute audio input successfully
	 */
	@Test
	void testAudioInputMuteON() throws Exception {
		vaddioBridgeNanoCommunicator.setConfigManagement("true");
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();
		String key = "LineInLeft#Mute";
		String value = "1";
		controllableProperty.setValue(value);
		controllableProperty.setProperty(key);
		vaddioBridgeNanoCommunicator.controlProperty(controllableProperty);
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) vaddioBridgeNanoCommunicator.getMultipleStatistics().get(0);
		List<AdvancedControllableProperty> advancedControllableProperty = extendedStatistics.getControllableProperties();
		String currentValue = String.valueOf(advancedControllableProperty.stream().filter(item -> item.getName().equals(key)).findFirst().get().getValue());
		Assertions.assertEquals("1", currentValue);
	}

	/**
	 * Test Control volume
	 *
	 * Expect control volume audio input successfully
	 */
	@Test
	void testAudioInputVolume() throws Exception {
		vaddioBridgeNanoCommunicator.setConfigManagement("true");
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();
		String key = "LineInLeft#Volume(dB)";
		String value = "-1";
		controllableProperty.setValue(value);
		controllableProperty.setProperty(key);
		vaddioBridgeNanoCommunicator.controlProperty(controllableProperty);
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) vaddioBridgeNanoCommunicator.getMultipleStatistics().get(0);
		List<AdvancedControllableProperty> advancedControllableProperty = extendedStatistics.getControllableProperties();
		String currentValue = String.valueOf(advancedControllableProperty.stream().filter(item -> item.getName().equals(key)).findFirst().get().getValue());
		Assertions.assertEquals("-1.0", currentValue);
		Assertions.assertEquals("-1", extendedStatistics.getStatistics().get("LineInLeft#VolumeCurrentValue(dB)"));
	}

	/**
	 * Test Control volume
	 *
	 * Expect control volume audio input successfully
	 */
	@Test
	void testCrosspointGainHDMIOutLeft() throws Exception {
		vaddioBridgeNanoCommunicator.setConfigManagement("true");
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();
		String key = "CrosspointGainHDMIOutLeft#HDMIInLeftGain(dB)";
		String value = "-1";
		controllableProperty.setValue(value);
		controllableProperty.setProperty(key);
		vaddioBridgeNanoCommunicator.controlProperty(controllableProperty);
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) vaddioBridgeNanoCommunicator.getMultipleStatistics().get(0);
		List<AdvancedControllableProperty> advancedControllableProperty = extendedStatistics.getControllableProperties();
		String currentValue = String.valueOf(advancedControllableProperty.stream().filter(item -> item.getName().equals(key)).findFirst().get().getValue());
		Assertions.assertEquals("-1.0", currentValue);
		Assertions.assertEquals("-1", extendedStatistics.getStatistics().get("CrosspointGainHDMIOutLeft#HDMIInLeftGainCurrentValue(dB)"));
	}

	/**
	 * Test Control route on
	 *
	 * Expect control route on successfully
	 */
	@Test
	void testCrosspointHDMIOutLeftRouteON() throws Exception {
		vaddioBridgeNanoCommunicator.setConfigManagement("true");
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();
		String key = "CrosspointHDMIOutLeft#HDMIInLeftRoute";
		String value = "1";
		controllableProperty.setValue(value);
		controllableProperty.setProperty(key);
		vaddioBridgeNanoCommunicator.controlProperty(controllableProperty);
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) vaddioBridgeNanoCommunicator.getMultipleStatistics().get(0);
		List<AdvancedControllableProperty> advancedControllableProperty = extendedStatistics.getControllableProperties();
		String currentValue = String.valueOf(advancedControllableProperty.stream().filter(item -> item.getName().equals(key)).findFirst().get().getValue());
		Assertions.assertEquals("1", currentValue);
	}

	/**
	 * Test Control route off
	 *
	 * Expect control route off successfully
	 */
	@Test
	void testCrosspointHDMIOutLeftRouteOFF() throws Exception {
		vaddioBridgeNanoCommunicator.setConfigManagement("true");
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();
		String key = "CrosspointHDMIOutLeft#HDMIInLeftRoute";
		String value = "0";
		controllableProperty.setValue(value);
		controllableProperty.setProperty(key);
		vaddioBridgeNanoCommunicator.controlProperty(controllableProperty);
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) vaddioBridgeNanoCommunicator.getMultipleStatistics().get(0);
		List<AdvancedControllableProperty> advancedControllableProperty = extendedStatistics.getControllableProperties();
		String currentValue = String.valueOf(advancedControllableProperty.stream().filter(item -> item.getName().equals(key)).findFirst().get().getValue());
		Assertions.assertEquals("0", currentValue);
	}

	/**
	 * Test Reboot Control
	 *
	 * Expect device reboot successfully
	 */
	@Test
	void testReboot() throws Exception {
		vaddioBridgeNanoCommunicator.setConfigManagement("true");
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		vaddioBridgeNanoCommunicator.getMultipleStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();
		String key = "SystemReboot";
		String value = "1";
		controllableProperty.setValue(value);
		controllableProperty.setProperty(key);
		vaddioBridgeNanoCommunicator.controlProperty(controllableProperty);
		Assertions.assertThrows(SocketTimeoutException.class, () -> vaddioBridgeNanoCommunicator.getMultipleStatistics(), "Expect timeout exception, due to the device already rebootin");
	}
}
