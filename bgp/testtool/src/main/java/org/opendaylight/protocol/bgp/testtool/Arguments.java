/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.testtool;

import static org.opendaylight.protocol.util.InetSocketAddressUtil.getInetSocketAddress;

import ch.qos.logback.classic.Level;
import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.ArgumentType;
import net.sourceforge.argparse4j.inf.Namespace;
import org.opendaylight.protocol.util.ArgumentsInput;
import org.opendaylight.protocol.util.InetSocketAddressUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;

final class Arguments implements ArgumentsInput {
    private static final String PROGRAM_NAME = "BGP testing tool.";
    private static final String ARG_PREFIX = "--";
    private static final int DEFAULT_LOCAL_PORT = 0;
    private static final int DEFAULT_REMOTE_PORT = 1790;
    private static final InetAddress LOCALHOST = InetAddresses.forString("127.0.0.1");
    private static final InetSocketAddress REMOTE_ADDRESS = new InetSocketAddress(LOCALHOST, DEFAULT_REMOTE_PORT);
    private static final InetSocketAddress LOCAL_ADDRESS = new InetSocketAddress(LOCALHOST, DEFAULT_LOCAL_PORT);
    private static final int INITIAL_HOLD_TIME = 90;
    private static final String LOG_LEVEL = "log_level";
    private static final String REMOTE_ADDRESS_PARAMETER = "remoteAddress";
    private static final String REMOTE_ADDRESS_PARAMETER_HELP = "IP address of remote BGP peer, which the tool"
            + " can accept or initiate connect to that address (based on the mode)";
    private static final String LOCAL_ADDRESS_PARAMETER = "localAddress";
    private static final String LOCAL_ADDRESS_PARAMETER_HELP = "IP address of BGP speakers which the tools simulates";
    private static final String EXTENDED_COMMUNITIES_PARAMETER = "extended_communities";
    private static final String EXTENDED_COMMUNITIES_PARAMETER_HELP = "Extended communities to be send. "
            + "Format: x,x,x where x is each extended community from bgp-types.yang as "
            + "as-4-generic-spec-extended-community, link-bandwidth-extended-community, ...";
    private static final String ACTIVE_CONNECTION_PARAMETER = "active";
    private static final String ACTIVE_CONNECTION_HELP = "Active initialization of the connection, by default false";
    private static final String HOLD_TIMER_PARAMETER = "holdtimer";
    private static final String INITIAL_HOLD_TIME_HELP = "In seconds, value of the desired holdtimer."
            + " According to RFC4271, recommended value for deadtimer is 90 seconds(set by default)";
    private static final String PREFIXES_PARAMETER = "prefixes";
    private static final String PREFIXES_PARAMETER_HELP = "Number of prefixes to be sent";
    private static final String MULTIPATH_PARAMETER = "multiPathSupport";
    private static final String MULTIPATH_PARAMETER_HELP = "Active Multipart support";
    private static final String AS_PARAMETER = "as";
    private static final String AS_PARAMETER_HELP = "Value of AS in the initial open message";
    private static final String SPEAKERS_COUNT = "speakersCount";
    private static final String SPEAKERS_COUNT_HELP = "Number of simulated BGP speakers, when creating each speaker,"
            + " use incremented local-address for binding";
    private static final ArgumentParser ARGUMENT_PARSER = initializeArgumentParser();
    private final Namespace parseArgs;

    private Arguments(final Namespace parseArgs) {
        this.parseArgs = parseArgs;
    }

    private interface ArgumentTypeTool<T> extends ArgumentType<T> {
        default T convert(ArgumentParser var1, Argument var2, String input) throws ArgumentParserException {
            return convert(input);
        }

        T convert(String input) throws ArgumentParserException;
    }

    static Arguments parseArguments(final String[] args) {
        try {
            final Namespace namespace = ARGUMENT_PARSER.parseArgs(args);
            return new Arguments(namespace);
        } catch (final ArgumentParserException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static ArgumentParser initializeArgumentParser() {
        final ArgumentParser parser = ArgumentParsers.newArgumentParser(PROGRAM_NAME);

        parser.addArgument("-i", toArgName(ACTIVE_CONNECTION_PARAMETER)).type(Boolean.class)
                .setDefault(false).help(ACTIVE_CONNECTION_HELP);
        parser.addArgument("-ho", toArgName(HOLD_TIMER_PARAMETER)).type(Integer.class)
                .setDefault(INITIAL_HOLD_TIME).help(INITIAL_HOLD_TIME_HELP);
        parser.addArgument("-pr", toArgName(PREFIXES_PARAMETER)).type(Integer.class)
                .setDefault(0).help(PREFIXES_PARAMETER_HELP);
        parser.addArgument("-sc", toArgName(SPEAKERS_COUNT)).type(Integer.class)
                .setDefault(0).help(SPEAKERS_COUNT_HELP);
        parser.addArgument("-mp", toArgName(MULTIPATH_PARAMETER)).type(Boolean.class)
                .setDefault(false).help(MULTIPATH_PARAMETER_HELP);
        parser.addArgument("-" + AS_PARAMETER, toArgName(AS_PARAMETER))
                .type((ArgumentTypeTool<AsNumber>) as -> new AsNumber(Long.valueOf(as)))
                .setDefault(new AsNumber(64496L)).help(AS_PARAMETER_HELP);
        parser.addArgument("-ec", toArgName(EXTENDED_COMMUNITIES_PARAMETER))
                .type((ArgumentTypeTool<List<String>>) extComInput ->
                        Arrays.asList(extComInput.split(","))).setDefault(Collections.emptyList())
                .help(EXTENDED_COMMUNITIES_PARAMETER_HELP);
        parser.addArgument("-ll", toArgName(LOG_LEVEL))
                .type((ArgumentTypeTool<Level>) Level::toLevel).setDefault(Level.INFO).help("log levels");
        parser.addArgument("-ra", toArgName(REMOTE_ADDRESS_PARAMETER))
                .type((ArgumentTypeTool<List<InetSocketAddress>>) input ->
                        InetSocketAddressUtil.parseAddresses(input, DEFAULT_REMOTE_PORT))
                .setDefault(Collections.singletonList(REMOTE_ADDRESS))
                .help(REMOTE_ADDRESS_PARAMETER_HELP);
        parser.addArgument("-la", toArgName(LOCAL_ADDRESS_PARAMETER))
                .type((ArgumentTypeTool<InetSocketAddress>) input ->
                        getInetSocketAddress(input, DEFAULT_LOCAL_PORT))
                .setDefault(LOCAL_ADDRESS).help(LOCAL_ADDRESS_PARAMETER_HELP);
        return parser;
    }


    private static String toArgName(final String dst) {
        return ARG_PREFIX + dst;
    }

    @Override
    public Level getLogLevel() {
        return this.parseArgs.get(LOG_LEVEL);
    }

    AsNumber getAs() {
        return this.parseArgs.get("as");
    }

    InetSocketAddress getLocalAddresses() {
        return this.parseArgs.get(LOCAL_ADDRESS_PARAMETER);
    }

    List<InetSocketAddress> getRemoteAddresses() {
        return this.parseArgs.get(REMOTE_ADDRESS_PARAMETER);
    }

    int getNumberOfPrefixes() {
        return this.parseArgs.get(PREFIXES_PARAMETER);
    }

    List<String> getExtendedCommunities() {
        return this.parseArgs.get(EXTENDED_COMMUNITIES_PARAMETER);
    }

    int getHoldTimer() {
        return this.parseArgs.get(HOLD_TIMER_PARAMETER);
    }

    boolean getInitiateConnection() {
        return this.parseArgs.get(ACTIVE_CONNECTION_PARAMETER);
    }

    boolean getMultiPathSupport() {
        return this.parseArgs.get(MULTIPATH_PARAMETER);
    }

    int getSpeakerCount() {
        return this.parseArgs.get(SPEAKERS_COUNT);
    }
}
