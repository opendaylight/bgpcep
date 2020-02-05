/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.mvpn.spi.attributes.tunnel.identifier.AbstractTunnelIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.PmsiTunnelType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel.pmsi.tunnel.TunnelIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.BidirPimTree;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.BidirPimTreeBuilder;

public final class BidirPimTreeParser extends AbstractTunnelIdentifier<BidirPimTree> {
    public int getType() {
        return PmsiTunnelType.BidirPimTree.getIntValue();
    }

    @Override
    public int serialize(final BidirPimTree tunnelIdentifier, final ByteBuf buffer) {
        PAddressPMulticastGroupUtil.serializeSenderPMulticastGroup(tunnelIdentifier.getBidirPimTree(), buffer);
        return PmsiTunnelType.BidirPimTree.getIntValue();
    }

    @Override
    public Class<? extends TunnelIdentifier> getClazz() {
        return BidirPimTree.class;
    }

    @Override
    public BidirPimTree parse(final ByteBuf buffer) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel.pmsi
                .tunnel.tunnel.identifier.bidir.pim.tree.BidirPimTreeBuilder bidirPimTree =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel
                        .pmsi.tunnel.tunnel.identifier.bidir.pim.tree.BidirPimTreeBuilder(PAddressPMulticastGroupUtil
                        .parseSenderPMulticastGroup(buffer));
        return new BidirPimTreeBuilder().setBidirPimTree(bidirPimTree.build()).build();
    }
}
