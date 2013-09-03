/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import java.util.Set;

import org.opendaylight.protocol.bgp.concepts.BGPTableType;
import org.opendaylight.protocol.framework.ProtocolSession;

/**
 * BGP Session represents the finite state machine in BGP, including timers and its purpose is to create a BGP
 * connection between BGP speakers. Session is automatically started, when TCP connection is created, but can be stopped
 * manually via close method of the Closeable interface.
 * 
 * If the session is up, it has to redirect messages to/from user. Handles also malformed messages and unknown requests.
 */
public interface BGPSession extends ProtocolSession<BGPMessage> {

	public Set<BGPTableType> getAdvertisedTableTypes();
}
