/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.collect.Maps;
import java.util.AbstractMap;
import java.util.Map;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv4.routes._case.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv4.routes._case.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv4.routes._case.ipv4.routes.Ipv4RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv4.routes._case.ipv4.routes.Ipv4RouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.route.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class AbstractAdjRIBsTest {

    private static final AsNumber TEST_AS_NUMBER = new AsNumber(35L);

    private static final Ipv4Prefix IPV4_PREFIX1 = new Ipv4Prefix("test-prefix1");

    private static final Ipv4Prefix IPV4_PREFIX2 = new Ipv4Prefix("test-prefix2");

    private static final InstanceIdentifier<Rib> RIB_IID = InstanceIdentifier.builder(BgpRib.class).child(Rib.class, new RibKey(new RibId("test-rib"))).toInstance();

    private static final KeyedInstanceIdentifier<Tables, TablesKey> TABLES_IID = RIB_IID.child(LocRib.class).child(Tables.class, new TablesKey(Ipv4AddressFamily.class,
            UnicastSubsequentAddressFamily.class));

    @Mock
    private AdjRIBsTransaction ribsTx;
    @Mock
    private Peer peer;

    private Map<InstanceIdentifier<?>, Map.Entry<DataObject, Boolean>> store = Maps.newHashMap();

    private final BGPObjectComparator bgpComparator = new BGPObjectComparator(TEST_AS_NUMBER);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.doReturn("").when(this.peer).toString();
        Mockito.doReturn(this.bgpComparator).when(this.ribsTx).comparator();
        Mockito.doAnswer(new Answer<Void>() {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                final InstanceIdentifier<Route> ii = (InstanceIdentifier<Route>) args[2];
                final Route data = (Route) args[4];
                AbstractAdjRIBsTest.this.store.put(ii, new AbstractMap.SimpleEntry(data, false));
                return null;
            }

        }).when(this.ribsTx).advertise(Mockito.<RouteEncoder>any(), Mockito.any(), Mockito.<InstanceIdentifier<Route>>any(), Mockito.<Peer>any(), Mockito.any(Route.class));

        Mockito.doAnswer(new Answer<Void>() {
            @SuppressWarnings("unchecked")
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                final InstanceIdentifier<Route> ii = (InstanceIdentifier<Route>) args[2];
                AbstractAdjRIBsTest.this.store.remove(ii);
                return null;
            }

        }).when(this.ribsTx).withdraw(Mockito.<RouteEncoder>any(), Mockito.any(), Mockito.<InstanceIdentifier<Route>>any());

        Mockito.doAnswer(new Answer<Void>() {
            @SuppressWarnings("unchecked")
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                final InstanceIdentifier<Tables> basePath = (InstanceIdentifier<Tables>) args[0];
                final Boolean uptodate = (Boolean) args[1];
                @SuppressWarnings("rawtypes")
                Map.Entry<DataObject, Boolean> entry = new AbstractMap.SimpleEntry(null, uptodate);
                AbstractAdjRIBsTest.this.store.put(basePath, entry);
                return null;
            }
        }).when(this.ribsTx).setUptodate(Matchers.<InstanceIdentifier<Tables>>any(), Mockito.anyBoolean());
    }

    @Test
    public void testAdjRibs() {
        final TestAdjRIBs adjsRib = new TestAdjRIBs(TABLES_IID);
        adjsRib.add(this.ribsTx, this.peer, IPV4_PREFIX1, new TestAdjRIBs.TestIpv4RIBEntryData(this.peer, new PathAttributesBuilder().build()));
        Mockito.verify(this.ribsTx, Mockito.times(1)).advertise(Mockito.<RouteEncoder>any(), Mockito.any(), Mockito.<InstanceIdentifier<Route>>any(), Mockito.<Peer>any(), Mockito.any(Route.class));
        Mockito.verify(this.ribsTx, Mockito.times(1)).setUptodate(Matchers.<InstanceIdentifier<Tables>>any(), Mockito.anyBoolean());
        Assert.assertEquals(2, this.store.size());
        Assert.assertFalse(this.store.get(TABLES_IID).getValue());

        adjsRib.markUptodate(this.ribsTx, this.peer);
        Mockito.verify(this.ribsTx, Mockito.times(2)).setUptodate(Matchers.<InstanceIdentifier<Tables>>any(), Mockito.anyBoolean());
        Assert.assertEquals(2, this.store.size());
        Assert.assertTrue(this.store.get(TABLES_IID).getValue());

        adjsRib.remove(this.ribsTx, this.peer, IPV4_PREFIX1);
        Mockito.verify(this.ribsTx, Mockito.times(1)).withdraw(Mockito.<RouteEncoder>any(), Mockito.any(), Mockito.<InstanceIdentifier<Route>>any());
        Assert.assertEquals(1, this.store.size());

        adjsRib.add(this.ribsTx, this.peer, IPV4_PREFIX1, new TestAdjRIBs.TestIpv4RIBEntryData(this.peer, new PathAttributesBuilder().build()));
        adjsRib.add(this.ribsTx, this.peer, IPV4_PREFIX2, new TestAdjRIBs.TestIpv4RIBEntryData(this.peer, new PathAttributesBuilder().build()));
        Mockito.verify(this.ribsTx, Mockito.times(3)).advertise(Mockito.<RouteEncoder>any(), Mockito.any(), Mockito.<InstanceIdentifier<Route>>any(), Mockito.<Peer>any(), Mockito.any(Route.class));

        adjsRib.addAllEntries(this.ribsTx);
        Mockito.verify(this.ribsTx, Mockito.times(5)).advertise(Mockito.<RouteEncoder>any(), Mockito.any(), Mockito.<InstanceIdentifier<Route>>any(), Mockito.<Peer>any(), Mockito.any(Route.class));
        Assert.assertEquals(3, this.store.size());

        adjsRib.clear(this.ribsTx, this.peer);
        Mockito.verify(this.ribsTx, Mockito.times(3)).setUptodate(Matchers.<InstanceIdentifier<Tables>>any(), Mockito.anyBoolean());
        Assert.assertEquals(1, this.store.size());
    }

    @Test
    public void testEndOfRib() {
        final TestAdjRIBs adjsRib = new TestAdjRIBs(TABLES_IID);
        final Update endOfRib = adjsRib.endOfRib();
        final PathAttributes1 attr1 = endOfRib.getPathAttributes().getAugmentation(PathAttributes1.class);
        Assert.assertNotNull(attr1);
        Assert.assertEquals(Ipv4AddressFamily.class, attr1.getMpReachNlri().getAfi());
        Assert.assertEquals(UnicastSubsequentAddressFamily.class, attr1.getMpReachNlri().getSafi());
    }

    @Test
    public void testUpdateMsgFor() {
        final TestAdjRIBs adjsRib = new TestAdjRIBs(TABLES_IID);
        final Update update1 = adjsRib.updateMessageFor(IPV4_PREFIX1, new Ipv4RouteBuilder().setAttributes(new AttributesBuilder().build()).build());
        final PathAttributes1 attr1 = update1.getPathAttributes().getAugmentation(PathAttributes1.class);
        Assert.assertNotNull(attr1);
        Assert.assertEquals(Ipv4AddressFamily.class, attr1.getMpReachNlri().getAfi());
        Assert.assertEquals(UnicastSubsequentAddressFamily.class, attr1.getMpReachNlri().getSafi());

        final Update update2 = adjsRib.updateMessageFor(IPV4_PREFIX2, null);
        final PathAttributes2 attr2 = update2.getPathAttributes().getAugmentation(PathAttributes2.class);
        Assert.assertNotNull(attr2);
        Assert.assertEquals(Ipv4AddressFamily.class, attr2.getMpUnreachNlri().getAfi());
        Assert.assertEquals(UnicastSubsequentAddressFamily.class, attr2.getMpUnreachNlri().getSafi());
    }

    private static final class TestAdjRIBs extends AbstractAdjRIBs<Ipv4Prefix, Ipv4Route, Ipv4RouteKey> {

        private static final class TestIpv4RIBEntryData extends RIBEntryData<Ipv4Prefix, Ipv4Route, Ipv4RouteKey> {

            private final PathAttributes attributes;

            protected TestIpv4RIBEntryData(final Peer peer, final PathAttributes attributes) {
                super(peer, attributes);
                this.attributes = attributes;
            }

            @Override
            protected Ipv4Route getDataObject(final Ipv4Prefix key, final Ipv4RouteKey id) {
                return new Ipv4RouteBuilder().setKey(id).setAttributes(new AttributesBuilder(this.attributes).build()).build();
            }

        }

        protected TestAdjRIBs(KeyedInstanceIdentifier<Tables, TablesKey> basePath) {
            super(basePath);
        }

        @Override
        public void addRoutes(AdjRIBsTransaction trans, Peer peer, MpReachNlri nlri, PathAttributes attributes) {
            return;
        }

        @Override
        public void removeRoutes(AdjRIBsTransaction trans, Peer peer, MpUnreachNlri nlri) {
            return;
        }

        @Override
        public void addAdvertisement(MpReachNlriBuilder builder, Ipv4Route data) {
            return;
        }

        @Override
        protected KeyedInstanceIdentifier<Ipv4Route, Ipv4RouteKey> identifierForKey(
                InstanceIdentifier<Tables> basePath, Ipv4Prefix id) {
            return basePath.child(Ipv4Routes.class).child(Ipv4Route.class,
                    new Ipv4RouteKey(id));
        }

        @Override
        protected void addWithdrawal(MpUnreachNlriBuilder builder, Ipv4Prefix id) {
            return;
        }

        @Override
        public KeyedInstanceIdentifier<Ipv4Route, Ipv4RouteKey> routeIdentifier(InstanceIdentifier<?> id) {
            return null;
        }

        @Override
        public Ipv4Prefix keyForIdentifier(KeyedInstanceIdentifier<Ipv4Route, Ipv4RouteKey> id) {
            return null;
        }
    }
}
