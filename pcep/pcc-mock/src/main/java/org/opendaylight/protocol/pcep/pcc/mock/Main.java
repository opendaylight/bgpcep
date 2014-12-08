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
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.ietf.stateful07.StatefulActivator;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPHandlerFactory;
import org.opendaylight.protocol.pcep.impl.PCEPSessionImpl;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static final int DEFAULT_PORT = 4189;
    private static final short DEFAULT_KEEP_ALIVE = 30;
    private static final short DEFAULT_DEAD_TIMER = 120;
    private static final int RECONNECT_STRATEGY_TIMEOUT = 2000;

    public static void main(String[] args) throws InterruptedException, ExecutionException, UnknownHostException {
        InetAddress localAddress = InetAddress.getByName("127.0.0.1");
        List<InetAddress> remoteAddress = Lists.newArrayList(InetAddress.getByName("127.0.0.1"));
        int pccCount = 1;
        int lsps = 1;
        boolean pcError = false;
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        short ka = DEFAULT_KEEP_ALIVE;
        short dt = DEFAULT_DEAD_TIMER;

        getRootLogger(lc).setLevel(ch.qos.logback.classic.Level.INFO);
        int argIdx = 0;
        while (argIdx < args.length) {
            if (args[argIdx].equals("--local-address")) {
                localAddress = InetAddress.getByName(args[++argIdx]);
            } else if (args[argIdx].equals("--remote-address")) {
                remoteAddress = parseAddresses(args[++argIdx]);
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
            } else {
                LOG.warn("WARNING: Unrecognized argument: {}", args[argIdx]);
            }
            argIdx++;
        }
        createPCCs(lsps, pcError, pccCount, localAddress, remoteAddress, ka, dt);
    }

    public static void createPCCs(final int lspsPerPcc, final boolean pcerr, final int pccCount,
            final InetAddress localAddress, final List<InetAddress> remoteAddress, final short keepalive, final short deadtimer) throws InterruptedException, ExecutionException {
        final SessionNegotiatorFactory<Message, PCEPSessionImpl, PCEPSessionListener> snf = new DefaultPCEPSessionNegotiatorFactory(
                new OpenBuilder().setKeepalive(keepalive).setDeadTimer(deadtimer).setSessionId((short) 0).build(), 0);

        final StatefulActivator activator07 = new StatefulActivator();
        activator07.start(ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance());
        final PCCMock<Message, PCEPSessionImpl, PCEPSessionListener> pcc = new PCCMock<>(snf, new PCEPHandlerFactory(
                ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance().getMessageHandlerRegistry()),
                new DefaultPromise<PCEPSessionImpl>(GlobalEventExecutor.INSTANCE));

        for (final InetAddress pceAddress : remoteAddress) {
            InetAddress currentAddress = localAddress;
            int i = 0;
            while (i < pccCount) {
                final InetAddress pccAddress = currentAddress;
                pcc.createClient(new InetSocketAddress(pccAddress, 0), new InetSocketAddress(pceAddress, DEFAULT_PORT),
                        new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, RECONNECT_STRATEGY_TIMEOUT),
                        new SessionListenerFactory<PCEPSessionListener>() {

                            @Override
                            public PCEPSessionListener getSessionListener() {
                                return new SimpleSessionListener(lspsPerPcc, pcerr, pccAddress);
                            }
                        }).get();
                i++;
                currentAddress = InetAddresses.increment(currentAddress);
            }
        }
    }

    private static ch.qos.logback.classic.Logger getRootLogger(final LoggerContext lc) {
        return Iterables.find(lc.getLoggerList(), new Predicate<Logger>() {
            @Override
            public boolean apply(Logger input) {
                return input.getName().equals(Logger.ROOT_LOGGER_NAME);
            }
        });
    }

    private static List<InetAddress> parseAddresses(final String address) {
        return Lists.transform(Arrays.asList(address.split(",")), new Function<String, InetAddress>() {
            @Override
            public InetAddress apply(String input) {
                try {
                    return InetAddress.getByName(input);
                } catch (UnknownHostException e) {
                    throw new RuntimeException("Not an IP address: " + input, e);
                }
            }
        });
    }

}
