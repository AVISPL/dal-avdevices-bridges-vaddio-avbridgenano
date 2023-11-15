/*
 * Copyright (c) 2023 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common;

/**
 * VaddioNanoConstant provides Constant class during the monitoring and controlling process
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 10/24/2023
 * @since 1.0.0
 */
public class VaddioNanoConstant {
	public static final String NONE = "None";
	public static final String OFF = "Off";
	public static final String ON = "On";
	public static final String GATEWAY = "Gateway";
	public static final String HOSTNAME = "HostName";
	public static final String IP_ADDRESS = "IPAddress";
	public static final String INTERFACE_NAME = "InterfaceName";
	public static final String MAC_ADDRESS = "MACAddress";
	public static final String SUBNET_MASK = "SubnetMask";
	public static final String VLAN = "VLAN";
	public static final String IP_CUSTOM_RESOLUTION = "IPCustomResolution";
	public static final String IP_PRESET_RESOLUTION = "IPPresetResolution";
	public static final String IP_PROTOCOL = "IPProtocol";
	public static final String IP_BIT_RATE_MODE = "IPBitrateMode";
	public static final String IP_MAX_BANDWIDTH = "IPMaxBandwidth(bps)";
	public static final String IP_RTMP_PORT = "IPRTMPPort";
	public static final String IP_RTMP_SERVICE = "IPRTMPService";
	public static final String IP_RTSP_MTU = "IPRTSPMTU(bytes)";
	public static final String IP_RTSP_PORT = "IPRTSPPort";
	public static final String IP_RTSP_URL = "IPRTSPURL";
	public static final String IP_STREAMING_ENABLED = "IPStreamingEnabled";
	public static final String IP_VIDEO_QUALITY = "IPVideoQuality";
	public static final String HID_AUDIO_CONTROLS_ENABLED = "HIDAudioControlsEnabled";
	public static final String USB_ACTIVE = "USBActive";
	public static final String USB_DEVICE = "USBDevice";
	public static final String USB_ENUMERATION_SPEED = "USBEnumerationSpeed";
	public static final String USB_RESOLUTION = "USBResolution";
	public static final String STREAMING_SETTINGS = "StreamingSettings";
	public static final String NETWORK_SETTINGS = "NetworkSettings";
	public static final String USB_STREAM_MODE = "USB streaming mode";
	public static final String IP_STREAM_MODE = "IP streaming mode";
	public static final String HASH = "#";
	public static final String MUTE = "Mute";
	public static final String CROSSPOINT_GAIN = "CrosspointGain";
	public static final String ENABLED_ROUTES = "EnabledRoutes";
	public static final String VOLUME = "Volume(dB)";
	public static final String VOLUME_CURRENT_VALUE = "VolumeCurrentValue(dB)";
	public static final String GAIN = "Gain(dB)";
	public static final String GAIN_CURRENT_VALUE = "GainCurrentValue(dB)";
	public static final String REGEX_RESPONSE = "\u001B|\\[|0;37m|0m";
	public static final String VOLUME_REGEX = "volume:(.*?)\r\n";
	public static final String MUTE_REGEX = "mute:(.*?)\r\n";
	public static final String EMPTY = "";
	public static final String SPACE = " ";
	public static final String SYSTEM_REBOOT = "SystemReboot";
	public static final String SYSTEM_REBOOT_DELAY = "SystemRebootDelay(s)";
	public static final String REBOOT = "Reboot";
	public static final String REBOOTING = "Rebooting";
	public static final String TRUE = "True";
	public static final String FALSE = "False";
	public static final String CUSTOM = "Custom";
	public static final String USB = "USB";
	public static final String IP = "IP";
	public static final String AUDIO_VERSION = "AudioVersion";
	public static final String ERROR_RESPONSE = "Syntax error";
	public static final String AUDIO_REGEX = "Audio(.*?)\r\n";
	public static final String SYSTEM_VERSION_REGEX = "System Version(.*?)\r\n";
	public static final int NUMBER_ONE = 1;
	public static final float MIN_VOLUME = -42f;
	public static final float MIN_VOLUME_LINE = -48f;
	public static final float MAX_VOLUME = 6f;
	public static final float MIN_GAIN = -12f;
	public static final float MAX_GAIN = 12f;
}