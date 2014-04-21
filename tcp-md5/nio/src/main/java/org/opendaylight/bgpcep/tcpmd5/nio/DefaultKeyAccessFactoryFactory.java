/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.nio;

import org.opendaylight.bgpcep.tcpmd5.DummyKeyAccessFactory;
import org.opendaylight.bgpcep.tcpmd5.KeyAccessFactory;
import org.opendaylight.bgpcep.tcpmd5.jni.NativeKeyAccessFactory;
import org.opendaylight.bgpcep.tcpmd5.jni.NativeSupportUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal utility class for selecting the best available KeyAccessFactory.
 * We prefer NativeKeyAccessFactory, but fall back to DummyKeyAccessFactory
 * if that is not available.
 */
final class DefaultKeyAccessFactoryFactory {
	private static final Logger LOG = LoggerFactory.getLogger(DefaultKeyAccessFactoryFactory.class);
	private static final KeyAccessFactory FACTORY;

	static {
		KeyAccessFactory factory;

		try {
			factory = NativeKeyAccessFactory.getInstance();
		} catch (NativeSupportUnavailableException e) {
			LOG.debug("Native key access not available, using no-op fallback", e);
			factory = DummyKeyAccessFactory.getInstance();
		}

		FACTORY = factory;
	}

	private DefaultKeyAccessFactoryFactory() {
		throw new UnsupportedOperationException("Utility class should never be instantiated");
	}

	public static KeyAccessFactory getKeyAccessFactory() {
		return FACTORY;
	}
}
