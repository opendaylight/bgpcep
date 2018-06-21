/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.TunnelIdentifier;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TunnelIdentifierHandler {
    static final int NO_TUNNEL_INFORMATION_PRESENT = 0;
    private static final Logger LOG = LoggerFactory.getLogger(TunnelIdentifierHandler.class);
    private static final String SKIP_SERIALIZATION = "Skipping serialization of PMSI Tunnel Attribute {}";
    private static final String SKIP_PARSE = "Skipping parsing of PMSI Tunnel Attribute type {}";
    private final HandlerRegistry<DataContainer, TunnelIdentifierParser, TunnelIdentifierSerializer> handlers =
            new HandlerRegistry<>();

    public TunnelIdentifierHandler(final AddressFamilyRegistry addressFamilyRegistry) {
        final RsvpTeP2MpLspParser rsvpTeP2MpLspParser = new RsvpTeP2MpLspParser();
        this.handlers.registerParser(TunnelType.RSVP_TE_P2MP_LSP.getIntValue(), rsvpTeP2MpLspParser);
        this.handlers.registerSerializer(TunnelType.RSVP_TE_P2MP_LSP.getClazz(), rsvpTeP2MpLspParser);

        final MldpP2mpLspParser mldpP2mpLspParser = new MldpP2mpLspParser(addressFamilyRegistry);
        this.handlers.registerParser(TunnelType.MLDP_P2MP_LSP.getIntValue(), mldpP2mpLspParser);
        this.handlers.registerSerializer(TunnelType.MLDP_P2MP_LSP.getClazz(), mldpP2mpLspParser);

        final PimSsmTreeParser pimSsmTreeParser = new PimSsmTreeParser();
        this.handlers.registerParser(TunnelType.PIM_SSM_TREE.getIntValue(), pimSsmTreeParser);
        this.handlers.registerSerializer(TunnelType.PIM_SSM_TREE.getClazz(), pimSsmTreeParser);

        final PimSmTreeParser pimSmTreeParser = new PimSmTreeParser();
        this.handlers.registerParser(TunnelType.PIM_SM_TREE.getIntValue(), pimSmTreeParser);
        this.handlers.registerSerializer(TunnelType.PIM_SM_TREE.getClazz(), pimSmTreeParser);

        final BidirPimTreeParser bidirPimTreeParser = new BidirPimTreeParser();
        this.handlers.registerParser(TunnelType.BIDIR_PIM_TREE.getIntValue(), bidirPimTreeParser);
        this.handlers.registerSerializer(TunnelType.BIDIR_PIM_TREE.getClazz(), bidirPimTreeParser);

        final IngressReplicationParser ingressReplicationParser = new IngressReplicationParser();
        this.handlers.registerParser(TunnelType.INGRESS_REPLICATION.getIntValue(), ingressReplicationParser);
        this.handlers.registerSerializer(TunnelType.INGRESS_REPLICATION.getClazz(), ingressReplicationParser);

        final MldpMp2mpLspParser mldpMp2mpLspParser = new MldpMp2mpLspParser();
        this.handlers.registerParser(TunnelType.M_LDP_MP_2_MP_LSP.getIntValue(), mldpMp2mpLspParser);
        this.handlers.registerSerializer(TunnelType.M_LDP_MP_2_MP_LSP.getClazz(), mldpMp2mpLspParser);
    }

    public TunnelIdentifier parse(final int tunnelType, final ByteBuf buffer) {
        final TunnelIdentifierParser parser = this.handlers.getParser(tunnelType);
        if (!buffer.isReadable() || parser == null) {
            LOG.debug(SKIP_PARSE, tunnelType);
            return null;
        }
        return parser.parse(buffer);
    }

    public int serialize(final TunnelIdentifier tunnel, final ByteBuf tunnelBuffer) {
        if (tunnel == null) {
            LOG.debug(SKIP_SERIALIZATION);
            return NO_TUNNEL_INFORMATION_PRESENT;
        }
        final TunnelIdentifierSerializer serializer = this.handlers.getSerializer(tunnel.getImplementedInterface());
        if (serializer == null) {
            LOG.debug(SKIP_SERIALIZATION, tunnel);
            return NO_TUNNEL_INFORMATION_PRESENT;
        }
        return serializer.serialize(tunnel, tunnelBuffer);
    }
}
