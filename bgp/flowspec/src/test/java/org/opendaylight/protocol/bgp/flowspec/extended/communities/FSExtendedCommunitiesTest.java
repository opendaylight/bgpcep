/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.flowspec.extended.communities;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.flowspec.BGPActivator;
import org.opendaylight.protocol.bgp.flowspec.FlowspecActivator;
import org.opendaylight.protocol.bgp.flowspec.SimpleFlowspecExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunityRegistry;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.Dscp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.redirect.as4.extended.community.RedirectAs4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.redirect.extended.community.RedirectExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.redirect.ip.nh.extended.community.RedirectIpNhExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.redirect.ipv4.extended.community.RedirectIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.redirect.ipv6.extended.community.RedirectIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.traffic.action.extended.community.TrafficActionExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.traffic.marking.extended.community.TrafficMarkingExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.traffic.rate.extended.community.TrafficRateExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.RedirectAs4ExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.RedirectAs4ExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.RedirectExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.RedirectExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.RedirectIpNhExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.RedirectIpNhExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.RedirectIpv4ExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.RedirectIpv4ExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.RedirectIpv6ExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.RedirectIpv6ExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.TrafficActionExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.TrafficActionExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.TrafficMarkingExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.TrafficMarkingExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.TrafficRateExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.TrafficRateExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.ExtendedCommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ShortAsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;

public class FSExtendedCommunitiesTest {

    private static final byte[] TRAFFIC_RATE = {(byte)128, 6, 0, 72, 0, 1, 2, 3};

    private static final byte[] TRAFFIC_ACTION = {(byte)128, 7, 0, 0, 0, 0, 0, 3};

    private static final byte[] REDIRECT_AS_2BYTES = {(byte)128, 8, 0, 35, 4, 2, 8, 7};

    private static final byte[] TRAFFIC_MARKING = {(byte)128, 9, 0, 0, 0, 0, 0, 63};

    private static final byte[] REDIRECT_IPV6 = {(byte)128, 11, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 1, 2};

    private static final byte[] REDIRECT_AS_4BYTES = {(byte)130, 8, 0, 0, 0x19, (byte) 0x94, 0, 126};

    private static final byte[] REDIRECT_IPV4 = {(byte)129, 8, 127, 0, 0, 1, 0, 126};

    private static final byte[] REDIRECT_NH_IPV4 = {8, 0, 127, 0, 0, 1, 0, 1};

    private static final byte[] REDIRECT_NH_IPV6 = {8, 0, 0x20, (byte) 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0, 0};

    private ExtendedCommunityRegistry registry;
    private BGPActivator act;

    @Before
    public void setUp() throws Exception {
        final SimpleFlowspecExtensionProviderContext fsContext = new SimpleFlowspecExtensionProviderContext();
        final FlowspecActivator activator = new FlowspecActivator(fsContext);

        this.act= new BGPActivator(activator);
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        this.act.start(context);
        this.registry = context.getExtendedCommunityRegistry();
    }

    @After
    public void tearDown() {
        this.act.close();
    }

    @Test
    public void testTrafficRateParser() throws BGPDocumentedException, BGPParsingException {
        final TrafficRateExtendedCommunityCase trafficRate = new TrafficRateExtendedCommunityCaseBuilder().setTrafficRateExtendedCommunity(
                new TrafficRateExtendedCommunityBuilder().setInformativeAs(new ShortAsNumber(72L))
                        .setLocalAdministrator(new Bandwidth(new byte[] { 0, 1, 2, 3 })).build()).build();
        final ExtendedCommunities expected = new ExtendedCommunitiesBuilder().setExtendedCommunity(trafficRate).setTransitive(true).build();

        final ExtendedCommunities parsed = this.registry.parseExtendedCommunity(Unpooled.copiedBuffer(TRAFFIC_RATE));
        Assert.assertEquals(expected, parsed);
    }

    @Test
    public void testTrafficRateSerializer() throws BGPDocumentedException, BGPParsingException {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.route.attributes.extended.communities.extended.community.TrafficRateExtendedCommunityCase trafficRate = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.route.attributes.extended.communities.extended.community.TrafficRateExtendedCommunityCaseBuilder().setTrafficRateExtendedCommunity(
                new TrafficRateExtendedCommunityBuilder().setInformativeAs(new ShortAsNumber(72L))
                        .setLocalAdministrator(new Bandwidth(new byte[] { 0, 1, 2, 3 })).build()).build();
        final ExtendedCommunities expected = new ExtendedCommunitiesBuilder().setExtendedCommunity(trafficRate).setTransitive(true).build();

        final ByteBuf output = Unpooled.buffer(TRAFFIC_RATE.length);
        this.registry.serializeExtendedCommunity(expected, output);
        Assert.assertArrayEquals(TRAFFIC_RATE, output.array());
    }

    @Test
    public void testTrafficActionParser() throws BGPDocumentedException, BGPParsingException {
        final TrafficActionExtendedCommunityCase trafficAction = new TrafficActionExtendedCommunityCaseBuilder().setTrafficActionExtendedCommunity(
                new TrafficActionExtendedCommunityBuilder().setSample(true).setTerminalAction(true).build()).build();
        final ExtendedCommunities expected = new ExtendedCommunitiesBuilder().setExtendedCommunity(trafficAction).setTransitive(true).build();

        final ExtendedCommunities parsed = this.registry.parseExtendedCommunity(Unpooled.copiedBuffer(TRAFFIC_ACTION));
        Assert.assertEquals(expected, parsed);
    }

    @Test
    public void testTrafficActionSerializer() throws BGPDocumentedException, BGPParsingException {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.route.attributes.extended.communities.extended.community.TrafficActionExtendedCommunityCase trafficAction = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.route.attributes.extended.communities.extended.community.TrafficActionExtendedCommunityCaseBuilder().setTrafficActionExtendedCommunity(
                new TrafficActionExtendedCommunityBuilder().setSample(true).setTerminalAction(true).build()).build();
        final ExtendedCommunities expected = new ExtendedCommunitiesBuilder().setExtendedCommunity(trafficAction).setTransitive(true).build();

        final ByteBuf output = Unpooled.buffer(TRAFFIC_ACTION.length);
        this.registry.serializeExtendedCommunity(expected, output);
        Assert.assertArrayEquals(TRAFFIC_ACTION, output.array());
    }

    @Test
    public void testTrafficMarkingParser() throws BGPDocumentedException, BGPParsingException {
        final TrafficMarkingExtendedCommunityCase trafficMarking = new TrafficMarkingExtendedCommunityCaseBuilder().setTrafficMarkingExtendedCommunity(
                new TrafficMarkingExtendedCommunityBuilder().setGlobalAdministrator(new Dscp((short) 63)).build()).build();
        final ExtendedCommunities expected = new ExtendedCommunitiesBuilder().setExtendedCommunity(trafficMarking).setTransitive(true).build();

        final ExtendedCommunities parsed = this.registry.parseExtendedCommunity(Unpooled.copiedBuffer(TRAFFIC_MARKING));
        Assert.assertEquals(expected, parsed);
    }

    @Test
    public void testTrafficMarkingSerializer() throws BGPDocumentedException, BGPParsingException {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.route.attributes.extended.communities.extended.community.TrafficMarkingExtendedCommunityCase trafficMarking = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.route.attributes.extended.communities.extended.community.TrafficMarkingExtendedCommunityCaseBuilder().setTrafficMarkingExtendedCommunity(
                new TrafficMarkingExtendedCommunityBuilder().setGlobalAdministrator(new Dscp((short) 63)).build()).build();
        final ExtendedCommunities expected = new ExtendedCommunitiesBuilder().setExtendedCommunity(trafficMarking).setTransitive(true).build();

        final ByteBuf output = Unpooled.buffer(TRAFFIC_MARKING.length);
        this.registry.serializeExtendedCommunity(expected, output);
        Assert.assertArrayEquals(TRAFFIC_MARKING, output.array());
    }

    @Test
    public void testRedirect2bParser() throws BGPDocumentedException, BGPParsingException {
        final RedirectExtendedCommunityCase redirect = new RedirectExtendedCommunityCaseBuilder().setRedirectExtendedCommunity(
                new RedirectExtendedCommunityBuilder().setGlobalAdministrator(new ShortAsNumber(35L)).setLocalAdministrator(
                        new byte[] { 4, 2, 8, 7 }).build()).build();
        final ExtendedCommunities expected = new ExtendedCommunitiesBuilder().setExtendedCommunity(redirect).setTransitive(true).build();

        final ExtendedCommunities parsed = this.registry.parseExtendedCommunity(Unpooled.copiedBuffer(REDIRECT_AS_2BYTES));
        Assert.assertEquals(expected, parsed);
    }

    @Test
    public void testredirect2bSerializer() throws BGPDocumentedException, BGPParsingException {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.route.attributes.extended.communities.extended.community.RedirectExtendedCommunityCase redirect = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.route.attributes.extended.communities.extended.community.RedirectExtendedCommunityCaseBuilder().setRedirectExtendedCommunity(
                new RedirectExtendedCommunityBuilder().setGlobalAdministrator(new ShortAsNumber(35L)).setLocalAdministrator(
                        new byte[] { 4, 2, 8, 7 }).build()).build();
        final ExtendedCommunities expected = new ExtendedCommunitiesBuilder().setExtendedCommunity(redirect).setTransitive(true).build();

        final ByteBuf output = Unpooled.buffer(REDIRECT_AS_2BYTES.length);
        this.registry.serializeExtendedCommunity(expected, output);
        Assert.assertArrayEquals(REDIRECT_AS_2BYTES, output.array());
    }

    @Test
    public void testRedirectIpv6Parser() throws BGPDocumentedException, BGPParsingException {
        final RedirectIpv6ExtendedCommunityCase redirect = new RedirectIpv6ExtendedCommunityCaseBuilder()
                .setRedirectIpv6(new RedirectIpv6Builder()
                        .setGlobalAdministrator(new Ipv6Address("102:304:506:708:90a:b0c:d0e:f10"))
                        .setLocalAdministrator(258).build()).build();
        final ExtendedCommunities expected = new ExtendedCommunitiesBuilder().setExtendedCommunity(redirect).setTransitive(true).build();

        final ExtendedCommunities parsed = this.registry.parseExtendedCommunity(Unpooled.copiedBuffer(REDIRECT_IPV6));
        Assert.assertEquals(expected, parsed);
    }

    @Test
    public void testRedirectIpv6Serializer() {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.route.attributes.extended.communities.extended.community.RedirectIpv6ExtendedCommunityCase redirect = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.route.attributes.extended.communities.extended.community.RedirectIpv6ExtendedCommunityCaseBuilder()
                .setRedirectIpv6(new RedirectIpv6Builder()
                        .setGlobalAdministrator(new Ipv6Address("102:304:506:708:90a:b0c:d0e:f10"))
                        .setLocalAdministrator(258).build()).build();
        final ExtendedCommunities expected = new ExtendedCommunitiesBuilder().setExtendedCommunity(redirect).setTransitive(true).build();

        final ByteBuf output = Unpooled.buffer(REDIRECT_IPV6.length);
        this.registry.serializeExtendedCommunity(expected, output);
        Assert.assertArrayEquals(REDIRECT_IPV6, output.array());
    }

    @Test
    public void testRedirect4bParser() throws BGPDocumentedException, BGPParsingException {
        final RedirectAs4ExtendedCommunityCase redirect = new RedirectAs4ExtendedCommunityCaseBuilder().setRedirectAs4(
                new RedirectAs4Builder().setGlobalAdministrator(new AsNumber(6548L)).setLocalAdministrator(126).build()).build();

        final ExtendedCommunities expected = new ExtendedCommunitiesBuilder().setExtendedCommunity(redirect).setTransitive(true).build();

        final ExtendedCommunities parsed = this.registry.parseExtendedCommunity(Unpooled.copiedBuffer(REDIRECT_AS_4BYTES));
        Assert.assertEquals(expected, parsed);
    }

    @Test
    public void testredirect4bSerializer() throws BGPDocumentedException, BGPParsingException {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.route.attributes.extended.communities.extended.community.RedirectAs4ExtendedCommunityCase redirect = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.route.attributes.extended.communities.extended.community.RedirectAs4ExtendedCommunityCaseBuilder().setRedirectAs4(
                new RedirectAs4Builder().setGlobalAdministrator(new AsNumber(6548L)).setLocalAdministrator(126).build()).build();

        final ExtendedCommunities expected = new ExtendedCommunitiesBuilder().setExtendedCommunity(redirect).setTransitive(true).build();

        final ByteBuf output = Unpooled.buffer(REDIRECT_AS_4BYTES.length);
        this.registry.serializeExtendedCommunity(expected, output);
        Assert.assertArrayEquals(REDIRECT_AS_4BYTES, output.array());
    }

    @Test
    public void testRedirectIpv4Parser() throws BGPDocumentedException, BGPParsingException {
        final RedirectIpv4ExtendedCommunityCase redirect = new RedirectIpv4ExtendedCommunityCaseBuilder().setRedirectIpv4(
                new RedirectIpv4Builder().setGlobalAdministrator(new Ipv4Address("127.0.0.1")).setLocalAdministrator(126).build()).build();

        final ExtendedCommunities expected = new ExtendedCommunitiesBuilder().setExtendedCommunity(redirect).setTransitive(true).build();

        final ExtendedCommunities parsed = this.registry.parseExtendedCommunity(Unpooled.copiedBuffer(REDIRECT_IPV4));
        Assert.assertEquals(expected, parsed);
    }

    @Test
    public void testredirectIpv4Serializer() throws BGPDocumentedException, BGPParsingException {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.route.attributes.extended.communities.extended.community.RedirectIpv4ExtendedCommunityCase redirect = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.route.attributes.extended.communities.extended.community.RedirectIpv4ExtendedCommunityCaseBuilder().setRedirectIpv4(
                new RedirectIpv4Builder().setGlobalAdministrator(new Ipv4Address("127.0.0.1")).setLocalAdministrator(126).build()).build();

        final ExtendedCommunities expected = new ExtendedCommunitiesBuilder().setExtendedCommunity(redirect).setTransitive(true).build();

        final ByteBuf output = Unpooled.buffer(REDIRECT_IPV4.length);
        this.registry.serializeExtendedCommunity(expected, output);
        Assert.assertArrayEquals(REDIRECT_IPV4, output.array());
    }

    @Test
    public void testRedirectIpv4NhParser() throws BGPDocumentedException, BGPParsingException {
        final RedirectIpNhExtendedCommunityCase redirect = new RedirectIpNhExtendedCommunityCaseBuilder().setRedirectIpNhExtendedCommunity(
                new RedirectIpNhExtendedCommunityBuilder().setNextHopAddress(new IpAddress(new Ipv4Address("127.0.0.1"))).setCopy(true).build()).build();

        final ExtendedCommunities expected = new ExtendedCommunitiesBuilder().setExtendedCommunity(redirect).setTransitive(true).build();

        final ExtendedCommunities parsed = this.registry.parseExtendedCommunity(Unpooled.copiedBuffer(REDIRECT_NH_IPV4));
        Assert.assertEquals(expected, parsed);
    }

    @Test
    public void testredirectIpv4NhSerializer() throws BGPDocumentedException, BGPParsingException {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.route.attributes.extended.communities.extended.community.RedirectIpNhExtendedCommunityCase redirect = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.route.attributes.extended.communities.extended.community.RedirectIpNhExtendedCommunityCaseBuilder().setRedirectIpNhExtendedCommunity(
                new RedirectIpNhExtendedCommunityBuilder().setNextHopAddress(new IpAddress(new Ipv4Address("127.0.0.1"))).setCopy(true).build()).build();

        final ExtendedCommunities expected = new ExtendedCommunitiesBuilder().setExtendedCommunity(redirect).setTransitive(true).build();

        final ByteBuf output = Unpooled.buffer(REDIRECT_NH_IPV4.length);
        this.registry.serializeExtendedCommunity(expected, output);
        Assert.assertArrayEquals(REDIRECT_NH_IPV4, output.array());
    }

    @Test
    public void testRedirectIpv6NhParser() throws BGPDocumentedException, BGPParsingException {
        final RedirectIpNhExtendedCommunityCase redirect = new RedirectIpNhExtendedCommunityCaseBuilder().setRedirectIpNhExtendedCommunity(
                new RedirectIpNhExtendedCommunityBuilder().setNextHopAddress(new IpAddress(new Ipv6Address("2001::1"))).setCopy(false).build()).build();

        final ExtendedCommunities expected = new ExtendedCommunitiesBuilder().setExtendedCommunity(redirect).setTransitive(true).build();

        final ExtendedCommunities parsed = this.registry.parseExtendedCommunity(Unpooled.copiedBuffer(REDIRECT_NH_IPV6));
        Assert.assertEquals(expected, parsed);
    }

    @Test
    public void testredirectIpv6NhSerializer() throws BGPDocumentedException, BGPParsingException {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.route.attributes.extended.communities.extended.community.RedirectIpNhExtendedCommunityCase redirect = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.route.attributes.extended.communities.extended.community.RedirectIpNhExtendedCommunityCaseBuilder().setRedirectIpNhExtendedCommunity(
                new RedirectIpNhExtendedCommunityBuilder().setNextHopAddress(new IpAddress(new Ipv6Address("2001::1"))).build()).build();

        final ExtendedCommunities expected = new ExtendedCommunitiesBuilder().setExtendedCommunity(redirect).setTransitive(true).build();

        final ByteBuf output = Unpooled.buffer(REDIRECT_NH_IPV6.length);
        this.registry.serializeExtendedCommunity(expected, output);
        Assert.assertArrayEquals(REDIRECT_NH_IPV6, output.array());
    }

}
