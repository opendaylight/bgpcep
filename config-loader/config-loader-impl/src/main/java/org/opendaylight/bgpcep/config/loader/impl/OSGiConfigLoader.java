/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.impl;

import com.google.common.annotations.Beta;
import org.opendaylight.bgpcep.config.loader.spi.ConfigFileProcessor;
import org.opendaylight.bgpcep.config.loader.spi.ConfigLoader;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.binding.dom.codec.spi.BindingDOMCodecServices;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
@Component(immediate = true)
public final class OSGiConfigLoader implements ConfigLoader {
    private static final Logger LOG = LoggerFactory.getLogger(OSGiConfigLoader.class);

    @Reference
    BindingDOMCodecServices codecServices;

    private ConfigLoaderImpl delegate;

    @Override
    public AbstractRegistration registerConfigFile(final ConfigFileProcessor config) {
        return delegate.registerConfigFile(config);
    }

    @Override
    public BindingNormalizedNodeSerializer getBindingNormalizedNodeSerializer() {
        return delegate.getBindingNormalizedNodeSerializer();
    }

    @Activate
    void activate() {
        delegate = new ConfigLoaderImpl(codecServices.getRuntimeContext().getEffectiveModelContext(), codecServices,
            fileWatcher);
        LOG.info("BGPCEP Configuration Loader started");
    }

    @Deactivate
    void deactivate() {
        delegate = null;
        LOG.info("BGPCEP Configuration Loader stopped");
    }
}
