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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.PimSsmTree;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.PimSsmTreeBuilder;

public final class PimSsmTreeParser extends AbstractTunnelIdentifier<PimSsmTree> {
    @Override
    public int serialize(final PimSsmTree tunnelIdentifier, final ByteBuf buffer) {
        PAddressPMulticastGroupUtil
                .serializeSenderPMulticastGroup(tunnelIdentifier.getPimSsmTree(), buffer);
        return getType();
    }

    @Override
    public Class<? extends TunnelIdentifier> getClazz() {
        return PimSsmTree.class;
    }

    @Override
    public PimSsmTree parse(final ByteBuf buffer) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel.pmsi
                .tunnel.tunnel.identifier.pim.ssm.tree.PimSsmTreeBuilder pimSsmTree =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel
                        .pmsi.tunnel.tunnel.identifier.pim.ssm.tree.PimSsmTreeBuilder(PAddressPMulticastGroupUtil
                        .parseSenderPMulticastGroup(buffer));
        return new PimSsmTreeBuilder().setPimSsmTree(pimSsmTree.build()).build();
    }

    @Override
    public int getType() {
        return PmsiTunnelType.PimSsmTree.getIntValue();
    }
}
