/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.opendaylight.binding.runtime.api.BindingRuntimeContext;
import org.opendaylight.binding.runtime.api.ClassLoadingStrategy;
import org.opendaylight.binding.runtime.spi.GeneratedClassLoadingStrategy;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTree;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTreeFactory;
import org.opendaylight.protocol.bgp.rib.impl.spi.Codecs;
import org.opendaylight.protocol.bgp.rib.impl.spi.CodecsRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CodecsRegistryImpl implements CodecsRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(CodecsRegistryImpl.class);

    private final LoadingCache<RIBSupport<?, ?, ?, ?>, Codecs> contexts = CacheBuilder.newBuilder()
        .build(new CacheLoader<RIBSupport<?, ?, ?, ?>, Codecs>() {
            @Override
            public Codecs load(final RIBSupport<?, ?, ?, ?> key) {
                return createContext(key);
            }
        });
    private final BindingCodecTreeFactory codecFactory;
    private final ClassLoadingStrategy classContext;
    private volatile BindingCodecTree latestCodecTree;

    private CodecsRegistryImpl(final BindingCodecTreeFactory codecFactory,
            final GeneratedClassLoadingStrategy strategy) {
        this.codecFactory = requireNonNull(codecFactory);
        this.classContext = requireNonNull(strategy);
    }

    public static CodecsRegistryImpl create(final BindingCodecTreeFactory codecFactory,
            final GeneratedClassLoadingStrategy classStrategy) {
        return new CodecsRegistryImpl(codecFactory, classStrategy);
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private Codecs createContext(final RIBSupport<?, ?, ?, ?> ribSupport) {
        final Codecs codecs = new CodecsImpl(ribSupport);
        if (this.latestCodecTree != null) {
            // FIXME: Do we need to recalculate latestCodecTree? E.g. new rib support was added
            // after bgp was started.
            codecs.onCodecTreeUpdated(this.latestCodecTree);
        }
        return codecs;
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    void onSchemaContextUpdated(final SchemaContext context) {
        final BindingRuntimeContext runtimeContext = BindingRuntimeContext.create(this.classContext, context);
        this.latestCodecTree  = this.codecFactory.create(runtimeContext);
        for (final Codecs codecs : this.contexts.asMap().values()) {
            try {
                codecs.onCodecTreeUpdated(this.latestCodecTree);
            } catch (final Exception e) {
                LOG.error("Failed to propagate SchemaContext to codec {}", codecs, e);
            }
        }
    }

    @Override
    public Codecs getCodecs(final RIBSupport<?, ?, ?, ?> ribSupport) {
        return this.contexts.getUnchecked(ribSupport);
    }
}
