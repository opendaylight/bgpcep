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
import java.util.Arrays;
import java.util.List;

/**
 * Created as a util for test tools
 */
public final class InetSocketAddressUtil {
    private static final String SEPARATOR = ",";

    private InetSocketAddressUtil() {
        throw new UnsupportedOperationException();
    }

    public static List<InetSocketAddress> parseAddresses(final String address, final int defaultPort) {
        return Lists.transform(Arrays.asList(address.split(SEPARATOR)), input -> getInetSocketAddress(input, defaultPort));
    }

    public static List<InetSocketAddress> parseAddresses(final String address) {
        return Lists.transform(Arrays.asList(address.split(SEPARATOR)), input -> getInetSocketAddress(input, null));
    }

    public static InetSocketAddress getInetSocketAddress(final String hostPortString, final Integer defaultPort) {
        final HostAndPort hostAndPort = HostAndPort.fromString(hostPortString);
        if (defaultPort != null) {
            return new InetSocketAddress(hostAndPort.getHostText(), hostAndPort.getPortOrDefault(defaultPort));
        }
        return new InetSocketAddress(hostAndPort.getHostText(), hostAndPort.getPort());
    }
}
