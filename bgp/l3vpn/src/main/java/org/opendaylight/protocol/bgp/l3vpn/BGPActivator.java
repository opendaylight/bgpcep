/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.bgp.vpn.ipv4.rev160210.VpnIpv4SubsequentAddressFamily;

public final class BGPActivator extends AbstractBGPExtensionProviderActivator {

    private static final int VPN_IPV4_SAFI = 128;

    @Override
    protected List<AutoCloseable> startImpl(final BGPExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>(4);

        regs.add(context.registerSubsequentAddressFamily(VpnIpv4SubsequentAddressFamily.class, VPN_IPV4_SAFI));

        return regs;
    }

}
