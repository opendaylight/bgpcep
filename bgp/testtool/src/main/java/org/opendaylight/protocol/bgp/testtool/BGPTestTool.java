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
import io.netty.channel.nio.NioEventLoopGroup;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.protocol.bgp.parser.impl.BGPActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.impl.BGPDispatcherImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.AddPathCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamiliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

class BGPTestTool {
    private final Map<String, BGPSessionListener> listeners = new HashMap<>();
    private BGPDispatcher dispatcher;
    private ArrayList<OptionalCapabilities> optCap;

    void start(final Arguments arguments) {
        initializeActivator();

        optCap = Lists.newArrayList(createMPCapability(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class),
            createMPCapability(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class), createAs4BytesMPCapability(arguments.getAs()));
        if (arguments.getMulipathSupport()) {
            optCap.add(createAddPathCapability());
        }
        final BgpParameters bgpParameters = createBgpParameters(optCap);

        for (final InetSocketAddress localAddress : arguments.getLocalAddresses()) {
            final BGPSessionListener sessionListener = new TestingListener(arguments.getNumberOfPrefixes(), arguments.getExtendedCommunities(),
                arguments.getMulipathSupport());
            listeners.put(localAddress.getHostName(), sessionListener);
            createPeer(this.dispatcher, arguments, localAddress, sessionListener, bgpParameters);
        }
    }

    private void initializeActivator() {
        BGPActivator activator = new BGPActivator();
        final BGPExtensionProviderContext ctx = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance();
        activator.start(ctx);
        this.dispatcher = new BGPDispatcherImpl(ctx.getMessageRegistry(), new NioEventLoopGroup(), new NioEventLoopGroup());
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

    protected static OptionalCapabilities createAddPathCapability() {
        return new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder().addAugmentation(CParameters1.class, new CParameters1Builder()
            .setAddPathCapability(new AddPathCapabilityBuilder().setAddressFamilies(Lists.newArrayList(new AddressFamiliesBuilder()
                .setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class).setSendReceive(SendReceive.Both).build()
            )).build()).build()).build()).build();
    }

    void printCount(final String localAddress) {
        final BGPSessionListener listener = listeners.get(localAddress);
        if (listener != null) {
            ((TestingListener) listener).printCount(localAddress);
        }
    }
}
