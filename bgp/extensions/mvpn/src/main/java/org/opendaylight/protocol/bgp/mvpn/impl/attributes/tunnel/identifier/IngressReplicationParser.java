/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier;

import static org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier.PAddressPMulticastGroupUtil.parseIpAddress;
import static org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier.PAddressPMulticastGroupUtil.serializeIpAddress;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.mvpn.spi.attributes.tunnel.identifier.AbstractTunnelIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.PmsiTunnelType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel.pmsi.tunnel.TunnelIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.IngressReplication;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.IngressReplicationBuilder;

public final class IngressReplicationParser extends AbstractTunnelIdentifier<IngressReplication> {
    @Override
    public int serialize(final IngressReplication tunnelIdentifier, final ByteBuf buffer) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel.pmsi
                .tunnel.tunnel.identifier.ingress.replication.IngressReplication ingressReplication =
                tunnelIdentifier.getIngressReplication();
        serializeIpAddress(ingressReplication.getReceivingEndpointAddress(), buffer);
        return getType();
    }

    @Override
    public Class<? extends TunnelIdentifier> getClazz() {
        return IngressReplication.class;
    }

    @Override
    public IngressReplication parse(final ByteBuf buffer) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel.pmsi
                .tunnel.tunnel.identifier.ingress.replication.IngressReplication builder =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel
                        .pmsi.tunnel.tunnel.identifier.ingress.replication.IngressReplicationBuilder()
                        .setReceivingEndpointAddress(parseIpAddress(buffer.readableBytes(), buffer)).build();
        return new IngressReplicationBuilder().setIngressReplication(builder).build();
    }

    @Override
    public int getType() {
        return PmsiTunnelType.IngressReplication.getIntValue();
    }
}
