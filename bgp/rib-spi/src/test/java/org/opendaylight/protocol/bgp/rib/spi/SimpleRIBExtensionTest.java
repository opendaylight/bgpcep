/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.opendaylight.protocol.bgp.rib.spi.RIBQNames.AFI_QNAME;
import static org.opendaylight.protocol.bgp.rib.spi.RIBQNames.SAFI_QNAME;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.mdsal.binding.dom.adapter.AdapterContext;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTestCustomizer;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

public class SimpleRIBExtensionTest extends AbstractConcurrentDataBrokerTest {
    private AdapterContext adapter;

    @Override
    protected final AbstractDataBrokerTestCustomizer createDataBrokerTestCustomizer() {
        final AbstractDataBrokerTestCustomizer customizer = super.createDataBrokerTestCustomizer();
        adapter = customizer.getAdapterContext();
        return customizer;
    }

    @Test
    public void testExtensionProvider() {
        final var codec = adapter.currentSerializer();
        var ctx = new DefaultRIBExtensionConsumerContext(codec);
        assertNull(ctx.getRIBSupport(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE));

        ctx = new DefaultRIBExtensionConsumerContext(codec, new TestActivator());
        assertNotNull(ctx.getRIBSupport(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE));
    }

    private static final class TestActivator implements RIBExtensionProviderActivator {
        @Override
        public List<Registration> startRIBExtensionProvider(final RIBExtensionProviderContext context,
                final BindingNormalizedNodeSerializer mappingService) {
            final var support = mock(RIBSupport.class);
            doReturn(Route.class).when(support).routesListClass();
            doReturn(DataObject.class).when(support).routesContainerClass();
            doReturn(DataObject.class).when(support).routesCaseClass();
            doReturn(new TablesKey(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE)).when(support)
                .getTablesKey();
            doReturn(NodeIdentifierWithPredicates.of(Tables.QNAME,
                Map.of(AFI_QNAME, Ipv4AddressFamily.QNAME, SAFI_QNAME, UnicastSubsequentAddressFamily.QNAME)))
                .when(support).tablesKey();
            return List.of(context.registerRIBSupport(support));
        }
    }
}
