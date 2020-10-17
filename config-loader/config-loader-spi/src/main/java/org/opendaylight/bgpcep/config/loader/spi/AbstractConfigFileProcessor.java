/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.spi;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic substrate for a typical ConfigFileProcessor. Aside from the processor itself, it exposes lifecycle hooks which
 * can be used either by the implementation itself or external users.
 */
public abstract class AbstractConfigFileProcessor implements ConfigFileProcessor, Registration {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractConfigFileProcessor.class);

    private final @NonNull ConfigLoader configLoader;
    private final String name;

    private AbstractRegistration reg;

    protected AbstractConfigFileProcessor(final String name, final ConfigLoader configLoader) {
        this.name = requireNonNull(name);
        this.configLoader = requireNonNull(configLoader);
    }

    /**
     * Return the configuration loader which is associated with this instance.
     *
     * @return A ConfigLoader instance.
     */
    protected final @NonNull ConfigLoader configLoader() {
        return configLoader;
    }

    /**
     * Start this processor by registering it with the config loader.
     *
     * @throws IllegalStateException if the processor has already been started
     */
    protected final void start() {
        checkState(reg == null, "%s Config Loader already started", name);
        reg = configLoader.registerConfigFile(this);
        LOG.info("{} Config Loader registered", name);
    }

    /**
     * Stop this processor by unregistering it from the loader. If this processor is not started, this method does
     * nothing.
     */
    protected final void stop() {
        if (reg != null) {
            reg.close();
            reg = null;
            LOG.info("{} Config Loader unregistered", name);
        }
    }
}
