/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi.osgi;

import org.opendaylight.protocol.pcep.spi.EROSubobjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.LabelHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.MessageHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionConsumerContext;
import org.opendaylight.protocol.pcep.spi.RROSubobjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.XROSubobjectHandlerRegistry;
import org.osgi.framework.BundleContext;

import com.google.common.base.Preconditions;

class OSGiPCEPExtensionConsumerContext implements PCEPExtensionConsumerContext {
	protected final BundleContext bundleContext;

	public OSGiPCEPExtensionConsumerContext(final BundleContext context) {
		this.bundleContext = Preconditions.checkNotNull(context);
	}

	@Override
	public LabelHandlerRegistry getLabelHandlerRegistry() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MessageHandlerRegistry getMessageHandlerRegistry() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectHandlerRegistry getObjectHandlerRegistry() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EROSubobjectHandlerRegistry getEROSubobjectHandlerRegistry() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RROSubobjectHandlerRegistry getRROSubobjectHandlerRegistry() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public XROSubobjectHandlerRegistry getXROSubobjectHandlerRegistry() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TlvHandlerRegistry getTlvHandlerRegistry() {
		// TODO Auto-generated method stub
		return null;
	}
}
