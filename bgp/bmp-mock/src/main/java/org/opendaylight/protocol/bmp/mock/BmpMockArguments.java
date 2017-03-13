/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.mock;

import static org.opendaylight.protocol.util.InetSocketAddressUtil.getInetSocketAddress;

import ch.qos.logback.classic.Level;
import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.opendaylight.protocol.util.ArgumentsInput;

public final class BmpMockArguments implements ArgumentsInput {

    private static final int DEFAULT_LOCAL_PORT = 0;
    private static final int DEFAULT_REMOTE_PORT = 12345;
    private static final InetAddress LOCALHOST = InetAddresses.forString("127.0.0.1");

    private static final InetSocketAddress REMOTE_ADDRESS = new InetSocketAddress(LOCALHOST, DEFAULT_REMOTE_PORT);
    private static final InetSocketAddress LOCAL_ADDRESS = new InetSocketAddress(LOCALHOST, DEFAULT_LOCAL_PORT);

    private static final String PROGRAM_NAME = "BGP Monitoring Protocol testing tool.";
    private static final String ARG_PREFIX = "--";
    private static final String ROUTERS_COUNT_DST = "routers_count";
    private static final String PEERS_COUNT_DST = "peers_count";
    private static final String PRE_POLICY_ROUTES_COUNT_DST = "pre_policy_routes";
    private static final String POST_POLICY_ROUTES_COUNT_DST = "post_policy_routes";
    private static final String LOCAL_ADDRESS_DST = "local_address";
    private static final String REMOTE_ADDRESS_DST = "remote_address";
    private static final String LOG_LEVEL_DST = "log_level";
    // when set to true, the mock will operate as a server listening for incoming active monitoring request
    private static final String PASSIVE_MODE_DST = "passive";

    private static final ArgumentParser ARGUMENT_PARSER = initializeArgumentParser();

    private final Namespace parseArgs;

    private BmpMockArguments(final Namespace parseArgs) {
        this.parseArgs = parseArgs;
    }

    public static BmpMockArguments parseArguments(final String[] args) {
        try {
            final Namespace namespace = ARGUMENT_PARSER.parseArgs(args);
            return new BmpMockArguments(namespace);
        } catch (final ArgumentParserException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public int getRoutersCount() {
        return this.parseArgs.getInt(ROUTERS_COUNT_DST);
    }

    public int getPeersCount() {
        return this.parseArgs.getInt(PEERS_COUNT_DST);
    }

    public int getPrePolicyRoutesCount() {
        return this.parseArgs.getInt(PRE_POLICY_ROUTES_COUNT_DST);
    }

    public int getPostPolicyRoutesCount() {
        return this.parseArgs.getInt(POST_POLICY_ROUTES_COUNT_DST);
    }

    public InetSocketAddress getLocalAddress() {
        return this.parseArgs.get(LOCAL_ADDRESS_DST);
    }

    public InetSocketAddress getRemoteAddress() {
        return this.parseArgs.get(REMOTE_ADDRESS_DST);
    }

    @Override
    public Level getLogLevel() {
        return this.parseArgs.get(LOG_LEVEL_DST);
    }

    public boolean isOnPassiveMode() {
        return this.parseArgs.get(PASSIVE_MODE_DST);
    }

    private static ArgumentParser initializeArgumentParser() {
        final ArgumentParser parser = ArgumentParsers.newArgumentParser(PROGRAM_NAME);
        parser.addArgument(toArgName(ROUTERS_COUNT_DST)).type(Integer.class).setDefault(1);
        parser.addArgument(toArgName(PEERS_COUNT_DST)).type(Integer.class).setDefault(0);
        parser.addArgument(toArgName(PRE_POLICY_ROUTES_COUNT_DST)).type(Integer.class).setDefault(0);
        parser.addArgument(toArgName(POST_POLICY_ROUTES_COUNT_DST)).type(Integer.class).setDefault(0);
        parser.addArgument(toArgName(PASSIVE_MODE_DST)).action(Arguments.storeTrue());
        parser.addArgument(toArgName(LOCAL_ADDRESS_DST)).type((parser13, arg, value) -> getInetSocketAddress(value, DEFAULT_LOCAL_PORT)).setDefault(LOCAL_ADDRESS);
        parser.addArgument(toArgName(REMOTE_ADDRESS_DST)).type((parser12, arg, value) -> getInetSocketAddress(value, DEFAULT_REMOTE_PORT)).setDefault(REMOTE_ADDRESS);
        parser.addArgument(toArgName(LOG_LEVEL_DST)).type((parser1, arg, value) -> Level.toLevel(value)).setDefault(Level.INFO);
        return parser;
    }

    private static String toArgName(final String dst) {
        return ARG_PREFIX + dst;
    }
}
