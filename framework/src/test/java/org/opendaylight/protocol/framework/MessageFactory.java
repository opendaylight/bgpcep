/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import java.nio.ByteBuffer;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;

public class MessageFactory implements ProtocolMessageFactory<SimpleMessage> {

	@Override
	public List<SimpleMessage> parse(final byte[] bytes) throws DeserializerException, DocumentedException {
		return Lists.newArrayList(new SimpleMessage(Charsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString()));
	}

	@Override
	public byte[] put(final SimpleMessage msg) {
		return msg.getMessage().getBytes();
	}
}
