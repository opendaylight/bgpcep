/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.AttributeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityRegistry;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.parser.spi.NlriRegistry;
import org.opendaylight.protocol.bgp.parser.spi.ParameterRegistry;
import org.opendaylight.protocol.bgp.parser.spi.ProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;

public final class SingletonProviderContext implements ProviderContext {
	private static final class Holder {
		private static final ProviderContext INSTANCE = new SingletonProviderContext();
	}

	private SingletonProviderContext() {

	}

	public static ProviderContext getInstance() {
		return Holder.INSTANCE;
	}

	@Override
	public AddressFamilyRegistry getAddressFamilyRegistry() {
		return SimpleAddressFamilyRegistry.getInstance();
	}

	@Override
	public AttributeRegistry getAttributeRegistry() {
		return SimpleAttributeRegistry.getInstance();
	}

	@Override
	public CapabilityRegistry getCapabilityRegistry() {
		return SimpleCapabilityRegistry.getInstance();
	}

	@Override
	public MessageRegistry getMessageRegistry() {
		return SimpleBGPMessageFactory.getInstance();
	}

	@Override
	public NlriRegistry getNlriRegistry() {
		return SimpleNlriRegistry.getInstance();
	}

	@Override
	public ParameterRegistry getParameterRegistry() {
		return SimpleParameterRegistry.getInstance();
	}

	@Override
	public SubsequentAddressFamilyRegistry getSubsequentAddressFamilyRegistry() {
		return SimpleSubsequentAddressFamilyRegistry.getInstance();
	}
}
