/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTree;
import org.opendaylight.protocol.bgp.rib.impl.spi.Codecs;
import org.opendaylight.protocol.bgp.rib.impl.spi.CodecsRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;

@Singleton
public final class ConstantCodecsRegistry implements CodecsRegistry {
    private final ConcurrentMap<RIBSupport<?, ?, ?, ?>, Codecs> contexts = new ConcurrentHashMap<>();
    private final BindingCodecTree codecTree;

    @Inject
    public ConstantCodecsRegistry(final BindingCodecTree codecTree) {
        this.codecTree = requireNonNull(codecTree);
    }

    @Override
    public Codecs getCodecs(final RIBSupport<?, ?, ?, ?> ribSupport) {
        return contexts.computeIfAbsent(ribSupport, this::createCodecs);
    }

    private Codecs createCodecs(final RIBSupport<?, ?, ?, ?> key) {
        final Codecs codecs = new CodecsImpl(key);
        codecs.onCodecTreeUpdated(codecTree);
        return codecs;
    }
}
