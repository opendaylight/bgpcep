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
		private static final ProviderContext INSTANCE;

		static {
			final ProviderContext pc = new SingletonProviderContext();
			new ActivatorImpl().start(pc);
			INSTANCE = pc;
		}
	}

	private final AddressFamilyRegistry afiReg = new SimpleAddressFamilyRegistry();
	private final AttributeRegistry attrReg = new SimpleAttributeRegistry();
	private final CapabilityRegistry capReg = new SimpleCapabilityRegistry();
	private final MessageRegistry msgReg = new SimpleMessageRegistry();
	private final NlriRegistry nlriReg;
	private final ParameterRegistry paramReg = new SimpleParameterRegistry();
	private final SubsequentAddressFamilyRegistry safiReg= new SimpleSubsequentAddressFamilyRegistry();

	private SingletonProviderContext() {
		nlriReg = new SimpleNlriRegistry(afiReg, safiReg);
	}

	public static ProviderContext getInstance() {
		return Holder.INSTANCE;
	}

	@Override
	public AddressFamilyRegistry getAddressFamilyRegistry() {
		return afiReg;
	}

	@Override
	public AttributeRegistry getAttributeRegistry() {
		return attrReg;
	}

	@Override
	public CapabilityRegistry getCapabilityRegistry() {
		return capReg;
	}

	@Override
	public MessageRegistry getMessageRegistry() {
		return msgReg;
	}

	@Override
	public NlriRegistry getNlriRegistry() {
		return nlriReg;
	}

	@Override
	public ParameterRegistry getParameterRegistry() {
		return paramReg;
	}

	@Override
	public SubsequentAddressFamilyRegistry getSubsequentAddressFamilyRegistry() {
		return safiReg;
	}
}
