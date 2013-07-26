/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import io.netty.channel.ChannelHandlerContext;

import java.util.Timer;

import org.opendaylight.protocol.framework.ProtocolConnection;
import org.opendaylight.protocol.framework.ProtocolSession;
import org.opendaylight.protocol.framework.ProtocolSessionFactory;
import org.opendaylight.protocol.framework.SessionParent;

public interface PCEPSessionFactory extends ProtocolSessionFactory {
	@Override
	public ProtocolSession getProtocolSession(SessionParent parent, Timer timer, ProtocolConnection connection, int sessionId,
			ChannelHandlerContext ctx);
}
