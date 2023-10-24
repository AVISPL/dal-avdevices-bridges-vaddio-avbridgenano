/*
 *  Copyright (c) 2023 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * VaddioBridgeNanoCommunicatorTest class
 */
public class VaddioBridgeNanoCommunicatorTest {
	private VaddioBridgeNanoCommunicator vaddioBridgeNanoCommunicator;

	@BeforeEach()
	public void setUp() throws Exception {
		vaddioBridgeNanoCommunicator = new VaddioBridgeNanoCommunicator();

		vaddioBridgeNanoCommunicator.setHost("");
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
}
