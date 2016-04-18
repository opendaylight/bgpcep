/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.opendaylight.protocol.bgp.linkstate.nlri.LinkNlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.LinkstateNlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.NodeNlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.PrefixNlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.SimpleNlriTypeRegistry;
import org.opendaylight.protocol.bgp.linkstate.nlri.TeLspIpv4NlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.TeLspIpv6NlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.TeLspNlriSerializer;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.linkstate.tlvs.AreaIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.AsNumTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.BgpRouterIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.CrouterIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.DomainIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.IpReachTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.Ipv4IfaceAddTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.Ipv4NeighborAddTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.Ipv6IFaceAddTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.LinkDescTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.MemAsNumTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.MultiTopoIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.NodeDescTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.OspfRTypeTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.PrefixDescTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.RemoteNodeDescTlvParser;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.protocol.rsvp.parser.spi.pojo.ServiceLoaderRSVPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.TeLspCase;

public class ActivatorTest {

    @Test
    public void testActivator() throws Exception {
        final BGPActivator act = new BGPActivator(true, ServiceLoaderRSVPExtensionProviderContext.getSingletonInstance().getRsvpRegistry());
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        final SimpleNlriTypeRegistry typeReg = SimpleNlriTypeRegistry.getInstance();

        assertNull(context.getAddressFamilyRegistry().classForFamily(16388));
        assertNull(context.getSubsequentAddressFamilyRegistry().classForFamily(71));

        act.start(context);

        assertEquals(LinkstateAddressFamily.class, context.getAddressFamilyRegistry().classForFamily(16388));
        assertEquals(LinkstateSubsequentAddressFamily.class, context.getSubsequentAddressFamilyRegistry().classForFamily(71));
        assertTrue(typeReg.getParser(NlriType.Node) instanceof NodeNlriParser);
        assertTrue(typeReg.getParser(NlriType.Link) instanceof LinkNlriParser);
        assertTrue(typeReg.getParser(NlriType.Ipv4Prefix) instanceof PrefixNlriParser);
        assertTrue(typeReg.getParser(NlriType.Ipv6Prefix) instanceof PrefixNlriParser);
        assertTrue(typeReg.getParser(NlriType.Ipv4TeLsp) instanceof TeLspIpv4NlriParser);
        assertTrue(typeReg.getParser(NlriType.Ipv6TeLsp) instanceof TeLspIpv6NlriParser);
        assertTrue(typeReg.getTlvParser(LinkstateNlriParser.NODE_DESCRIPTORS_NID) instanceof NodeDescTlvParser);
        assertTrue(typeReg.getTlvParser(LinkstateNlriParser.LOCAL_NODE_DESCRIPTORS_NID) instanceof NodeDescTlvParser);
        assertTrue(typeReg.getTlvParser(LinkstateNlriParser.ADVERTISING_NODE_DESCRIPTORS_NID) instanceof NodeDescTlvParser);
        assertTrue(typeReg.getTlvParser(LinkstateNlriParser.REMOTE_NODE_DESCRIPTORS_NID) instanceof RemoteNodeDescTlvParser);
        assertTrue(typeReg.getTlvParser(LinkstateNlriParser.LINK_DESCRIPTORS_NID) instanceof LinkDescTlvParser);
        assertTrue(typeReg.getTlvParser(LinkstateNlriParser.PREFIX_DESCRIPTORS_NID) instanceof PrefixDescTlvParser);
        assertTrue(typeReg.getSubTlvParser(AsNumTlvParser.AS_NUMBER) instanceof AsNumTlvParser);
        assertTrue(typeReg.getSubTlvParser(AreaIdTlvParser.AREA_ID) instanceof AreaIdTlvParser);
        assertTrue(typeReg.getSubTlvParser(BgpRouterIdTlvParser.BGP_ROUTER_ID) instanceof BgpRouterIdTlvParser);
        assertTrue(typeReg.getSubTlvParser(CrouterIdTlvParser.IGP_ROUTER_ID) instanceof CrouterIdTlvParser);
        assertTrue(typeReg.getSubTlvParser(DomainIdTlvParser.BGP_LS_ID) instanceof DomainIdTlvParser);
        assertTrue(typeReg.getSubTlvParser(IpReachTlvParser.IP_REACHABILITY) instanceof IpReachTlvParser);
        assertTrue(typeReg.getSubTlvParser(Ipv4IfaceAddTlvParser.IPV4_IFACE_ADDRESS) instanceof Ipv4IfaceAddTlvParser);
        assertTrue(typeReg.getSubTlvParser(Ipv4NeighborAddTlvParser.IPV4_NEIGHBOR_ADDRESS) instanceof Ipv4NeighborAddTlvParser);
        assertTrue(typeReg.getSubTlvParser(Ipv6IFaceAddTlvParser.IPV6_IFACE_ADDRESS) instanceof Ipv6IFaceAddTlvParser);
        assertTrue(typeReg.getSubTlvParser(MemAsNumTlvParser.MEMBER_AS_NUMBER) instanceof MemAsNumTlvParser);
        assertTrue(typeReg.getSubTlvParser(TlvUtil.MULTI_TOPOLOGY_ID) instanceof MultiTopoIdTlvParser);
        assertTrue(typeReg.getSubTlvParser(OspfRTypeTlvParser.OSPF_ROUTE_TYPE) instanceof OspfRTypeTlvParser);
        assertTrue(typeReg.getSerializer(NodeCase.class) instanceof NodeNlriParser);
        assertTrue(typeReg.getSerializer(LinkCase.class) instanceof LinkNlriParser);
        assertTrue(typeReg.getSerializer(PrefixCase.class) instanceof PrefixNlriParser);
        assertTrue(typeReg.getSerializer(TeLspCase.class) instanceof TeLspNlriSerializer);

        act.close();
    }

    @Test
    public void testRIBActivator() {
        final RIBActivator ribAct = new RIBActivator();
        final RIBExtensionProviderContext context = new SimpleRIBExtensionProviderContext();

        assertNull(context.getRIBSupport(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class));

        ribAct.startRIBExtensionProvider(context);

        assertNotNull(context.getRIBSupport(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class));

        ribAct.close();
    }
}
