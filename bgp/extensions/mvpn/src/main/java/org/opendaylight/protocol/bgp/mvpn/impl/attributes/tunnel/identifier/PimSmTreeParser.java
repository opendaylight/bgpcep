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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.PimSmTree;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.PimSmTreeBuilder;

public final class PimSmTreeParser extends AbstractTunnelIdentifier<PimSmTree> {
    @Override
    public int serialize(final PimSmTree tunnelIdentifier, final ByteBuf buffer) {
        PAddressPMulticastGroupUtil.serializeSenderPMulticastGroup(tunnelIdentifier
                .getPimSmTree(), buffer);
        return getType();
    }

    @Override
    public Class<? extends TunnelIdentifier> getClazz() {
        return PimSmTree.class;
    }

    @Override
    public PimSmTree parse(final ByteBuf buffer) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel.pmsi
                .tunnel.tunnel.identifier.pim.sm.tree.PimSmTreeBuilder pimSmTree =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel
                        .pmsi.tunnel.tunnel.identifier.pim.sm.tree.PimSmTreeBuilder(PAddressPMulticastGroupUtil
                        .parseSenderPMulticastGroup(buffer));
        return new PimSmTreeBuilder().setPimSmTree(pimSmTree.build()).build();
    }

    @Override
    public int getType() {
        return PmsiTunnelType.PimSmTree.getIntValue();
    }
}
