/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;
import com.google.common.net.InetAddresses;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiatorFactory;
import org.opendaylight.protocol.framework.TimedReconnectStrategy;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.ietf.initiated00.CrabbeInitiatedActivator;
import org.opendaylight.protocol.pcep.ietf.stateful07.StatefulActivator;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPSessionImpl;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;
import org.opendaylight.tcpmd5.api.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static final int DEFAULT_REMOTE_PORT = 4189;
    private static final int DEFAULT_LOCAL_PORT = 0;
    private static final short DEFAULT_KEEP_ALIVE = 30;
    private static final short DEFAULT_DEAD_TIMER = 120;
    private static final int RECONNECT_STRATEGY_TIMEOUT = 2000;
    private static final InetAddress LOCALHOST = InetAddresses.forString("127.0.0.1");

    private Main() { }

    public static void main(final String[] args) throws InterruptedException, ExecutionException, UnknownHostException {
        InetSocketAddress localAddress = new InetSocketAddress(LOCALHOST, DEFAULT_LOCAL_PORT);
        List<InetSocketAddress> remoteAddress = Lists.newArrayList(new InetSocketAddress(LOCALHOST, DEFAULT_REMOTE_PORT));
        int pccCount = 1;
        int lsps = 1;
        boolean pcError = false;
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        short ka = DEFAULT_KEEP_ALIVE;
        short dt = DEFAULT_DEAD_TIMER;
        String password = null;
        int reconnectTime = -1;

        getRootLogger(lc).setLevel(ch.qos.logback.classic.Level.INFO);
        int argIdx = 0;
        while (argIdx < args.length) {
            if (args[argIdx].equals("--local-address")) {
                localAddress = getInetSocketAddress(args[++argIdx], DEFAULT_LOCAL_PORT);
            } else if (args[argIdx].equals("--remote-address")) {
                remoteAddress = parseAddresses(args[++argIdx], DEFAULT_REMOTE_PORT);
            } else if (args[argIdx].equals("--pcc")) {
                pccCount = Integer.valueOf(args[++argIdx]);
            } else if (args[argIdx].equals("--lsp")) {
                lsps = Integer.valueOf(args[++argIdx]);
            } else if (args[argIdx].equals("--pcerr")) {
                pcError = true;
            } else if (args[argIdx].equals("--log-level")) {
                getRootLogger(lc).setLevel(Level.toLevel(args[++argIdx], ch.qos.logback.classic.Level.INFO));
            } else if (args[argIdx].equals("--keepalive") || args[argIdx].equals("-ka")) {
                ka = Short.valueOf(args[++argIdx]);
            } else if (args[argIdx].equals("--deadtimer") || args[argIdx].equals("-d")) {
                dt = Short.valueOf(args[++argIdx]);
            } else if (args[argIdx].equals("--password")) {
                password = args[++argIdx];
            } else if (args[argIdx].equals("--reconnect")) {
                reconnectTime = Integer.valueOf(args[++argIdx]).intValue() * 1000;
            } else {
                LOG.warn("WARNING: Unrecognized argument: {}", args[argIdx]);
            }
            argIdx++;
        }
        createPCCs(lsps, pcError, pccCount, localAddress, remoteAddress, ka, dt, password, reconnectTime);
    }

    public static void createPCCs(final int lspsPerPcc, final boolean pcerr, final int pccCount,
            final InetSocketAddress localAddress, final List<InetSocketAddress> remoteAddress, final short keepalive, final short deadtimer,
            final String password, final int reconnectTime) throws InterruptedException, ExecutionException {
        startActivators();
        InetAddress currentAddress = localAddress.getAddress();
        final Open openMessage = getOpenMessage(keepalive, deadtimer);
        final PCCDispatcher pccDispatcher = new PCCDispatcher(ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance().getMessageHandlerRegistry(),
                getSessionNegotiatorFactory(openMessage));
        for (int i = 0; i < pccCount; i++) {
            createPCC(lspsPerPcc, pcerr, new InetSocketAddress(currentAddress, localAddress.getPort()),
                    remoteAddress, openMessage, pccDispatcher, password, reconnectTime);
            currentAddress = InetAddresses.increment(currentAddress);
        }
    }

    @SuppressWarnings("deprecation")
    private static void createPCC(final int lspsPerPcc, final boolean pcerr, final InetSocketAddress localAddress,
            final List<InetSocketAddress> remoteAddress, final Open openMessage, final PCCDispatcher pccDispatcher,
            final String password, final int reconnectTime) throws InterruptedException, ExecutionException {
        final SessionNegotiatorFactory<Message, PCEPSessionImpl, PCEPSessionListener> snf = getSessionNegotiatorFactory(openMessage);
        for (final InetSocketAddress pceAddress : remoteAddress) {
            pccDispatcher.createClient(localAddress, pceAddress, reconnectTime == -1 ? getNeverReconnectStrategyFactory() : getTimedReconnectStrategyFactory(reconnectTime), new SessionListenerFactory<PCEPSessionListener>() {
                @Override
                public PCEPSessionListener getSessionListener() {
                    return new SimpleSessionListener(lspsPerPcc, pcerr, localAddress.getAddress());
                }
            }, snf, getKeyMapping(pceAddress.getAddress(), password));
        }
    }

    @SuppressWarnings("deprecation")
    private static SessionNegotiatorFactory<Message, PCEPSessionImpl, PCEPSessionListener> getSessionNegotiatorFactory(final Open openMessage) {
        return new DefaultPCEPSessionNegotiatorFactory(openMessage, 0);
    }

    private static ch.qos.logback.classic.Logger getRootLogger(final LoggerContext lc) {
        return Iterables.find(lc.getLoggerList(), new Predicate<Logger>() {
            @Override
            public boolean apply(final Logger input) {
                return (input != null) ? input.getName().equals(Logger.ROOT_LOGGER_NAME) : false;
            }
        });
    }

    private static List<InetSocketAddress> parseAddresses(final String address, final int defaultPort) {
        return Lists.transform(Arrays.asList(address.split(",")), new Function<String, InetSocketAddress>() {
            @Override
            public InetSocketAddress apply(final String input) {
                return getInetSocketAddress(input, defaultPort);
            }
        });
    }

    private static InetSocketAddress getInetSocketAddress(final String hostPortString, final int defaultPort) {
        final HostAndPort hostAndPort = HostAndPort.fromString(hostPortString).withDefaultPort(defaultPort);
        return new InetSocketAddress(hostAndPort.getHostText(), hostAndPort.getPort());
    }

    private static Optional<KeyMapping> getKeyMapping(final InetAddress inetAddress, final String password) {
        if (password == null) {
            return Optional.absent();
        }
        final KeyMapping keyMapping = new KeyMapping();
        keyMapping.put(inetAddress, password.getBytes(Charsets.US_ASCII));
        return Optional.of(keyMapping);
    }

    private static Open getOpenMessage(final short keepalive, final short deadtimer) {
        final Tlvs1 tlvs1 = new Tlvs1Builder().setStateful(new StatefulBuilder().addAugmentation(Stateful1.class,
                new Stateful1Builder().setInitiation(true).build()).setLspUpdateCapability(true).build()).build();
        return new OpenBuilder().setTlvs(new TlvsBuilder().addAugmentation(Tlvs1.class, tlvs1).build())
                .setKeepalive(keepalive).setDeadTimer(deadtimer).setSessionId((short) 0).build();
    }

    private static void startActivators() {
        final PCCActivator pccActivator = new PCCActivator();
        final StatefulActivator stateful = new StatefulActivator();
        final CrabbeInitiatedActivator activator = new CrabbeInitiatedActivator();
        final PCEPExtensionProviderContext ctx = ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance();
        pccActivator.start(ctx);
        stateful.start(ctx);
        activator.start(ctx);
    }

    @SuppressWarnings("deprecation")
    private static ReconnectStrategyFactory getNeverReconnectStrategyFactory() {
        return new ReconnectStrategyFactory() {

            @Override
            public ReconnectStrategy createReconnectStrategy() {
                return new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, RECONNECT_STRATEGY_TIMEOUT);
            }
        };
    }

    @SuppressWarnings("deprecation")
    private static ReconnectStrategyFactory getTimedReconnectStrategyFactory(final int reconnectTime) {
        return new ReconnectStrategyFactory() {

            @Override
            public ReconnectStrategy createReconnectStrategy() {
                return new TimedReconnectStrategy(GlobalEventExecutor.INSTANCE, RECONNECT_STRATEGY_TIMEOUT, reconnectTime, 1.0, null, null, null);
            }
        };
    }

}
