/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv4Address;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv6Address;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeShort;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedShort;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.TeLspCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.TeLspCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.te.lsp._case.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.te.lsp._case.address.family.Ipv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.te.lsp._case.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.te.lsp._case.address.family.Ipv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.te.lsp._case.address.family.Ipv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.TunnelId;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;

@VisibleForTesting
public final class TeLspNlriSerializer implements NlriTypeCaseSerializer {

    @VisibleForTesting
    public static final YangInstanceIdentifier.NodeIdentifier LSP_ID = new YangInstanceIdentifier.NodeIdentifier(
        QName.create(CLinkstateDestination.QNAME, "lsp-id").intern());
    @VisibleForTesting
    public static final YangInstanceIdentifier.NodeIdentifier TUNNEL_ID = new YangInstanceIdentifier.NodeIdentifier(
        QName.create(CLinkstateDestination.QNAME, "tunnel-id").intern());
    @VisibleForTesting
    public static final YangInstanceIdentifier.NodeIdentifier IPV4_TUNNEL_SENDER_ADDRESS = new YangInstanceIdentifier.NodeIdentifier(
        QName.create(CLinkstateDestination.QNAME, "ipv4-tunnel-sender-address").intern());
    @VisibleForTesting
    public static final YangInstanceIdentifier.NodeIdentifier IPV4_TUNNEL_ENDPOINT_ADDRESS = new YangInstanceIdentifier
        .NodeIdentifier(QName.create(CLinkstateDestination.QNAME, "ipv4-tunnel-endpoint-address").intern());
    @VisibleForTesting
    public static final YangInstanceIdentifier.NodeIdentifier IPV6_TUNNEL_SENDER_ADDRESS = new YangInstanceIdentifier
        .NodeIdentifier( QName.create(CLinkstateDestination.QNAME, "ipv6-tunnel-sender-address").intern());
    @VisibleForTesting
    public static final YangInstanceIdentifier.NodeIdentifier IPV6_TUNNEL_ENDPOINT_ADDRESS = new YangInstanceIdentifier
        .NodeIdentifier(QName.create(CLinkstateDestination.QNAME, "ipv6-tunnel-endpoint-address").intern());

    @VisibleForTesting
    public static final YangInstanceIdentifier.NodeIdentifier ADDRESS_FAMILY = new YangInstanceIdentifier.NodeIdentifier(AddressFamily.QNAME);

    public static NlriType serializeIpvTSA(final AddressFamily addressFamily, final ByteBuf body) {
        if (addressFamily.equals(Ipv6Case.class)) {
            final Ipv6Address ipv6 = ((Ipv6Case) addressFamily).getIpv6TunnelSenderAddress();
            Preconditions.checkArgument(ipv6 != null, "Ipv6TunnelSenderAddress is mandatory.");
            writeIpv6Address(ipv6, body);
            return NlriType.Ipv6TeLsp;
        }

        final Ipv4Address ipv4 = ((Ipv4Case) addressFamily).getIpv4TunnelSenderAddress();
        Preconditions.checkArgument(ipv4 != null, "Ipv4TunnelSenderAddress is mandatory.");
        writeIpv4Address(ipv4, body);

        return NlriType.Ipv4TeLsp;
    }

    public static void serializeTunnelID(final TunnelId tunnelID, final ByteBuf body) {
        Preconditions.checkArgument(tunnelID != null, "TunnelId is mandatory.");
        writeUnsignedShort(tunnelID.getValue(), body);
    }

    public static void serializeLspID(final LspId lspId, final ByteBuf body) {
        Preconditions.checkArgument(lspId != null, "LspId is mandatory.");
        writeShort(lspId.getValue().shortValue(), body);
    }

    public static void serializeTEA(final AddressFamily addressFamily, final ByteBuf body) {
        if (addressFamily.equals(Ipv6Case.class)) {
            final Ipv6Address ipv6 = ((Ipv6Case) addressFamily).getIpv6TunnelEndpointAddress();
            Preconditions.checkArgument(ipv6 != null, "Ipv6TunnelEndpointAddress is mandatory.");
            writeIpv6Address(ipv6, body);
            return;
        }

        final Ipv4Address ipv4 = ((Ipv4Case) addressFamily).getIpv4TunnelEndpointAddress();
        Preconditions.checkArgument(ipv4 != null, "Ipv4TunnelEndpointAddress is mandatory.");
        Preconditions.checkArgument(ipv4 != null, "Ipv4TunnelEndpointAddress is mandatory.");
        writeIpv4Address(ipv4, body);
    }

    public static boolean isTeLsp(final ChoiceNode objectType) {
        return objectType.getChild(ADDRESS_FAMILY).isPresent();
    }

    public static TeLspCase serializeTeLsp(final ChoiceNode objectType) {
        final TeLspCaseBuilder teLsp = new TeLspCaseBuilder();
        teLsp.setLspId(new LspId((Long) objectType.getChild(LSP_ID).get().getValue()));
        teLsp.setTunnelId(new TunnelId((Integer) objectType.getChild(TUNNEL_ID).get().getValue()));
        final ChoiceNode addressFamily = (ChoiceNode) objectType.getChild(ADDRESS_FAMILY).get();
        teLsp.setAddressFamily(serializeAddressFamily(addressFamily, addressFamily.getChild(IPV4_TUNNEL_SENDER_ADDRESS).isPresent()));

        return teLsp.build();
    }

    private static AddressFamily serializeAddressFamily(final ChoiceNode addressFamily, final boolean ipv4Case) {
        if(ipv4Case) {
            return new Ipv4CaseBuilder()
                .setIpv4TunnelSenderAddress(new Ipv4Address((String) addressFamily.getChild(IPV4_TUNNEL_SENDER_ADDRESS).get().getValue()))
                .setIpv4TunnelEndpointAddress(new Ipv4Address((String) addressFamily.getChild(IPV4_TUNNEL_ENDPOINT_ADDRESS).get().getValue()))
                .build();
        }

        return new Ipv6CaseBuilder()
            .setIpv6TunnelSenderAddress(new Ipv6Address((String) addressFamily.getChild(IPV6_TUNNEL_SENDER_ADDRESS).get().getValue()))
            .setIpv6TunnelEndpointAddress(new Ipv6Address((String) addressFamily.getChild(IPV6_TUNNEL_ENDPOINT_ADDRESS).get().getValue()))
            .build();
    }

    @Override
    public NlriType serializeTypeNlri(final CLinkstateDestination destination, final ByteBuf localdescs, final ByteBuf byteAggregator) {
        final TeLspCase teLSP = ((TeLspCase) destination.getObjectType());
        final AddressFamily afi = teLSP.getAddressFamily();
        NlriType nlriType = serializeIpvTSA(afi, localdescs);
        serializeTunnelID(teLSP.getTunnelId(), localdescs);
        serializeLspID(teLSP.getLspId(), localdescs);
        serializeTEA(afi, localdescs);
        return nlriType;
    }

}
