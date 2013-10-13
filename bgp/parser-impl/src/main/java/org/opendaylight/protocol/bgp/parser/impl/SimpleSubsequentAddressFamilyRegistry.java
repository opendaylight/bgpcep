/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;

import com.google.common.base.Preconditions;

public final class SimpleSubsequentAddressFamilyRegistry implements SubsequentAddressFamilyRegistry {
	private static final class Holder {
		private static final SubsequentAddressFamilyRegistry INSTANCE =
				new SimpleSubsequentAddressFamilyRegistry();
	}

	private final SimpleFamilyRegistry<SubsequentAddressFamily, Integer> registry = new SimpleFamilyRegistry<>();

	private SimpleSubsequentAddressFamilyRegistry() {

	}

	public static final SubsequentAddressFamilyRegistry getInstance() {
		return Holder.INSTANCE;
	}

	@Override
	public AutoCloseable registerSubsequentAddressFamily(final Class<? extends SubsequentAddressFamily> clazz, final int number) {
		Preconditions.checkArgument(number >= 0 && number <= 255);
		return registry.registerFamily(clazz, number);
	}

	@Override
	public Class<? extends SubsequentAddressFamily> classForFamily(final int number) {
		return registry.classForFamily(number);
	}

	@Override
	public Integer numberForClass(final Class<? extends SubsequentAddressFamily> clazz) {
		return registry.numberForClass(clazz);
	}
}
