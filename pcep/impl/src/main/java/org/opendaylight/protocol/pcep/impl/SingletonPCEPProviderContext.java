/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import javax.annotation.concurrent.ThreadSafe;

import org.opendaylight.protocol.pcep.spi.EROSubobjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.LabelHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.MessageHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPProviderContext;
import org.opendaylight.protocol.pcep.spi.RROSubobjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.XROSubobjectHandlerRegistry;

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
	private final EROSubobjectHandlerRegistry eroSubReg = new SimpleEROSubobjectHandlerFactory();
	private final RROSubobjectHandlerRegistry rroSubReg = new SimpleRROSubobjectHandlerFactory();
	private final XROSubobjectHandlerRegistry xroSubReg = new SimpleXROSubobjectHandlerFactory();
	private final TlvHandlerRegistry tlvReg = new SimpleTlvHandlerRegistry();

	private SingletonPCEPProviderContext() {

	}

	public static PCEPProviderContext getInstance() {
		return Holder.INSTANCE;
	}

	@Override
	public LabelHandlerRegistry getLabelHandlerRegistry() {
		return labelReg;
	}

	@Override
	public MessageHandlerRegistry getMessageHandlerRegistry() {
		return this.msgReg;
	}

	@Override
	public ObjectHandlerRegistry getObjectHandlerRegistry() {
		return this.objReg;
	}

	@Override
	public EROSubobjectHandlerRegistry getEROSubobjectHandlerRegistry() {
		return this.eroSubReg;
	}

	@Override
	public RROSubobjectHandlerRegistry getRROSubobjectHandlerRegistry() {
		return this.rroSubReg;
	}

	@Override
	public XROSubobjectHandlerRegistry getXROSubobjectHandlerRegistry() {
		return this.xroSubReg;
	}

	@Override
	public TlvHandlerRegistry getTlvHandlerRegistry() {
		return this.tlvReg;
	}
}
