/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created as a util for test tools.
 */
public final class InetSocketAddressUtil {
    private static final String SEPARATOR = ",";
    private static final List<String> ASSIGNED_IPS = new ArrayList<>();
    private static final List<Integer> ASSIGNED_PORTS = new ArrayList<>();

    private InetSocketAddressUtil() {
        // Hidden on purpose
    }

    public static List<InetSocketAddress> parseAddresses(final String address, final int defaultPort) {
        return Arrays.asList(address.split(SEPARATOR)).stream()
                .map(input -> getInetSocketAddress(input, defaultPort)).collect(Collectors.toList());
    }

    public static List<InetSocketAddress> parseAddresses(final String address) {
        return Lists.transform(Arrays.asList(address.split(SEPARATOR)), input -> getInetSocketAddress(input, null));
    }

    public static HostAndPort toHostAndPort(final InetSocketAddress address) {
        return HostAndPort.fromParts(address.getHostString(), address.getPort());
    }

    public static InetSocketAddress getInetSocketAddress(final String hostPortString, final Integer defaultPort) {
        final HostAndPort hostAndPort = HostAndPort.fromString(hostPortString);
        if (defaultPort != null) {
            return new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPortOrDefault(defaultPort));
        }
        return new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPort());
    }

    public static synchronized InetSocketAddress getRandomLoopbackInetSocketAddress(final int port) {
        String newIp;
        do {
            newIp = getRandomLoopbackIpAddress();
        } while (ASSIGNED_IPS.contains(newIp));
        ASSIGNED_IPS.add(newIp);
        return new InetSocketAddress(getRandomLoopbackIpAddress(), port);
    }

    public static InetSocketAddress getRandomLoopbackInetSocketAddress() {
        return getRandomLoopbackInetSocketAddress(getRandomPort());
    }

    /**
     * Generate a random high range port number.
     *
     * @return A port number range from 20000 to 60000
     */
    public static synchronized int getRandomPort() {
        int port;
        do {
            port = 20000 + (int) Math.round(40000 * Math.random());
        } while (ASSIGNED_PORTS.contains(port));
        ASSIGNED_PORTS.add(port);
        return port;
    }

    /**
     * Generate a random loopback ip address.
     * IP address range: 127.50.50.50 ~ 127.250.250.250
     * We did not utilize the whole 127./8 range to avoid using common addresses like 127.0.0.1
     *
     * @return Generated random loopback IP address
     */
    public static String getRandomLoopbackIpAddress() {
        final StringBuilder sb = new StringBuilder("127");
        for (int i = 0; i < 3; i++) {
            sb.append(".").append(50 + (int) Math.round(Math.random() * 200));
        }
        return sb.toString();
    }
}
