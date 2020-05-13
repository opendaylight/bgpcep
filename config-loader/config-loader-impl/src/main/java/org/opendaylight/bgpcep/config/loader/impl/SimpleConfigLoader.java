/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.impl;

import static java.util.Objects.requireNonNull;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.binding.dom.codec.spi.BindingDOMCodecServices;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

@Singleton
public final class SimpleConfigLoader extends AbstractConfigLoader {
    private final @NonNull BindingNormalizedNodeSerializer serializer;
    private final @NonNull EffectiveModelContext modelContext;

    @Inject
    public SimpleConfigLoader(final FileWatcher fileWatcher, final BindingDOMCodecServices codec) {
        super(fileWatcher);
        this.serializer = requireNonNull(codec);
        this.modelContext = codec.getRuntimeContext().getEffectiveModelContext();
    }

    @Override
    public BindingNormalizedNodeSerializer getBindingNormalizedNodeSerializer() {
        return serializer;
    }

    @PostConstruct
    public void postConstruct() {
        start();
    }

    @PreDestroy
    public void preDestroy() {
        stop();
    }

    @Override
    EffectiveModelContext modelContext() {
        return modelContext;
    }
}
