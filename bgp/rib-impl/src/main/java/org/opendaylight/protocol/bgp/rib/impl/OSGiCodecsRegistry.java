/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTree;
import org.opendaylight.protocol.bgp.rib.impl.spi.Codecs;
import org.opendaylight.protocol.bgp.rib.impl.spi.CodecsRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
@Component(immediate = true)
public final class OSGiCodecsRegistry implements CodecsRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(OSGiCodecsRegistry.class);

    private final ConcurrentMap<RIBSupport<?, ?, ?, ?>, Codecs> contexts = new ConcurrentHashMap<>();
    private volatile BindingCodecTree codecTree;

    @Override
    public Codecs getCodecs(final RIBSupport<?, ?, ?, ?> ribSupport) {
        return contexts.computeIfAbsent(ribSupport, this::createCodecs);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC)
    void bindCodecTree(final BindingCodecTree newCodecTree) {
        this.codecTree = requireNonNull(newCodecTree);
    }

    void unbindCodecTree() {
        this.codecTree = null;
    }

    void updatedCodecTree(final BindingCodecTree newCodecTree) {
        this.codecTree = requireNonNull(newCodecTree);
        contexts.values().forEach(codecs -> codecs.onCodecTreeUpdated(newCodecTree));
    }

    @Activate
    @SuppressWarnings("static-method")
    void activate() {
        LOG.info("BGP codec registry started");
    }

    @Deactivate
    void deactivate() {
        contexts.clear();
        LOG.info("BGP codec registry stopped");
    }

    private Codecs createCodecs(final RIBSupport<?, ?, ?, ?> key) {
        final Codecs codecs = new CodecsImpl(key);
        codecs.onCodecTreeUpdated(codecTree);
        return codecs;
    }
}
