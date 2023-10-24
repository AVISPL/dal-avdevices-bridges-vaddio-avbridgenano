/*
 * Copyright (c) 2023 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.util.CollectionUtils;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.dal.communicator.SshCommunicator;

/**
 * VaddioBridgeNanoCommunicator An implementation of SshCommunicator to provide communication and interaction with Vaddio Bridge Nano device
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/14/2023
 * @since 1.0.0
 */
public class VaddioBridgeNanoCommunicator extends SshCommunicator implements Monitorable, Controller {

	/**
	 * Prevent case where {@link VaddioBridgeNanoCommunicator#controlProperty(ControllableProperty)} slow down -
	 * the getMultipleStatistics interval if it's fail to doGet the cmd
	 */
	private static final int controlSSHTimeout = 3000;
	/**
	 * Set back to default timeout value in {@link SshCommunicator}
	 */
	private static final int statisticsSSHTimeout = 30000;

	/**
	 * ReentrantLock to prevent telnet session is closed when adapter is retrieving statistics from the device.
	 */
	private final ReentrantLock reentrantLock = new ReentrantLock();

	/**
	 * Store previous/current ExtendedStatistics
	 */
	private ExtendedStatistics localExtendedStatistics;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Statistics> getMultipleStatistics() {
		ExtendedStatistics extendedStatistics = new ExtendedStatistics();
		List<AdvancedControllableProperty> advancedControllableProperty = new ArrayList<>();
		Map<String, String> stats = new HashMap<>();
		reentrantLock.lock();
		try {
			extendedStatistics.setStatistics(stats);
			extendedStatistics.setControllableProperties(advancedControllableProperty);
			localExtendedStatistics = extendedStatistics;
		} finally {
			reentrantLock.unlock();
		}
		return Collections.singletonList(localExtendedStatistics);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalInit() throws Exception {
		super.internalInit();
		this.createChannel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalDestroy() {
		if (localExtendedStatistics != null && localExtendedStatistics.getStatistics() != null && localExtendedStatistics.getControllableProperties() != null) {
			localExtendedStatistics.getStatistics().clear();
			localExtendedStatistics.getControllableProperties().clear();
		}
		super.internalDestroy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void controlProperty(ControllableProperty controllableProperty) {
		String property = controllableProperty.getProperty();
		String value = String.valueOf(controllableProperty.getValue());
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Perform control operation with property: %s and value: %s", property, value));
		}
		reentrantLock.lock();
		try {
			this.timeout = controlSSHTimeout;
			if (localExtendedStatistics == null) {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Error while controlling %s metric", property));
				}
				return;
			}
		} finally {
			this.timeout = statisticsSSHTimeout;
			reentrantLock.unlock();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void controlProperties(List<ControllableProperty> list) throws Exception {
		if (CollectionUtils.isEmpty(list)) {
			throw new IllegalArgumentException("ControllableProperties can not be null or empty");
		}
		for (ControllableProperty p : list) {
			try {
				controlProperty(p);
			} catch (Exception e) {
				logger.error(String.format("Error when control property %s", p.getProperty()), e);
			}
		}
	}
}