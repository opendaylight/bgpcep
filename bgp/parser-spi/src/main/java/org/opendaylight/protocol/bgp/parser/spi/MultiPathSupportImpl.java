/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.spi;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamilies;

public final class MultiPathSupportImpl implements MultiPathSupport {

    private final Set<BgpTableType> supportedTables;

    public static MultiPathSupport createReceiveMultiPathSupport(final List<AddressFamilies> addPathCapabilities) {
        final Set<BgpTableType> support = addPathCapabilities
            .stream()
            .filter(e -> e.getSendReceive() == SendReceive.Both || e.getSendReceive() == SendReceive.Receive)
            .map(e -> new BgpTableTypeImpl(e.getAfi(), e.getSafi()))
            .collect(Collectors.toSet());
        return new MultiPathSupportImpl(support);
    }

    private MultiPathSupportImpl(final Set<BgpTableType> supportedTables) {
        this.supportedTables = supportedTables;
    }

    @Override
    public boolean isTableTypeSupported(final BgpTableType tableType) {
        return this.supportedTables.contains(tableType);
    }

}
