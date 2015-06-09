/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.api;

import io.netty.channel.ChannelInboundHandler;
import java.net.InetAddress;

/**
 * BMP Session represents the finite state machine in BMP, its purpose is to create a BMP connection between
 * Peer and monitoring application. Session is automatically started, when TCP connection is created, but can be stopped
 * manually via close method of the {@link java.io.Closeable} interface.
 * If the session is up, it has to redirect messages to/from user. Handles also malformed messages and unknown requests.
 */
public interface BmpSession extends ChannelInboundHandler, AutoCloseable {

    InetAddress getRemoteAddress();
}
