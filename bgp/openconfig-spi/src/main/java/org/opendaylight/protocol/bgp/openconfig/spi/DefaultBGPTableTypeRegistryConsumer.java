/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.spi;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableBiMap;
import java.util.List;
import java.util.ServiceLoader;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.kohsuke.MetaInfServices;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yangtools.concepts.Immutable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;

@Beta
@Singleton
@Component(immediate = true, service = BGPTableTypeRegistryConsumer.class)
@MetaInfServices(value = BGPTableTypeRegistryConsumer.class)
public final class DefaultBGPTableTypeRegistryConsumer extends AbstractBGPTableTypeRegistryConsumer
        implements Immutable {
    private final @NonNull ImmutableBiMap<BgpTableType, AfiSafiType> tableTypes;
    private final @NonNull ImmutableBiMap<TablesKey, AfiSafiType> tableKeys;

    public DefaultBGPTableTypeRegistryConsumer() {
        this(ServiceLoader.load(BGPTableTypeRegistryProviderActivator.class));
    }

    @Inject
    public DefaultBGPTableTypeRegistryConsumer(final Iterable<BGPTableTypeRegistryProviderActivator> activators) {
        final var builder = new SimpleBGPTableTypeRegistryProvider();
        for (BGPTableTypeRegistryProviderActivator activator : activators) {
            activator.startBGPTableTypeRegistryProvider(builder);
        }
        tableTypes = ImmutableBiMap.copyOf(builder.tableTypes());
        tableKeys = ImmutableBiMap.copyOf(builder.tableKeys());
    }

    @Activate
    public DefaultBGPTableTypeRegistryConsumer(final @Reference(policyOption = ReferencePolicyOption.GREEDY)
            List<BGPTableTypeRegistryProviderActivator> activators) {
        this((Iterable<BGPTableTypeRegistryProviderActivator>) activators);
    }

    @Override
    ImmutableBiMap<BgpTableType, AfiSafiType> tableTypes() {
        return tableTypes;
    }

    @Override
    ImmutableBiMap<TablesKey, AfiSafiType> tableKeys() {
        return tableKeys;
    }
}
