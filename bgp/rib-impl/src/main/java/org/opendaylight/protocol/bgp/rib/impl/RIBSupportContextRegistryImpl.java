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
import org.opendaylight.protocol.bgp.rib.impl.spi.CodecsRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

final class RIBSupportContextRegistryImpl implements RIBSupportContextRegistry {

    private final LoadingCache<RIBSupport, RIBSupportContextImpl> contexts = CacheBuilder.newBuilder()
            .build(new CacheLoader<RIBSupport, RIBSupportContextImpl>(){
                @Override
                public RIBSupportContextImpl load(final RIBSupport key) {
                    return createRIBSupportContext(key);
                }
            });

    private final RIBExtensionConsumerContext extensionContext;
    private final CodecsRegistry codecs;

    private RIBSupportContextRegistryImpl(final RIBExtensionConsumerContext extensions, final CodecsRegistry codecs) {
        this.extensionContext = requireNonNull(extensions);
        this.codecs = requireNonNull(codecs);
    }

    static RIBSupportContextRegistryImpl create(final RIBExtensionConsumerContext extensions, final CodecsRegistry codecs) {
        return new RIBSupportContextRegistryImpl(extensions, codecs);
    }

    private RIBSupportContextImpl createRIBSupportContext(final RIBSupport support) {
        return new RIBSupportContextImpl(support, this.codecs);
    }

    @Override
    public RIBSupportContext getRIBSupportContext(final TablesKey key) {
        final RIBSupport ribSupport = this.extensionContext.getRIBSupport(key);
        if(ribSupport != null) {
            return this.contexts.getUnchecked(ribSupport);
        }
        return null;
    }

    @Override
    public RIBSupportContext getRIBSupportContext(final NodeIdentifierWithPredicates key) {
        final RIBSupport ribSupport = this.extensionContext.getRIBSupport(key);
        if(ribSupport != null) {
            return this.contexts.getUnchecked(ribSupport);
        }
        return null;
    }
}
