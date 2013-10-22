/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import org.opendaylight.protocol.bgp.parser.BGPMessageFactory;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.framework.DeserializerException;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.yangtools.yang.binding.Notification;

import com.google.common.base.Preconditions;

public final class BGPMessageFactoryImpl implements BGPMessageFactory {
	private final MessageRegistry registry;

	public BGPMessageFactoryImpl(final MessageRegistry registry) {
		this.registry = Preconditions.checkNotNull(registry);
	}

	/*
	 * (non-Javadoc)
	 * @see org.opendaylight.protocol.bgp.parser.BGPMessageParser#parse(byte[])
	 */
	@Override
	public Notification parse(final byte[] bytes) throws DeserializerException, DocumentedException {
		return this.registry.parseMessage(bytes);
	}

	@Override
	public byte[] put(final Notification msg) {
		return this.registry.serializeMessage(msg);
	}
}
