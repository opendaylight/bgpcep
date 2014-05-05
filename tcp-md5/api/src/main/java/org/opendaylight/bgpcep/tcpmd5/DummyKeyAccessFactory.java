/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5;

import java.nio.channels.Channel;

import com.google.common.base.Preconditions;

/**
 * Dummy KeyAccessFactory. This factory does not support any channels
 * and it does not give out any KeyAccess objects.
 */
public final class DummyKeyAccessFactory implements KeyAccessFactory {
	private static final DummyKeyAccessFactory INSTANCE = new DummyKeyAccessFactory();

	private DummyKeyAccessFactory() {

	}

	/**
	 * Get a DummyKeyAccessFactory instance.
	 *
	 * @return A singleton instance.
	 */
	public static DummyKeyAccessFactory getInstance() {
		return INSTANCE;
	}

	@Override
	public KeyAccess getKeyAccess(final Channel channel) {
		Preconditions.checkNotNull(channel);
		return null;
	}

	@Override
	public boolean canHandleChannelClass(final Class<? extends Channel> clazz) {
		Preconditions.checkNotNull(clazz);
		return false;
	}
}
