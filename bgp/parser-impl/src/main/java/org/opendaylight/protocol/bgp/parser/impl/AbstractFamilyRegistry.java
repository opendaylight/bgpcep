/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.protocol.concepts.AbstractRegistration;

import com.google.common.base.Preconditions;

abstract class AbstractFamilyRegistry<CLASS, NUMBER> {
	private final Map<Class<? extends CLASS>, NUMBER> classToNumber = new ConcurrentHashMap<>();
	private final Map<NUMBER, Class<? extends CLASS>> numberToClass = new ConcurrentHashMap<>();

	protected synchronized AutoCloseable registerFamily(final Class<? extends CLASS> clazz, final NUMBER number) {
		Preconditions.checkNotNull(clazz);

		final Class<?> c = numberToClass.get(number);
		Preconditions.checkState(c == null, "Number " + number + " already registered to " + c);

		final NUMBER n = classToNumber.get(clazz);
		Preconditions.checkState(n == null, "Class " + clazz + " already registered to " + n);

		numberToClass.put(number, clazz);
		classToNumber.put(clazz, number);

		final Object lock = this;
		return new AbstractRegistration() {

			@Override
			protected void removeRegistration() {
				synchronized (lock) {
					classToNumber.remove(clazz);
					numberToClass.remove(number);
				}
			}
		};
	}

	protected Class<? extends CLASS> classForFamily(final NUMBER number) {
		return numberToClass.get(number);
	}

	protected NUMBER numberForClass(final Class<? extends CLASS> clazz) {
		return classToNumber.get(clazz);
	}
}
