/*
 * Copyright (c) 2023 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.avdevices.bridges.vaddio.avbridgenano.common;

import java.lang.reflect.Method;

import com.avispl.symphony.api.dal.error.ResourceNotReachableException;

/**
 * EnumTypeHandler class provides during the monitoring and controlling process
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 11/9/2023
 * @since 1.0.0
 */
public class EnumTypeHandler {
	/**
	 * Get value by name
	 *
	 * @param enumType the enum type is enum class
	 * @param name is String
	 * @return T is metric instance
	 */
	public static <T extends Enum<T>> String getValueByName(Class<T> enumType, String name) {
		try {
			for (T metric : enumType.getEnumConstants()) {
				Method methodName = metric.getClass().getMethod("getName");
				String nameMetric = (String) methodName.invoke(metric); // getName executed
				if (name.equals(nameMetric)) {
					Method methodValue = metric.getClass().getMethod("getValue");
					return methodValue.invoke(metric).toString();
				}
			}
			throw new ResourceNotReachableException("Fail to get enum " + enumType.getSimpleName() + " with name is " + name);
		} catch (Exception e) {
			throw new ResourceNotReachableException(e.getMessage(), e);
		}
	}

	/**
	 * Get name by value
	 *
	 * @param enumType the enum type is enum class
	 * @param value is String
	 * @return T is metric instance
	 */
	public static <T extends Enum<T>> String getNameByValue(Class<T> enumType, String value) {
		try {
			for (T metric : enumType.getEnumConstants()) {
				Method methodValue = metric.getClass().getMethod("getValue");
				String valueMetric = methodValue.invoke(metric).toString(); // getName executed
				if (value.equals(valueMetric)) {
					Method methodName = metric.getClass().getMethod("getName");
					return methodName.invoke(metric).toString();
				}
			}
			throw new ResourceNotReachableException("Fail to get enum " + enumType.getSimpleName() + " with value is " + value);
		} catch (Exception e) {
			throw new ResourceNotReachableException(e.getMessage(), e);
		}
	}
}