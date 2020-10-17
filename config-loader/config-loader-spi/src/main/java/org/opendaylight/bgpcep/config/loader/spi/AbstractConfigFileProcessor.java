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

import com.google.common.util.concurrent.FluentFuture;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic substrate for a typical ConfigFileProcessor. Aside from the processor itself, it exposes lifecycle hooks which
 * can be used either by the implementation itself or external users.
 */
public abstract class AbstractConfigFileProcessor implements ConfigFileProcessor, Registration {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractConfigFileProcessor.class);

    private final @NonNull ConfigLoader configLoader;
    private final @NonNull DOMDataBroker dataBroker;
    private final String name;

    private AbstractRegistration reg;

    protected AbstractConfigFileProcessor(final String name, final ConfigLoader configLoader,
            final DOMDataBroker dataBroker) {
        this.name = requireNonNull(name);
        this.configLoader = requireNonNull(configLoader);
        this.dataBroker = requireNonNull(dataBroker);
    }

    @Override
    public final void loadConfiguration(final NormalizedNode<?, ?> dto) {
        final FluentFuture<? extends CommitInfo> future = loadConfiguration(dataBroker, dto);
        try {
            future.get();
        } catch (final ExecutionException | InterruptedException e) {
            LOG.warn("Failed to create {} configuration", name, e);
        }
    }

    /**
     * Load specified DTO using provided {@link DOMDataBroker}.
     *
     * @param dataBroker data broker to use
     * @param dto normalizedNode
     * @return Transaction commit future
     */
    protected abstract @NonNull FluentFuture<? extends CommitInfo> loadConfiguration(@NonNull DOMDataBroker dataBroker,
        @NonNull NormalizedNode<?, ?> dto);

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
