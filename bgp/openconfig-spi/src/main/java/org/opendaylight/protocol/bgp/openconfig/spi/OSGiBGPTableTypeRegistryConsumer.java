/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.spi;

import static com.google.common.base.Verify.verifyNotNull;

import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = BGPTableTypeRegistryConsumer.class)
// FIXME: merge this with DefaultBGPTableTypeRegistryConsumer when we have OSGi R7
public final class OSGiBGPTableTypeRegistryConsumer implements BGPTableTypeRegistryConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(OSGiBGPTableTypeRegistryConsumer.class);

    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    List<BGPTableTypeRegistryProviderActivator> extensionActivators;

    private DefaultBGPTableTypeRegistryConsumer delegate;

    @Override
    public BgpTableType getTableType(final Class<? extends AfiSafiType> afiSafiType) {
        return delegate().getTableType(afiSafiType);
    }

    @Override
    public TablesKey getTableKey(final Class<? extends AfiSafiType> afiSafiType) {
        return delegate().getTableKey(afiSafiType);
    }

    @Override
    public Class<? extends AfiSafiType> getAfiSafiType(final BgpTableType bgpTableType) {
        return delegate().getAfiSafiType(bgpTableType);
    }

    @Override
    public Class<? extends AfiSafiType> getAfiSafiType(final TablesKey tablesKey) {
        return delegate().getAfiSafiType(tablesKey);
    }

    @Activate
    void activate() {
        LOG.info("BGPTableTypeRegistryProviderActivator starting with {} extensions", extensionActivators.size());
        delegate = new DefaultBGPTableTypeRegistryConsumer(extensionActivators);
        LOG.info("BGPTableTypeRegistryProvider started");
    }

    @Deactivate
    void deactivate() {
        delegate = null;
        LOG.info("BGPTableTypeRegistryProvider stopped");
    }

    @NonNull DefaultBGPTableTypeRegistryConsumer delegate() {
        return verifyNotNull(delegate);
    }
}
