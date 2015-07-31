/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.opendaylight.protocol.bgp.rib.impl.spi.Codecs;
import org.opendaylight.protocol.bgp.rib.impl.spi.CodecsRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTree;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTreeFactory;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class CodecsRegistryImpl implements CodecsRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(CodecsRegistryImpl.class);

    private final LoadingCache<RIBSupport, Codecs> contexts = CacheBuilder.newBuilder()
        .build(new CacheLoader<RIBSupport, Codecs>(){

            @Override
            public Codecs load(final RIBSupport key) {
                return createContext(key);
            }
        });
    private final BindingCodecTreeFactory codecFactory;
    private final GeneratedClassLoadingStrategy classContext;
    private volatile BindingCodecTree latestCodecTree;

    private CodecsRegistryImpl(final BindingCodecTreeFactory codecFactory, final GeneratedClassLoadingStrategy strategy) {
        this.codecFactory = Preconditions.checkNotNull(codecFactory);
        this.classContext = Preconditions.checkNotNull(strategy);
    }

    static CodecsRegistryImpl create(final BindingCodecTreeFactory codecFactory, final GeneratedClassLoadingStrategy classStrategy) {
        return new CodecsRegistryImpl(codecFactory, classStrategy);
    }

    private Codecs createContext(final RIBSupport ribSupport) {
        final Codecs codecs = new CodecsImpl(ribSupport);
        if (this.latestCodecTree != null) {
            // FIXME: Do we need to recalculate latestCodecTree? E.g. new rib support was added
            // after bgp was started.
            codecs.onCodecTreeUpdated(this.latestCodecTree);
        }
        return codecs;
    }

    void onSchemaContextUpdated(final SchemaContext context) {
        final BindingRuntimeContext runtimeContext = BindingRuntimeContext.create(this.classContext, context);
        this.latestCodecTree  = this.codecFactory.create(runtimeContext);
        for (final Codecs codecs : this.contexts.asMap().values()) {
            try {
                codecs.onCodecTreeUpdated(this.latestCodecTree);
            } catch (final Exception e) {
                LOG.error("Codec creation threw {}", e.getMessage(), e);
            }
        }
    }

    @Override
    public Codecs getCodecs(final RIBSupport ribSupport) {
        return this.contexts.getUnchecked(ribSupport);
    }
}
