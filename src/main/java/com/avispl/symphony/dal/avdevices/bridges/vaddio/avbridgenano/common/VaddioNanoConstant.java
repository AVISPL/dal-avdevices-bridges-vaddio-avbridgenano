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
	public static final String OK = "OK";
	public static final String OFF = "Off";
	public static final String ON = "On";
	public static final String GATEWAY = "Gateway";
	public static final String HOSTNAME = "HostName";
	public static final String IP_ADDRESS = "IPAddress";
	public static final String INTERFACE_NAME = "InterfaceName";
	public static final String MAC_ADDRESS = "MACAddress";
	public static final String SUBNET_MASK = "SubnetMask";
	public static final String VLAN = "VLAN";
	public static final String DISABLE = "Disabled";
	public static final String ENABLE = "Enabled";
	public static final String IP_CUSTOM_RESOLUTION = "IPCustomResolution";
	public static final String IP_PRESET_RESOLUTION = "IPPresetResolution";
	public static final String IP_PROTOCOL = "IPProtocol";
	public static final String IP_BIT_RATE_MODE = "IPBitRateMode";
	public static final String IP_MAX_BANDWIDTH = "IPMaxBandwidth(bps)";
	public static final String IP_RTMP_PORT = "IPRTMPPort";
	public static final String IP_RTMP_SERVICE = "IPRTMPService";
	public static final String IP_RTSP_MTU = "IPRTSPMTU(bytes)";
	public static final String IP_RTSP_PORT = "IPRTSPPort";
	public static final String IP_RTSP_URL = "IPRTSPPath";
	public static final String IP_STREAMING_ENABLED = "IPStreaming";
	public static final String IP_VIDEO_QUALITY = "IPVideoQuality";
	public static final String HID_AUDIO_CONTROLS_ENABLED = "HIDAudioControls";
	public static final String USB_DEVICE = "USBDeviceName";
	public static final String STREAMING_SETTINGS = "StreamingSettings";
	public static final String NETWORK_SETTINGS = "NetworkSettings";
	public static final String IP_STREAM_MODE = "IP streaming mode";
	public static final String STREAM_MODE = "StreamingMode";
	public static final String AUDIO_MUTE = "AudioMute";
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
	public static final String DEFAULT_REBOOT = "30";
	public static final String SPACE = " ";
	public static final String SYSTEM_REBOOT = "SystemReboot";
	public static final String SYSTEM_REBOOT_DELAY = "SystemRebootDelay(s)";
	public static final String REBOOT = "Reboot";
	public static final String REBOOTING = "Rebooting";
	public static final String TRUE = "True";
	public static final String FALSE = "False";
	public static final String RTMP = "RTMP";
	public static final String RTSP = "RTSP";
	public static final String CUSTOM = "Custom";
	public static final String USB = "USB";
	public static final String IP = "IP";
	public static final String AUDIO_VERSION = "AudioVersion";
	public static final String ERROR_RESPONSE = "Syntax error";
	public static final String AUDIO_REGEX = "Audio(.*?)\r\n";
	public static final String VIDEO_MUTE_REGEX = "mute:(.*?)\r\n";
	public static final String SYSTEM_VERSION_REGEX = "System Version(.*?)\r\n";
	public static final String GAIN_COMMAND = " crosspoint-gain ";
	public static final String ROUTES_COMMAND = " route get";
	public static final String MUTE_COMMAND = " mute get";
	public static final String VOLUME_COMMAND = " volume get";
	public static final String VOLUME_CONTROL = " volume ";
	public static final String MUTE_CONTROL = " mute ";
	public static final String GET = " get";
	public static final String SET = " set ";
	public static final String VIDEO_MUTE = "VideoMute";
	public static final String AUDIO_COMMAND = "audio ";
	public static final int NUMBER_ONE = 1;
	public static final String MIN_VOLUME = "-42";
	public static final String MIN_VOLUME_LINE = "-48";
	public static final String MAX_VOLUME = "6";
	public static final String MIN_GAIN = "-12";
	public static final String MAX_GAIN = "12";
	public static final String DOT = ".";
	public static final String DOT_REGEX = "\\.";
	public static final String DASH = "-";
	public static final String REBOOT_MESSAGE = "timeout: socket is not established";
	public static final int MIN_REBOOT = 0;
	public static final int MAX_REBOOT = 60;
}