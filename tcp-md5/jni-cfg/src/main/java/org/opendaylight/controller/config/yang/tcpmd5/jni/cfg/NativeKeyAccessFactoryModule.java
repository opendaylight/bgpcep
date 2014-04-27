/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.tcpmd5.jni.cfg;

import java.nio.channels.Channel;

import org.opendaylight.bgpcep.tcpmd5.KeyAccess;
import org.opendaylight.bgpcep.tcpmd5.KeyAccessFactory;
import org.opendaylight.bgpcep.tcpmd5.jni.NativeKeyAccessFactory;
import org.opendaylight.bgpcep.tcpmd5.jni.NativeSupportUnavailableException;

/**
 * Service representing a way for accessing key informtion.
 */
public class NativeKeyAccessFactoryModule extends org.opendaylight.controller.config.yang.tcpmd5.jni.cfg.AbstractNativeKeyAccessFactoryModule {
	private KeyAccessFactory kaf;

	public NativeKeyAccessFactoryModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
		super(identifier, dependencyResolver);
	}

	public NativeKeyAccessFactoryModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.tcpmd5.jni.cfg.NativeKeyAccessFactoryModule oldModule, final java.lang.AutoCloseable oldInstance) {
		super(identifier, dependencyResolver, oldModule, oldInstance);
	}

	@Override
	public void customValidation() {
		try {
			kaf = NativeKeyAccessFactory.getInstance();
		} catch (NativeSupportUnavailableException e) {
			throw new UnsupportedOperationException("Native support is not available", e);
		}
	}

	@Override
	public java.lang.AutoCloseable createInstance() {
		final KeyAccessFactory kaf = this.kaf;

		final class CloseableNativeKeyAccessFactory implements AutoCloseable, KeyAccessFactory {
			@Override
			public KeyAccess getKeyAccess(final Channel channel) {
				return kaf.getKeyAccess(channel);
			}

			@Override
			public boolean canHandleChannelClass(final Class<? extends Channel> clazz) {
				return kaf.canHandleChannelClass(clazz);
			}

			@Override
			public void close() {
				// Nothing to do
			}
		}

		return new CloseableNativeKeyAccessFactory();
	}
}
