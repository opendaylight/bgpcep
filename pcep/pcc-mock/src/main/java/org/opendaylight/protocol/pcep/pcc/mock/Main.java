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
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.ietf.stateful07.PCEPStatefulCapability;
import org.opendaylight.protocol.util.InetSocketAddressUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static final int DEFAULT_REMOTE_PORT = 4189;
    private static final int DEFAULT_LOCAL_PORT = 0;
    private static final short DEFAULT_KEEP_ALIVE = 30;
    private static final short DEFAULT_DEAD_TIMER = 120;
    private static final InetAddress LOCALHOST = InetAddresses.forString("127.0.0.1");
    private static boolean triggeredInitSync = Boolean.FALSE;
    private static boolean includeDbv = Boolean.FALSE;
    private static boolean incrementalSync = Boolean.FALSE;
    private static boolean triggeredResync = Boolean.FALSE;
    private static BigInteger syncOptDBVersion;
    private static int reconnectAfterXSeconds;
    private static int disonnectAfterXSeconds;


    private Main() {
        throw new UnsupportedOperationException();
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
            switch (args[argIdx]) {
                case "--local-address":
                    localAddress = InetSocketAddressUtil.getInetSocketAddress(args[++argIdx], DEFAULT_LOCAL_PORT);
                    break;
                case "--remote-address":
                    remoteAddress = InetSocketAddressUtil.parseAddresses(args[++argIdx], DEFAULT_REMOTE_PORT);
                    break;
                case "--pcc":
                    pccCount = Integer.parseInt(args[++argIdx]);
                    break;
                case "--lsp":
                    lsps = Integer.parseInt(args[++argIdx]);
                    break;
                case "--pcerr":
                    pcError = true;
                    break;
                case "--log-level":
                    getRootLogger(lc).setLevel(Level.toLevel(args[++argIdx], Level.INFO));
                    break;
                case "--keepalive":
                case "-ka":
                    ka = Short.valueOf(args[++argIdx]);
                    break;
                case "--deadtimer":
                case "-d":
                    dt = Short.valueOf(args[++argIdx]);
                    break;
                case "--password":
                    password = args[++argIdx];
                    break;
                case "--reconnect":
                    reconnectTime = Integer.parseInt(args[++argIdx]);
                    break;
                case "--redelegation-timeout":
                    redelegationTimeout = Integer.parseInt(args[++argIdx]);
                    break;
                case "--state-timeout":
                    stateTimeout = Integer.parseInt(args[++argIdx]);
                    break;
                case "--state-sync-avoidance":
                    //"--state-sync-avoidance 10, 5, 10
                    includeDbv = Boolean.TRUE;
                    final Long dbVersionAfterReconnect = Long.valueOf(args[++argIdx]);
                    disonnectAfterXSeconds = Integer.parseInt(args[++argIdx]);
                    reconnectAfterXSeconds = Integer.parseInt(args[++argIdx]);
                    syncOptDBVersion = BigInteger.valueOf(dbVersionAfterReconnect);
                    break;
                case "--incremental-sync-procedure":
                    //TODO Check that DBv > Lsp always ??
                    includeDbv = Boolean.TRUE;
                    incrementalSync = Boolean.TRUE;
                    //Version of database to be used after restart
                    final Long initialDbVersionAfterReconnect = Long.valueOf(args[++argIdx]);
                    disonnectAfterXSeconds = Integer.parseInt(args[++argIdx]);
                    reconnectAfterXSeconds = Integer.parseInt(args[++argIdx]);
                    syncOptDBVersion = BigInteger.valueOf(initialDbVersionAfterReconnect);
                    break;
                case "--triggered-initial-sync":
                    triggeredInitSync = Boolean.TRUE;
                    break;
                case "--triggered-re-sync":
                    triggeredResync = Boolean.TRUE;
                    break;
                default:
                    LOG.warn("WARNING: Unrecognized argument: {}", args[argIdx]);
                    break;
            }
            argIdx++;
        }

        if (incrementalSync) {
            Preconditions.checkArgument(syncOptDBVersion.intValue() > lsps,
                    "Synchronization Database Version which will be used after " +
                "reconnectes requires to be higher than lsps");
        }

        final Optional<BigInteger> dBVersion = Optional.fromNullable(syncOptDBVersion);
        final PCCsBuilder pccs = new PCCsBuilder(lsps, pcError, pccCount, localAddress, remoteAddress, ka, dt,
                password, reconnectTime, redelegationTimeout,
            stateTimeout, getCapabilities());
        final TimerHandler timerHandler = new TimerHandler(pccs, dBVersion, disonnectAfterXSeconds,
                reconnectAfterXSeconds);
        pccs.createPCCs(BigInteger.valueOf(lsps), Optional.fromNullable(timerHandler));
        if (!triggeredInitSync) {
            timerHandler.createDisconnectTask();
        }
    }

    private static PCEPCapability getCapabilities() {
        if (triggeredInitSync) {
            Preconditions.checkArgument(includeDbv);
        }
        return new PCEPStatefulCapability(true, true, true, triggeredInitSync, triggeredResync,
                incrementalSync, includeDbv);
    }

    private static ch.qos.logback.classic.Logger getRootLogger(final LoggerContext lc) {
        return lc.getLoggerList().stream().filter(input -> (input != null) && input.getName()
            .equals(Logger.ROOT_LOGGER_NAME)).findFirst().get();
    }
}
