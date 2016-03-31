/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.testtool;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import io.netty.channel.nio.NioEventLoopGroup;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.impl.BGPDispatcherImpl;
import org.opendaylight.protocol.bgp.rib.impl.StrictBGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starter class for testing.
 */
public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static final String USAGE = "DESCRIPTION:\n"
        + "\tCreates a server with given parameters. As long as it runs, it accepts connections " + "from PCCs.\n" + "USAGE:\n"
        + "\t-a, --address\n" + "\t\tthe ip address to which is this server bound.\n"
        + "\t\tFormat: x.x.x.x:y where y is port number.\n\n"
        + "\t\tThis IP address will appear in BGP Open message as BGP Identifier of the server.\n" +

            "\t-as\n" + "\t\t value of AS in the initial open message\n\n" +

            "\t-h, --holdtimer\n" + "\t\tin seconds, value of the desired holdtimer\n"
            + "\t\tAccording to RFC4271, recommended value for deadtimer is 90 seconds.\n"
            + "\t\tIf not set, this will be the default value.\n\n" +

            "\t--help\n" + "\t\tdisplay this help and exits\n\n" +

            "With no parameters, this help is printed.";

    private final BGPDispatcherImpl dispatcher;

    private static final int INITIAL_HOLD_TIME = 90;

    private static final int RETRY_TIMER = 10;

    private Main() {
        this.dispatcher = new BGPDispatcherImpl(ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getMessageRegistry(), new NioEventLoopGroup(), new NioEventLoopGroup());
    }

    public static void main(final String[] args) throws UnknownHostException {
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("--help"))) {
            LOG.info(Main.USAGE);
            return;
        }

        InetSocketAddress address = null;
        int holdTimerValue = INITIAL_HOLD_TIME;
        AsNumber as = null;

        int i = 0;
        while (i < args.length) {
            if (args[i].equalsIgnoreCase("-a") || args[i].equalsIgnoreCase("--address")) {
                final String[] ip = args[i + 1].split(":");
                address = new InetSocketAddress(InetAddress.getByName(ip[0]), Integer.parseInt(ip[1]));
                i++;
            } else if (args[i].equalsIgnoreCase("-h") || args[i].equalsIgnoreCase("--holdtimer")) {
                holdTimerValue = Integer.valueOf(args[i + 1]);
                i++;
            } else if (args[i].equalsIgnoreCase("-as")) {
                as = new AsNumber(Long.valueOf(args[i + 1]));
                i++;
            } else {
                LOG.error("WARNING: Unrecognized argument: {}", args[i]);
            }
            i++;
        }

        final Main m = new Main();

        final BGPSessionListener sessionListener = new TestingListener();

        final BgpParameters bgpParameters = createBgpParameters(Lists.newArrayList(
                createMPCapability(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class),
                createMPCapability(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class)));

        final BGPSessionPreferences proposal = new BGPSessionPreferences(as, holdTimerValue, new Ipv4Address("25.25.25.2"), as,
                Collections.singletonList(bgpParameters));

        LOG.debug("{} {} {}", address, sessionListener, proposal);

        final InetSocketAddress addr = address;
        final StrictBGPPeerRegistry strictBGPPeerRegistry = new StrictBGPPeerRegistry();
        strictBGPPeerRegistry.addPeer(StrictBGPPeerRegistry.getIpAddress(address), sessionListener, proposal);

        m.dispatcher.createClient(addr, strictBGPPeerRegistry, RETRY_TIMER);
    }

    @VisibleForTesting
    protected static BgpParameters createBgpParameters(final List<OptionalCapabilities> optionalCapabilities) {
        return new BgpParametersBuilder().setOptionalCapabilities(optionalCapabilities).build();
    }

    @VisibleForTesting
    protected static OptionalCapabilities createMPCapability(final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi) {
        return new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder().addAugmentation(CParameters1.class,
                new CParameters1Builder().setMultiprotocolCapability(
                        new MultiprotocolCapabilityBuilder().setAfi(afi).setSafi(safi).build()).build()).build()).build();
    }
}
