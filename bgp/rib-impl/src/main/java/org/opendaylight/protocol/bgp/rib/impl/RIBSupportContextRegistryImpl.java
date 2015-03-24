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
import java.util.ArrayList;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTree;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTreeFactory;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class RIBSupportContextRegistryImpl implements RIBSupportContextRegistry {

    private final LoadingCache<TablesKey, RIBSupportContextImpl> contexts = CacheBuilder.newBuilder()
            .build(new CacheLoader<TablesKey, RIBSupportContextImpl>(){

                @Override
                public RIBSupportContextImpl load(TablesKey key) {
                    return createContext(key);
                };
            });

    private final RIBExtensionConsumerContext extensionContext;
    private final BindingCodecTreeFactory codecFactory;
    private BindingCodecTree latestCodecTree;

    private RIBSupportContextRegistryImpl(RIBExtensionConsumerContext extensions, BindingCodecTreeFactory codecFactory) {

        this.extensionContext = Preconditions.checkNotNull(extensions);
        this.codecFactory = Preconditions.checkNotNull(codecFactory);
    }

    static RIBSupportContextRegistryImpl create(RIBExtensionConsumerContext extensions,
            BindingCodecTreeFactory codecFactory) {
        return new RIBSupportContextRegistryImpl(extensions, codecFactory);
    }

    /* (non-Javadoc)
     * @see org.opendaylight.protocol.bgp.rib.impl.RIBSupportContextRegistry#getRIBSupportContext(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey)
     */
    @Override
    public RIBSupportContext getRIBSupportContext(TablesKey key) {
        return contexts.getUnchecked(key);
    }

    private RIBSupportContextImpl createContext(TablesKey key) {
        RIBSupport ribSupport = extensionContext.getRIBSupport(key);
        if(ribSupport == null) {
            return null;
        }
        RIBSupportContextImpl ribContext = new RIBSupportContextImpl(ribSupport);
        if(latestCodecTree != null) {
            ribContext.onCodecTreeUpdated(latestCodecTree);
        }
        return ribContext;
    }

    void onSchemaContextUpdated(SchemaContext context) {
        updateCodecTree(context,getKnownClasses());
    }

    private void updateCodecTree(SchemaContext context,Class<?>[] classes) {
        BindingCodecTree codecTree = codecFactory.create(context, classes);
        for(RIBSupportContextImpl rib : contexts.asMap().values()) {
            rib.onCodecTreeUpdated(codecTree);
        }
        latestCodecTree = codecTree;
    }

    private Class<?>[] getKnownClasses() {
        ArrayList<Class<?>> ret = new ArrayList<>();
        for(RIBSupportContextImpl rib : contexts.asMap().values()) {
            RIBSupport tableSupport = rib.getRibSupport();
            ret.add(tableSupport.routesCaseClass());
            ret.add(tableSupport.routesContainerClass());
            ret.add(tableSupport.routesListClass());
        }
        return ret.toArray(new Class[]{});
    }

}
