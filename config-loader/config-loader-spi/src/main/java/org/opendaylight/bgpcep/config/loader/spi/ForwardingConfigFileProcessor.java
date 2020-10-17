/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.spi;

import com.google.common.collect.ForwardingObject;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * Implementation of {@link ConfigFileProcessor} which forwards request to a delegate.
 */
public abstract class ForwardingConfigFileProcessor extends ForwardingObject implements ConfigFileProcessor {
    @Override
    public SchemaPath getSchemaPath() {
        return delegate().getSchemaPath();
    }

    @Override
    public void loadConfiguration(final NormalizedNode<?, ?> dto) {
        delegate().loadConfiguration(dto);
    }

    @Override
    protected abstract @NonNull ConfigFileProcessor delegate();
}
