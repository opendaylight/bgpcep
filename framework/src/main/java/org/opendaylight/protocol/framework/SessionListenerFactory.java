/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import java.net.InetAddress;

/**
 * Factory for generating Session Listeners. Used by a server. This interface should be
 * implemented by a protocol specific abstract class, that is extended by
 * a final class that implements the methods.
 */
public interface SessionListenerFactory {
	/**
	 * Returns one session listener
	 * @param address serves as constraint, so that factory is able to
	 * return different listeners for different factories
	 * @return specific session listener
	 */
	public SessionListener getSessionListener(final InetAddress address);
}
