/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier;

import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PAddressPMulticastGroupUtil.parseIpAddress;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PAddressPMulticastGroupUtil.serializeIpAddress;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.TunnelIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.IngressReplication;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.IngressReplicationBuilder;

final class IngressReplicationParser implements TunnelIdentifierSerializer, TunnelIdentifierParser {
    @Override
    public int serialize(final TunnelIdentifier tunnelIdentifier, final ByteBuf buffer) {
        Preconditions.checkArgument(tunnelIdentifier instanceof IngressReplication,
                "The tunnelIdentifier %s is not IngressReplication type.", tunnelIdentifier);
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi
                .tunnel.tunnel.identifier.ingress.replication.IngressReplication ingressReplication =
                ((IngressReplication) tunnelIdentifier).getIngressReplication();
        serializeIpAddress(ingressReplication.getReceivingEndpointAddress(), buffer);
        return TunnelType.INGRESS_REPLICATION.getIntValue();
    }

    @Override
    public TunnelIdentifier parse(final ByteBuf buffer) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi
                .tunnel.tunnel.identifier.ingress.replication.IngressReplication builder =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel
                        .pmsi.tunnel.tunnel.identifier.ingress.replication.IngressReplicationBuilder()
                        .setReceivingEndpointAddress(parseIpAddress(buffer.readableBytes(), buffer)).build();
        return new IngressReplicationBuilder().setIngressReplication(builder).build();
    }
}
