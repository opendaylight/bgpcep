/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.testtool;

import static org.opendaylight.protocol.bgp.testtool.BGPPeerBuilder.createPeer;

import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import io.netty.channel.nio.NioEventLoopGroup;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.protocol.bgp.flowspec.FlowspecActivator;
import org.opendaylight.protocol.bgp.flowspec.SimpleFlowspecExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.impl.BGPActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.impl.BGPDispatcherImpl;
import org.opendaylight.protocol.bgp.rib.impl.StrictBGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.AddPathCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.add.path.capability.AddressFamiliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

final class BGPTestTool {
    private final Map<String, BGPSessionListener> listeners = new HashMap<>();

    void start(final Arguments arguments) {
        final BGPDispatcher dispatcher = initializeActivator();

        final ArrayList<OptionalCapabilities> optCap = Lists.newArrayList(createMPCapability(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class),
            createMPCapability(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class), createAs4BytesMPCapability(arguments.getAs()));
        if (arguments.getMultiPathSupport()) {
            optCap.add(createAddPathCapability());
        }
        final BgpParameters bgpParameters = createBgpParameters(optCap);

        final InetSocketAddress localAddress = arguments.getLocalAddresses();
        final int port = localAddress.getPort();
        InetAddress address = localAddress.getAddress();
        int numberOfSpeakers = arguments.getSpeakerCount();
        do {
            final BGPSessionListener sessionListener = new TestingListener(arguments.getNumberOfPrefixes(), arguments.getExtendedCommunities(),
                arguments.getMultiPathSupport());
            this.listeners.put(address.getHostAddress(), sessionListener);
            createPeer(dispatcher, arguments, new InetSocketAddress(address, port), sessionListener, bgpParameters);
            numberOfSpeakers--;
            address = InetAddresses.increment(address);
        } while (numberOfSpeakers > 0);
    }

    private static BGPDispatcher initializeActivator() {
        final BGPActivator activator = new BGPActivator();
        final BGPExtensionProviderContext ctx = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance();
        activator.start(ctx);

        final org.opendaylight.protocol.bgp.inet.BGPActivator inetActivator = new org.opendaylight.protocol.bgp.inet.BGPActivator();
        inetActivator.start(ctx);

        final org.opendaylight.protocol.bgp.evpn.impl.BGPActivator evpnActivator = new org.opendaylight.protocol.bgp.evpn.impl.BGPActivator();
        evpnActivator.start(ctx);

        final SimpleFlowspecExtensionProviderContext fsContext = new SimpleFlowspecExtensionProviderContext();
        final FlowspecActivator flowspecActivator = new FlowspecActivator(fsContext);
        final org.opendaylight.protocol.bgp.flowspec.BGPActivator flowspecBGPActivator = new org.opendaylight.protocol.bgp.flowspec.BGPActivator(flowspecActivator);
        flowspecBGPActivator.start(ctx);

        final org.opendaylight.protocol.bgp.labeled.unicast.BGPActivator labeledActivator = new org.opendaylight.protocol.bgp.labeled.unicast.BGPActivator();
        labeledActivator.start(ctx);

        final org.opendaylight.protocol.bgp.l3vpn.ipv4.BgpIpv4Activator bgpIpv4Activator = new org.opendaylight.protocol.bgp.l3vpn.ipv4.BgpIpv4Activator();
        bgpIpv4Activator.start(ctx);

        final org.opendaylight.protocol.bgp.l3vpn.ipv6.BgpIpv6Activator bgpIpv6Activator = new org.opendaylight.protocol.bgp.l3vpn.ipv6.BgpIpv6Activator();
        bgpIpv6Activator.start(ctx);

        return new BGPDispatcherImpl(ctx.getMessageRegistry(), new NioEventLoopGroup(), new NioEventLoopGroup(),
            new StrictBGPPeerRegistry());
    }

    private static OptionalCapabilities createMPCapability(final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi) {
        return new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder().addAugmentation(CParameters1.class, new CParameters1Builder()
            .setMultiprotocolCapability(new MultiprotocolCapabilityBuilder().setAfi(afi).setSafi(safi).build()).build()).build()).build();
    }

    private static OptionalCapabilities createAs4BytesMPCapability(final AsNumber as) {
        return new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder()
            .setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(as).build()).build()).build();
    }

    private static BgpParameters createBgpParameters(final List<OptionalCapabilities> optionalCapabilities) {
        return new BgpParametersBuilder().setOptionalCapabilities(optionalCapabilities).build();
    }

    private static OptionalCapabilities createAddPathCapability() {
        return new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder().addAugmentation(CParameters1.class, new CParameters1Builder()
            .setAddPathCapability(new AddPathCapabilityBuilder().setAddressFamilies(Lists.newArrayList(new AddressFamiliesBuilder()
                .setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class).setSendReceive(SendReceive.Both).build()
            )).build()).build()).build()).build();
    }

    void printCount(final String localAddress) {
        final BGPSessionListener listener = this.listeners.get(localAddress);
        if (listener != null) {
            ((TestingListener) listener).printCount(localAddress);
        }
    }
}
