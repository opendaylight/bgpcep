/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

public class SimpleMessageFactory implements ProtocolMessageFactory {

	@Override
	public ProtocolMessage parse(byte[] bytes, ProtocolMessageHeader msgHeader)
			throws DeserializerException, DocumentedException {
		return null;
	}

	@Override
	public byte[] put(ProtocolMessage msg) {
		return null;
	}
}
