/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.testtool;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutionException;
import org.opendaylight.netconf.transport.spi.TcpMd5Secrets;
import org.opendaylight.protocol.pcep.MessageRegistry;
import org.opendaylight.protocol.pcep.PCEPTimerProposal;
import org.opendaylight.protocol.pcep.ietf.stateful.PCEPStatefulCapability;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPDispatcherImpl;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionConsumerContext;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    // FIXME: inline as a text block
    private static final String USAGE = """
        DESCRIPTION:
        	Creates a server with given parameters. As long as it runs, it accepts connections \
        from PCCs.
        USAGE:
        	-a, --address
        		the ip address to which is this server bound.
        		Format: x.x.x.x:y where y is port number.

        	-d, --deadtimer
        		in seconds, value of the desired deadtimer
        		According to RFC5440, recommended value for deadtimer is 4 times the value
        		of KeepAlive timer. If it's not, a warning is printed.
        		If not set, it's value will be derived from KeepAlive timer value.

        	-ka, --keepalive
        		in seconds, value of the desired KeepAlive timer.
        		If not present, KeepAlive timer will be set to recommended value (30s).

        	--stateful
        		passive stateful

        	--active
        		active stateful (implies --stateful)

        	--instant
        		instantiated stateful, <seconds> cleanup timeout \
        (default value, if not included = 0) (implies --stateful)

        	-arm, --autoResponseMessages <path to file>
        		 <path to file> with groovy script which implements MessageGeneratorService.
        		 Messages are used as auto response for every message received. Purely for testing puposes!\s

        	-psm, --periodicallySendMessages <path to file> <period>
        		 <path to file> with groovy script which implements\
         MessageGeneratorService followed by <period> in seconds.
        		 Messages which are sent periodically. Purely for testing puposes!\s

        	-snm, --sendNowMessage <path to file>
        		 <path to file> with groovy script which implements MessageGeneratorService.
        		 Messages are sent in defined states defined by programmer. Purely for testing puposes!\s

        	--help
        		display this help and exits

        With no parameters, this help is printed.""";
    private static final int KA_TO_DEADTIMER_RATIO = 4;

    private Main() {

    }

    public static void main(final String[] args) throws UnknownHostException, InterruptedException, ExecutionException {
        if (args.length == 0 || args.length == 1 && args[0].equalsIgnoreCase("--help")) {
            LOG.info(Main.USAGE);
            return;
        }

        InetSocketAddress address = null;
        Uint8 keepAliveValue = Uint8.valueOf(30);
        Uint8 deadTimerValue = Uint8.ZERO;
        // FIXME: add a command-line option for this
        Uint16 maxUnknownMessages = Uint16.valueOf(5);
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

        final MessageRegistry handlerRegistry = ServiceLoader.load(PCEPExtensionConsumerContext.class).findFirst()
            .orElseThrow()
            .getMessageHandlerRegistry();
        final var dispatcher = new PCEPDispatcherImpl();
        dispatcher.createServer(address, TcpMd5Secrets.of(), handlerRegistry,
            new DefaultPCEPSessionNegotiatorFactory(new TestingSessionListenerFactory(),
                new PCEPTimerProposal(keepAliveValue, deadTimerValue),
                stateful ? List.of(new PCEPStatefulCapability(active, instant, false, false, false, false)) : List.of(),
                maxUnknownMessages, null))
            .get();
    }
}
