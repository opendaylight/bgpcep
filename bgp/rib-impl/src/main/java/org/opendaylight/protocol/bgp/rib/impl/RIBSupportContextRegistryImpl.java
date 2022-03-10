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
import org.opendaylight.protocol.bgp.rib.impl.spi.CodecsRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

final class RIBSupportContextRegistryImpl implements RIBSupportContextRegistry {

    private final RIBExtensionConsumerContext extensionContext;
    private final CodecsRegistry codecs;
    private final LoadingCache<RIBSupport<?, ?>, RIBSupportContextImpl> contexts = CacheBuilder.newBuilder()
            .build(new CacheLoader<RIBSupport<?, ?>, RIBSupportContextImpl>() {
                @Override
                public RIBSupportContextImpl load(final RIBSupport<?, ?> key) {
                    return createRIBSupportContext(key);
                }
            });

    private RIBSupportContextRegistryImpl(final RIBExtensionConsumerContext extensions, final CodecsRegistry codecs) {
        this.extensionContext = requireNonNull(extensions);
        this.codecs = requireNonNull(codecs);
    }

    static RIBSupportContextRegistryImpl create(final RIBExtensionConsumerContext extensions,
            final CodecsRegistry codecs) {
        return new RIBSupportContextRegistryImpl(extensions, codecs);
    }

    private RIBSupportContextImpl createRIBSupportContext(final RIBSupport<?, ?> support) {
        return new RIBSupportContextImpl(support, this.codecs);
    }

    @Override
    public <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>>
            RIBSupport<C, S> getRIBSupport(final TablesKey key) {
        final RIBSupportContext ribSupport = getRIBSupportContext(key);
        return ribSupport == null ? null : ribSupport.getRibSupport();
    }

    @Override
    public RIBSupportContext getRIBSupportContext(final TablesKey key) {
        final RIBSupport<?, ?> ribSupport = this.extensionContext.getRIBSupport(key);
        return ribSupport == null ? null : this.contexts.getUnchecked(ribSupport);
    }

    @Override
    public RIBSupportContext getRIBSupportContext(final NodeIdentifierWithPredicates key) {
        final RIBSupport<?, ?> ribSupport = this.extensionContext.getRIBSupport(key);
        return ribSupport == null ? null : this.contexts.getUnchecked(ribSupport);
    }
}
