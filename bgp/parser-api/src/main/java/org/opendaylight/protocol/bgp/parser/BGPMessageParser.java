/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import java.io.Closeable;

import org.opendaylight.protocol.framework.DeserializerException;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.protocol.framework.ProtocolMessageFactory;
import org.opendaylight.protocol.framework.ProtocolMessageHeader;

/**
 * Parser for BGP Messages.
 */
public interface BGPMessageParser extends ProtocolMessageFactory, Closeable {

	@Override
	public BGPMessage parse(byte[] bytes, ProtocolMessageHeader msgHeader) throws DeserializerException, DocumentedException;
}
