/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.testtool;

import static org.opendaylight.protocol.util.InetSocketAddressUtil.getInetSocketAddress;
import static org.opendaylight.protocol.util.InetSocketAddressUtil.parseAndBuildAddressesForNPeers;

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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;

final class Arguments implements ArgumentsInput {
    private static final String PROGRAM_NAME = "BGP testing tool.";
    private static final String ARG_PREFIX = "--";
    private static final int DEFAULT_LOCAL_PORT = 0;
    private static final int DEFAULT_REMOTE_PORT = 12345;
    private static final InetAddress LOCALHOST = InetAddresses.forString("127.0.0.1");
    private static final InetSocketAddress REMOTE_ADDRESS = new InetSocketAddress(LOCALHOST, DEFAULT_REMOTE_PORT);
    private static final InetSocketAddress LOCAL_ADDRESS = new InetSocketAddress(LOCALHOST, DEFAULT_LOCAL_PORT);
    private static final int INITIAL_HOLD_TIME = 90;

    private static final ArgumentParser ARGUMENT_PARSER = initializeArgumentParser();
    private final Namespace parseArgs;

    Arguments(final Namespace parseArgs) {
        this.parseArgs = parseArgs;
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

        parser.addArgument("-as").type(new ArgumentType<AsNumber>() {
            @Override
            public AsNumber convert(final ArgumentParser argumentParser, final Argument argument, final String as) throws ArgumentParserException {
                return new AsNumber(Long.valueOf(as));
            }
        }).setDefault(new AsNumber(64496L)).help("Value of AS in the initial open message");
        parser.addArgument("-i", toArgName("initiate-connection")).type(Boolean.class).setDefault(false)
            .help("Active initialization of the connection, by default false");
        parser.addArgument("-ho", toArgName("holdtimer")).type(Integer.class).setDefault(INITIAL_HOLD_TIME)
            .help("In seconds, value of the desired holdtimer. According to RFC4271, recommended value for deadtimer is 90 seconds(set by default)");
        parser.addArgument("-pr", toArgName("prefixes")).type(Integer.class).setDefault(0)
            .help("Number of prefixes to be sent");
        parser.addArgument("-ec", toArgName("extended-communities")).type(new ArgumentType<List<String>>() {
            @Override
            public List<String> convert(final ArgumentParser argumentParser, final Argument argument, final String extComInput) throws ArgumentParserException {
                return Arrays.asList(extComInput.split(","));
            }
        }).setDefault(Collections.emptyList()).help("Extended communities to be send. Format: x,x,x where x is each extended community from " +
            "bgp-types.yang as as-4-generic-spec-extended-community, link-bandwidth-extended-community, ...");
        parser.addArgument("-ll", toArgName("log-level")).type(new ArgumentType<Level>() {
            @Override
            public Level convert(final ArgumentParser parser, final Argument arg, final String value) throws ArgumentParserException {
                return Level.toLevel(value);
            }
        }).setDefault(Level.INFO).help("log levels");
        parser.addArgument("-ra", toArgName("remoteAddress")).type(new ArgumentType<InetSocketAddress>() {
            @Override
            public InetSocketAddress convert(final ArgumentParser parser, final Argument arg, final String value) throws ArgumentParserException {
                return getInetSocketAddress(value, DEFAULT_LOCAL_PORT);
            }
        }).setDefault(LOCAL_ADDRESS).help("Ip address to which is this server bound. Format: x.x.x.x:y where y is port number. This IP address " +
            "will appear in BGP Open message as BGP Identifier of the server. ");
        parser.addArgument("-la", toArgName("localAddress")).type(new ArgumentType<List<InetSocketAddress>>() {
            @Override
            public List<InetSocketAddress> convert(final ArgumentParser parser, final Argument arg, final String input) throws ArgumentParserException {
                return parseAndBuildAddressesForNPeers(input, DEFAULT_REMOTE_PORT);
            }
        }).setDefault(REMOTE_ADDRESS).help("Format: x.x.x.x:y Or x.x.x.x:y,n where x is initial ip address, y is port number and n the number of peers.");
        parser.addArgument("-mp", toArgName("mpSupport")).type(Boolean.class).setDefault(false)
            .help("Active Multipart support");
        return parser;
    }


    private static String toArgName(final String dst) {
        return ARG_PREFIX + dst;
    }

    @Override
    public Level getLogLevel() {
        return this.parseArgs.get("log_level");
    }

    AsNumber getAs() {
        return this.parseArgs.get("as");
    }

    List<InetSocketAddress> getLocalAddresses() {
        return this.parseArgs.get("localAddress");
    }


    InetSocketAddress getRemoteAddress() {
        return this.parseArgs.get("remoteAddress");
    }

    int getNumberOfPrefixes() {
        return this.parseArgs.get("prefixes");
    }

    List<String> getExtendedCommunities() {
        return this.parseArgs.get("extended_communities");
    }

    int getHoldtimer() {
        return this.parseArgs.get("holdtimer");
    }

    boolean getInitiateConnection() {
        return this.parseArgs.get("initiate_connection");
    }

    boolean getMulipathSupport() {
        return this.parseArgs.get("mpSupport");
    }
}
