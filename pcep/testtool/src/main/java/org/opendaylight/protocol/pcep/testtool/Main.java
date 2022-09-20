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
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutionException;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.ietf.stateful.PCEPStatefulCapability;
import org.opendaylight.protocol.pcep.impl.BasePCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPDispatcherImpl;
import org.opendaylight.protocol.pcep.spi.MessageRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionConsumerContext;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static final String USAGE = "DESCRIPTION:\n"
            + "\tCreates a server with given parameters. As long as it runs, it accepts connections "
            + "from PCCs.\n" + "USAGE:\n"
            + "\t-a, --address\n" + "\t\tthe ip address to which is this server bound.\n"
            + "\t\tFormat: x.x.x.x:y where y is port number.\n\n"

            + "\t-d, --deadtimer\n" + "\t\tin seconds, value of the desired deadtimer\n"
            + "\t\tAccording to RFC5440, recommended value for deadtimer is 4 times the value\n"
            + "\t\tof KeepAlive timer. If it's not, a warning is printed.\n"
            + "\t\tIf not set, it's value will be derived from KeepAlive timer value.\n\n"

            + "\t-ka, --keepalive\n" + "\t\tin seconds, value of the desired KeepAlive timer.\n"
            + "\t\tIf not present, KeepAlive timer will be set to recommended value (30s).\n\n"

            + "\t--stateful\n" + "\t\tpassive stateful\n\n"

            + "\t--active\n" + "\t\tactive stateful (implies --stateful)\n\n"

            + "\t--instant\n"
            + "\t\tinstantiated stateful, <seconds> cleanup timeout "
            + "(default value, if not included = 0) (implies --stateful)\n\n"

            + "\t-arm, --autoResponseMessages <path to file>\n"
            + "\t\t <path to file> with groovy script which implements MessageGeneratorService.\n"
            + "\t\t Messages are used as auto response for every message received. Purely for testing puposes! \n\n"

            + "\t-psm, --periodicallySendMessages <path to file> <period>\n"
            + "\t\t <path to file> with groovy script which implements"
            + " MessageGeneratorService followed by <period> in seconds.\n"
            + "\t\t Messages which are sent periodically. Purely for testing puposes! \n\n"

            + "\t-snm, --sendNowMessage <path to file>\n"
            + "\t\t <path to file> with groovy script which implements MessageGeneratorService.\n"
            + "\t\t Messages are sent in defined states defined by programmer. Purely for testing puposes! \n\n"

            + "\t--help\n" + "\t\tdisplay this help and exits\n\n"

            + "With no parameters, this help is printed.";
    private static final int KA_TO_DEADTIMER_RATIO = 4;
    private static final Uint8 KA_DEFAULT = Uint8.valueOf(30);
    private static final int MAX_UNKNOWN_MESSAGES = 5;

    private Main() {

    }

    public static void main(final String[] args) throws UnknownHostException, InterruptedException, ExecutionException {
        if (args.length == 0 || args.length == 1 && args[0].equalsIgnoreCase("--help")) {
            LOG.info(Main.USAGE);
            return;
        }

        InetSocketAddress address = null;
        Uint8 keepAliveValue = KA_DEFAULT;
        Uint8 deadTimerValue = Uint8.ZERO;
        boolean stateful = false;
        boolean active = false;
        boolean instant = false;

        int pos = 0;
        while (pos < args.length) {
            if (args[pos].equalsIgnoreCase("-a") || args[pos].equalsIgnoreCase("--address")) {
                final String[] ip = args[pos + 1].split(":");
                address = new InetSocketAddress(InetAddress.getByName(ip[0]), Integer.parseInt(ip[1]));
                pos++;
            } else if (args[pos].equalsIgnoreCase("-d") || args[pos].equalsIgnoreCase("--deadtimer")) {
                deadTimerValue = Uint8.valueOf(args[pos + 1]);
                pos++;
            } else if (args[pos].equalsIgnoreCase("-ka") || args[pos].equalsIgnoreCase("--keepalive")) {
                keepAliveValue = Uint8.valueOf(args[pos + 1]);
                pos++;
            } else if (args[pos].equalsIgnoreCase("--stateful")) {
                stateful = true;
            } else if (args[pos].equalsIgnoreCase("--active")) {
                stateful = true;
                active = true;
            } else if (args[pos].equalsIgnoreCase("--instant")) {
                stateful = true;
                instant = true;
            } else {
                LOG.warn("WARNING: Unrecognized argument: {}", args[pos]);
            }
            pos++;
        }
        if (Uint8.ZERO.equals(deadTimerValue)) {
            final var newValue = keepAliveValue.toJava() * KA_TO_DEADTIMER_RATIO;
            deadTimerValue = newValue <= Uint8.MAX_VALUE.toJava() ? Uint8.valueOf(newValue) : Uint8.MAX_VALUE;
        } else if (deadTimerValue.toJava() != keepAliveValue.toJava() * KA_TO_DEADTIMER_RATIO) {
            LOG.warn("WARNING: The value of DeadTimer should be 4 times the value of KeepAlive.");
        }

        final List<PCEPCapability> caps = new ArrayList<>();
        caps.add(new PCEPStatefulCapability(stateful, active, instant, false, false, false, false));
        final PCEPSessionProposalFactory spf = new BasePCEPSessionProposalFactory(deadTimerValue, keepAliveValue, caps);

        final MessageRegistry handlerRegistry = ServiceLoader.load(PCEPExtensionConsumerContext.class).findFirst()
            .orElseThrow()
            .getMessageHandlerRegistry();
        final PCEPDispatcherImpl dispatcher = new PCEPDispatcherImpl(handlerRegistry,
            new DefaultPCEPSessionNegotiatorFactory(spf, MAX_UNKNOWN_MESSAGES),
            new NioEventLoopGroup(), new NioEventLoopGroup());
        dispatcher.createServer(new TestToolPCEPDispatcherDependencies(address)).get();
    }
}
