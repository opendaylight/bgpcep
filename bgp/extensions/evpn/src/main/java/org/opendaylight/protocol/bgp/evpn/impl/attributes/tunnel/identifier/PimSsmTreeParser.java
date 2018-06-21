/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.TunnelIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.PimSsmTree;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.PimSsmTreeBuilder;

final class PimSsmTreeParser implements TunnelIdentifierSerializer, TunnelIdentifierParser {
    @Override
    public int serialize(final TunnelIdentifier tunnelIdentifier, final ByteBuf buffer) {
        Preconditions.checkArgument(tunnelIdentifier instanceof PimSsmTree,
                "The tunnelIdentifier %s is not PimSsmTree type.", tunnelIdentifier);
        PAddressPMulticastGroupUtil
                .serializeSenderPMulticastGroup(((PimSsmTree) tunnelIdentifier).getPimSsmTree(), buffer);
        return TunnelType.PIM_SSM_TREE.getIntValue();
    }

    @Override
    public TunnelIdentifier parse(final ByteBuf buffer) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi
                .tunnel.tunnel.identifier.pim.ssm.tree.PimSsmTreeBuilder pimSsmTree =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel
                        .pmsi.tunnel.tunnel.identifier.pim.ssm.tree.PimSsmTreeBuilder(PAddressPMulticastGroupUtil
                        .parseSenderPMulticastGroup(buffer));
        return new PimSsmTreeBuilder().setPimSsmTree(pimSsmTree.build()).build();
    }
}
