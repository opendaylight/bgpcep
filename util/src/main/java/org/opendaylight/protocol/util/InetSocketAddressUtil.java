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
import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created as a util for test tools
 */
public final class InetSocketAddressUtil {

    private InetSocketAddressUtil() {
        throw new UnsupportedOperationException();
    }

    public static List<InetSocketAddress> parseAddresses(final String address, final int defaultPort) {
        return Lists.transform(Arrays.asList(address.split(",")), input -> getInetSocketAddress(input, defaultPort));
    }

    public static List<InetSocketAddress> parseAddresses(final String address) {
        return Lists.transform(Arrays.asList(address.split(",")), input -> getInetSocketAddress(input, null));
    }

    public static InetSocketAddress getInetSocketAddress(final String hostPortString, final Integer defaultPort) {
        final HostAndPort hostAndPort = HostAndPort.fromString(hostPortString);
        if (defaultPort != null) {
            return new InetSocketAddress(hostAndPort.getHostText(), hostAndPort.getPortOrDefault(defaultPort));
        }
        return new InetSocketAddress(hostAndPort.getHostText(), hostAndPort.getPort());
    }

    /**
     * Build N addresses
     *
     * @param input Format: x.x.x.x:y Or x.x.x.x:y,n where x is initial ip address, y is port number and n the number of peers.
     * @param defaultPort default Port
     * @return N Addresses
     */
    public static List<InetSocketAddress> parseAndBuildAddressesForNPeers(final String input, final int defaultPort) {
        final String[] localAddressInput = input.split(",");
        final List<InetSocketAddress> addressesList = new ArrayList<>();
        final InetSocketAddress mandatoryAddress = getInetSocketAddress(localAddressInput[0], defaultPort);
        addressesList.add(mandatoryAddress);
        InetAddress address = mandatoryAddress.getAddress();
        final int port = mandatoryAddress.getPort();
        if (localAddressInput.length > 1) {
            for (int i = 1; i < Integer.valueOf(localAddressInput[1]); i++) {
                address = InetAddresses.increment(address);
                addressesList.add(new InetSocketAddress(address, port));
            }
        }
        return addressesList;
    }
}
