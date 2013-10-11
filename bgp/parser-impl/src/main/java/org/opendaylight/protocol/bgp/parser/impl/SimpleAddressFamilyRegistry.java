/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;

import com.google.common.base.Preconditions;

public final class SimpleAddressFamilyRegistry implements AddressFamilyRegistry {
	public static final AddressFamilyRegistry INSTANCE;

	static {
		final AddressFamilyRegistry reg = new SimpleAddressFamilyRegistry();

		reg.registerAddressFamily(Ipv4AddressFamily.class, 1);
		reg.registerAddressFamily(Ipv6AddressFamily.class, 2);
		reg.registerAddressFamily(LinkstateAddressFamily.class, 16388);

		INSTANCE = reg;
	}

	private final SimpleFamilyRegistry<AddressFamily, Integer> registry = new SimpleFamilyRegistry<>();

	@Override
	public AutoCloseable registerAddressFamily(final Class<? extends AddressFamily> clazz, final int number) {
		Preconditions.checkArgument(number >= 0 && number <= 65535);
		return registry.registerFamily(clazz, number);
	}

	@Override
	public Class<? extends AddressFamily> classForFamily(final int number) {
		return registry.classForFamily(number);
	}

	@Override
	public Integer numberForClass(final Class<? extends AddressFamily> clazz) {
		return registry.numberForClass(clazz);
	}
}
