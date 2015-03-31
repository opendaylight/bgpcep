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
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTree;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTreeFactory;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

class RIBSupportContextRegistryImpl implements RIBSupportContextRegistry {

    private final LoadingCache<RIBSupport, RIBSupportContextImpl> contexts = CacheBuilder.newBuilder()
            .build(new CacheLoader<RIBSupport, RIBSupportContextImpl>(){

                @Override
                public RIBSupportContextImpl load(final RIBSupport key) {
                    return createContext(key);
                };
            });

    private final RIBExtensionConsumerContext extensionContext;
    private final BindingCodecTreeFactory codecFactory;
    private final GeneratedClassLoadingStrategy classContext;
    private volatile BindingCodecTree latestCodecTree;

    private RIBSupportContextRegistryImpl(final RIBExtensionConsumerContext extensions, final BindingCodecTreeFactory codecFactory,
            final GeneratedClassLoadingStrategy strategy) {
        this.extensionContext = Preconditions.checkNotNull(extensions);
        this.codecFactory = Preconditions.checkNotNull(codecFactory);
        this.classContext = Preconditions.checkNotNull(strategy);
    }

    static RIBSupportContextRegistryImpl create(final RIBExtensionConsumerContext extensions,
            final BindingCodecTreeFactory codecFactory, final GeneratedClassLoadingStrategy classStrategy) {
        return new RIBSupportContextRegistryImpl(extensions, codecFactory, classStrategy);
    }

    @Override
    public RIBSupportContext getRIBSupportContext(final TablesKey key) {
        final RIBSupport ribSupport = extensionContext.getRIBSupport(key);
        if(ribSupport != null) {
            return contexts.getUnchecked(ribSupport);
        }
        return null;
    }

    @Override
    public RIBSupportContext getRIBSupportContext(final NodeIdentifierWithPredicates key) {
        final RIBSupport ribSupport = extensionContext.getRIBSupport(key);
        if(ribSupport != null) {
            return contexts.getUnchecked(ribSupport);
        }
        return null;
    }

    private RIBSupportContextImpl createContext(final RIBSupport ribSupport) {
        final RIBSupportContextImpl ribContext = new RIBSupportContextImpl(ribSupport);
        if(latestCodecTree != null) {
            // FIXME: Do we need to recalculate latestCodecTree? E.g. new rib support was added
            // after bgp was started.
            ribContext.onCodecTreeUpdated(latestCodecTree);
        }
        return ribContext;
    }

    void onSchemaContextUpdated(final SchemaContext context) {
        final BindingRuntimeContext runtimeContext = BindingRuntimeContext.create(classContext, context);
        latestCodecTree  = codecFactory.create(runtimeContext);
        for(final RIBSupportContextImpl rib : contexts.asMap().values()) {
            rib.onCodecTreeUpdated(latestCodecTree);
        }
    }

}
