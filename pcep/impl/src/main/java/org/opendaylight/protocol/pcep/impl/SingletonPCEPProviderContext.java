/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import javax.annotation.concurrent.ThreadSafe;

import org.opendaylight.protocol.pcep.spi.LabelHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.MessageHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPProviderContext;
import org.opendaylight.protocol.pcep.spi.SubobjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;

/**
 *
 */
@ThreadSafe
public final class SingletonPCEPProviderContext implements PCEPProviderContext {
	private static final class Holder {
		private static final PCEPProviderContext INSTANCE;

		static {
			final PCEPProviderContext pc = new SingletonPCEPProviderContext();
			new PCEPImplActivator().start(pc);
			INSTANCE = pc;
		}
	}

	private final LabelHandlerRegistry labelReg = new SimpleLabelHandlerRegistry();
	private final MessageHandlerRegistry msgReg = new SimpleMessageHandlerRegistry();
	private final ObjectHandlerRegistry objReg = new SimpleObjectHandlerRegistry();
	private final SubobjectHandlerRegistry subobjReg =  new SimpleSubobjectHandlerFactory();
	private final TlvHandlerRegistry tlvReg = new SimpleTlvHandlerRegistry();

	private SingletonPCEPProviderContext() {

	}

	public static PCEPProviderContext getInstance() {
		return Holder.INSTANCE;
	}

	@Override
	public LabelHandlerRegistry getHandlerRegistry() {
		return labelReg;
	}

	@Override
	public MessageHandlerRegistry getMessageHandlerRegistry() {
		return msgReg;
	}

	@Override
	public ObjectHandlerRegistry getObjectHandlerRegistry() {
		return objReg;
	}

	@Override
	public SubobjectHandlerRegistry getSubobjectHandlerRegistry() {
		return subobjReg;
	}

	@Override
	public TlvHandlerRegistry getTlvHandlerRegistry() {
		return tlvReg;
	}
}
