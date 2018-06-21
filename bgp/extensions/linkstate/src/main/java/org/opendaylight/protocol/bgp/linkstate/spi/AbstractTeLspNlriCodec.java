/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate.spi;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.TeLspCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.TeLspCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.te.lsp._case.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.te.lsp._case.address.family.Ipv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.te.lsp._case.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.te.lsp._case.address.family.Ipv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.te.lsp._case.address.family.Ipv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.TunnelId;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;

public abstract class AbstractTeLspNlriCodec extends AbstractNlriTypeCodec {

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
    private static final YangInstanceIdentifier.NodeIdentifier IPV6_TUNNEL_SENDER_ADDRESS = new YangInstanceIdentifier
        .NodeIdentifier(QName.create(CLinkstateDestination.QNAME, "ipv6-tunnel-sender-address").intern());
    @VisibleForTesting
    private static final YangInstanceIdentifier.NodeIdentifier IPV6_TUNNEL_ENDPOINT_ADDRESS = new YangInstanceIdentifier
        .NodeIdentifier(QName.create(CLinkstateDestination.QNAME, "ipv6-tunnel-endpoint-address").intern());

    @VisibleForTesting
    public static final YangInstanceIdentifier.NodeIdentifier ADDRESS_FAMILY = new YangInstanceIdentifier.NodeIdentifier(AddressFamily.QNAME);

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
        if (ipv4Case) {
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
    protected final void serializeObjectType(final ObjectType objectType, final ByteBuf buffer) {
        Preconditions.checkArgument(objectType instanceof TeLspCase);
        final TeLspCase teLSP = (TeLspCase) objectType;
        final AddressFamily addressFamily = teLSP.getAddressFamily();
        if (addressFamily instanceof Ipv4Case) {
            serializeIpv4Case(teLSP, (Ipv4Case) addressFamily, buffer);
        } else {
            serializeIpv6Case(teLSP, (Ipv6Case) addressFamily, buffer);
        }
    }

    private static void serializeIpv4Case(final TeLspCase teLSP, final Ipv4Case ipv4Case, final ByteBuf buffer) {
        ByteBufWriteUtil.writeIpv4Address(ipv4Case.getIpv4TunnelSenderAddress(), buffer);
        serializeTunnelIdAndLspId(buffer, teLSP);
        ByteBufWriteUtil.writeIpv4Address(ipv4Case.getIpv4TunnelEndpointAddress(), buffer);
    }

    private static void serializeIpv6Case(final TeLspCase teLSP, final Ipv6Case ipv6Case, final ByteBuf buffer) {
        ByteBufWriteUtil.writeIpv6Address(ipv6Case.getIpv6TunnelSenderAddress(), buffer);
        serializeTunnelIdAndLspId(buffer, teLSP);
        ByteBufWriteUtil.writeIpv6Address(ipv6Case.getIpv6TunnelEndpointAddress(), buffer);
    }

    private static void serializeTunnelIdAndLspId(final ByteBuf buffer, final TeLspCase teLSP) {
        ByteBufWriteUtil.writeUnsignedShort(teLSP.getTunnelId().getValue(), buffer);
        ByteBufWriteUtil.writeUnsignedShort(teLSP.getLspId().getValue().intValue(), buffer);
    }
}
