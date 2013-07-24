/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import java.net.InetAddress;

import org.opendaylight.protocol.framework.SessionListenerFactory;

/**
 * Factory for generating PCEP Session Listeners. Used by a server.
 */
public abstract class PCEPSessionListenerFactory implements SessionListenerFactory {

	/**
	 * Returns one session listener that is registered to this factory
	 * @param address serves as constraint, so that factory is able to
	 * return different listeners for different factories
	 * @return specific session listener
	 */
	@Override
	public abstract PCEPSessionListener getSessionListener(InetAddress address);

}
