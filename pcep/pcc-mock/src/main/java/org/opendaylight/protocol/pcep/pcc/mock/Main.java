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
import com.google.common.net.HostAndPort;
import com.google.common.net.InetAddresses;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static final int DEFAULT_REMOTE_PORT = 4189;
    private static final int DEFAULT_LOCAL_PORT = 0;
    private static final short DEFAULT_KEEP_ALIVE = 30;
    private static final short DEFAULT_DEAD_TIMER = 120;
    private static final InetAddress LOCALHOST = InetAddresses.forString("127.0.0.1");
    private static StateSyncOpt STATE_SYNC_OPT = StateSyncOpt.Inactive;
    private static BigInteger SYNC_OPT_DB_VERSION;
    private static Integer reconnectAfter;
    private static Integer disconnectAfter;

    private Main() {
    }

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
        long reconnectTime = -1;
        int redelegationTimeout = 0;
        int stateTimeout = -1;

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
                reconnectTime = TimeUnit.SECONDS.toMillis(Integer.valueOf(args[++argIdx]).intValue());
            } else if (args[argIdx].equals("--redelegation-timeout")) {
                redelegationTimeout = Integer.valueOf(args[++argIdx]);
            } else if (args[argIdx].equals("--state-timeout")) {
                stateTimeout = Integer.valueOf(args[++argIdx]);
            } else if (args[argIdx].equals("--state-sync-avoidance")) {
                //"--state-sync-avoidance 10, 5, 10
                STATE_SYNC_OPT = StateSyncOpt.AvoidanceProcedure;
                final Long dbVersionAfterReconnect = Long.valueOf(args[++argIdx]); // Can be an static like 10? its just start with higher dbV
                disconnectAfter = Integer.valueOf(args[++argIdx]);
                reconnectAfter = Integer.valueOf(args[++argIdx]);
                SYNC_OPT_DB_VERSION = BigInteger.valueOf(dbVersionAfterReconnect);
            } else {
                LOG.warn("WARNING: Unrecognized argument: {}", args[argIdx]);
            }
            argIdx++;
        }
        final PCCsBuilder pccs = new PCCsBuilder(lsps, pcError, pccCount, localAddress, remoteAddress, ka, dt, password, reconnectTime, redelegationTimeout,
            stateTimeout, SYNC_OPT_DB_VERSION, STATE_SYNC_OPT, disconnectAfter, reconnectAfter);
        pccs.createPCCs();

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

    private static ch.qos.logback.classic.Logger getRootLogger(final LoggerContext lc) {
        return Iterables.find(lc.getLoggerList(), new Predicate<Logger>() {
            @Override
            public boolean apply(final Logger input) {
                return (input != null) ? input.getName().equals(Logger.ROOT_LOGGER_NAME) : false;
            }
        });
    }

}
