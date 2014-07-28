/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.testtool;

import io.netty.channel.nio.NioEventLoopGroup;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.ietf.initiated00.Stateful07SessionProposalFactory;
import org.opendaylight.protocol.pcep.ietf.stateful07.StatefulActivator;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPDispatcherImpl;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static final String USAGE = "DESCRIPTION:\n"
        + "\tCreates a server with given parameters. As long as it runs, it accepts connections " + "from PCCs.\n" + "USAGE:\n"
        + "\t-a, --address\n" + "\t\tthe ip address to which is this server bound.\n"
        + "\t\tFormat: x.x.x.x:y where y is port number.\n\n" +

            "\t-d, --deadtimer\n" + "\t\tin seconds, value of the desired deadtimer\n"
            + "\t\tAccording to RFC5440, recommended value for deadtimer is 4 times the value\n"
            + "\t\tof KeepAlive timer. If it's not, a warning is printed.\n"
            + "\t\tIf not set, it's value will be derived from KeepAlive timer value.\n\n" +

            "\t-ka, --keepalive\n" + "\t\tin seconds, value of the desired KeepAlive timer.\n"
            + "\t\tIf not present, KeepAlive timer will be set to recommended value (30s).\n\n" +

            "\t--stateful\n" + "\t\tpassive stateful\n\n" +

            "\t--active\n" + "\t\tactive stateful (implies --stateful)\n\n" +

            "\t--instant\n"
            + "\t\tinstantiated stateful, <seconds> cleanup timeout (default value, if not included = 0) (implies --stateful)\n\n" +

            "\t-arm, --autoResponseMessages <path to file>\n"
            + "\t\t <path to file> with groovy script which implements MessageGeneratorService.\n"
            + "\t\t Messages are used as auto response for every message received. Purely for testing puposes! \n\n" +

            "\t-psm, --periodicallySendMessages <path to file> <period>\n"
            + "\t\t <path to file> with groovy script which implements MessageGeneratorService followed by <period> in seconds.\n"
            + "\t\t Messages which are sent periodically. Purely for testing puposes! \n\n" +

            "\t-snm, --sendNowMessage <path to file>\n"
            + "\t\t <path to file> with groovy script which implements MessageGeneratorService.\n"
            + "\t\t Messages are sent in defined states defined by programmer. Purely for testing puposes! \n\n" +

            "\t--help\n" + "\t\tdisplay this help and exits\n\n" +

            "With no parameters, this help is printed.";

    private Main() {

    }

    public static void main(final String[] args) throws Exception {
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("--help"))) {
            LOG.info(Main.USAGE);
            return;
        }

        InetSocketAddress address = null;
        int keepAliveValue = 30;
        int deadTimerValue = 0;
        boolean stateful = false;
        boolean active = false;
        final boolean versioned = false;
        boolean instant = false;

        int i = 0;
        while (i < args.length) {
            if (args[i].equalsIgnoreCase("-a") || args[i].equalsIgnoreCase("--address")) {
                final String[] ip = args[i + 1].split(":");
                address = new InetSocketAddress(InetAddress.getByName(ip[0]), Integer.parseInt(ip[1]));
                i++;
            } else if (args[i].equalsIgnoreCase("-d") || args[i].equalsIgnoreCase("--deadtimer")) {
                deadTimerValue = Integer.valueOf(args[i + 1]);
                i++;
            } else if (args[i].equalsIgnoreCase("-ka") || args[i].equalsIgnoreCase("--keepalive")) {
                keepAliveValue = Integer.valueOf(args[i + 1]);
                i++;
            } else if (args[i].equalsIgnoreCase("--stateful")) {
                stateful = true;
            } else if (args[i].equalsIgnoreCase("--active")) {
                stateful = true;
                active = true;
            } else if (args[i].equalsIgnoreCase("--instant")) {
                stateful = true;
                instant = true;
            } else {
                LOG.warn("WARNING: Unrecognized argument: {}", args[i]);
            }
            i++;
        }
        if (deadTimerValue != 0 && deadTimerValue != keepAliveValue * 4) {
            LOG.warn("WARNING: The value of DeadTimer should be 4 times the value of KeepAlive.");
        }
        if (deadTimerValue == 0) {
            deadTimerValue = keepAliveValue * 4;
        }

        final PCEPSessionProposalFactory spf = new Stateful07SessionProposalFactory(deadTimerValue, keepAliveValue, stateful, active, instant);

        final Open prefs = spf.getSessionProposal(address, 0);

        final StatefulActivator activator07 = new StatefulActivator();
        activator07.start(ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance());

        final PCEPDispatcherImpl dispatcher = new PCEPDispatcherImpl(ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance().getMessageHandlerRegistry(), new DefaultPCEPSessionNegotiatorFactory(prefs, 5), new NioEventLoopGroup(), new NioEventLoopGroup());

        dispatcher.createServer(address, new TestingSessionListenerFactory()).get();
    }
}
