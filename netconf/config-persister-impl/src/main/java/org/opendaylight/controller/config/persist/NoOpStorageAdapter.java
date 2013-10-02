/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.persist;

import java.io.IOException;
import java.util.Set;

import org.opendaylight.controller.config.persist.api.storage.StorageAdapter;
import org.opendaylight.netconf.util.osgi.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.google.common.base.Optional;

public class NoOpStorageAdapter implements StorageAdapter {
	private static final Logger logger = LoggerFactory.getLogger(NoOpStorageAdapter.class);

	@Override
	public void setProperties(ConfigProvider configProvider) {
		logger.debug("setProperties called with {}", configProvider);
	}

	@Override
	public void persistConfig(Element configSnapshot, Set<String> capabilities) throws IOException {
		// TODO add caps
		logger.debug("persistConfig called with {}", configSnapshot);
	}

	@Override
	public Optional<PersistedConfig> loadLastConfig() throws IOException {
		logger.debug("loadLastConfig called");
		return Optional.absent();
	}

	@Override
	public void close() throws IOException {
		logger.debug("close called");
	}
}
