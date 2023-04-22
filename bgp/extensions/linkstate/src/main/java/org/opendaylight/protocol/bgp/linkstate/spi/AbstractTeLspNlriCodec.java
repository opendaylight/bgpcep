/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.spi;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import io.netty.buffer.ByteBuf;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.TeLspCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.TeLspCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.te.lsp._case.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.te.lsp._case.address.family.Ipv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.te.lsp._case.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.te.lsp._case.address.family.Ipv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.te.lsp._case.address.family.Ipv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.TunnelId;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;

public abstract class AbstractTeLspNlriCodec extends AbstractNlriTypeCodec {

    @VisibleForTesting
    public static final NodeIdentifier LSP_ID = NodeIdentifier.create(
        QName.create(CLinkstateDestination.QNAME, "lsp-id").intern());
    @VisibleForTesting
    public static final NodeIdentifier TUNNEL_ID = NodeIdentifier.create(
        QName.create(CLinkstateDestination.QNAME, "tunnel-id").intern());
    @VisibleForTesting
    public static final NodeIdentifier IPV4_TUNNEL_SENDER_ADDRESS = NodeIdentifier.create(
        QName.create(CLinkstateDestination.QNAME, "ipv4-tunnel-sender-address").intern());
    @VisibleForTesting
    public static final NodeIdentifier IPV4_TUNNEL_ENDPOINT_ADDRESS = NodeIdentifier.create(
        QName.create(CLinkstateDestination.QNAME, "ipv4-tunnel-endpoint-address").intern());
    @VisibleForTesting
    private static final NodeIdentifier IPV6_TUNNEL_SENDER_ADDRESS = NodeIdentifier.create(
        QName.create(CLinkstateDestination.QNAME, "ipv6-tunnel-sender-address").intern());
    @VisibleForTesting
    private static final NodeIdentifier IPV6_TUNNEL_ENDPOINT_ADDRESS = NodeIdentifier.create(
        QName.create(CLinkstateDestination.QNAME, "ipv6-tunnel-endpoint-address").intern());

    @VisibleForTesting
    public static final NodeIdentifier ADDRESS_FAMILY = NodeIdentifier.create(AddressFamily.QNAME);

    @Deprecated(forRemoval = true)
    public static boolean isTeLsp(final ChoiceNode objectType) {
        return objectType.childByArg(ADDRESS_FAMILY) != null;
    }

    @Deprecated(forRemoval = true)
    public static TeLspCase serializeTeLsp(final ChoiceNode objectType) {
        return serializeObjectType(objectType, (ChoiceNode) objectType.getChildByArg(ADDRESS_FAMILY));
    }

    private static AddressFamily serializeAddressFamily(final ChoiceNode addressFamily, final boolean ipv4Case) {
        if (ipv4Case) {
            return new Ipv4CaseBuilder()
                .setIpv4TunnelSenderAddress(new Ipv4AddressNoZone(
                    (String) addressFamily.getChildByArg(IPV4_TUNNEL_SENDER_ADDRESS).body()))
                .setIpv4TunnelEndpointAddress(new Ipv4AddressNoZone(
                    (String) addressFamily.getChildByArg(IPV4_TUNNEL_ENDPOINT_ADDRESS).body()))
                .build();
        }

        return new Ipv6CaseBuilder()
            .setIpv6TunnelSenderAddress(new Ipv6AddressNoZone(
                (String) addressFamily.getChildByArg(IPV6_TUNNEL_SENDER_ADDRESS).body()))
            .setIpv6TunnelEndpointAddress(new Ipv6AddressNoZone(
                (String) addressFamily.getChildByArg(IPV6_TUNNEL_ENDPOINT_ADDRESS).body()))
            .build();
    }

    public static @Nullable TeLspCase serializeObjectType(final ChoiceNode objectType) {
        final var addressFamily = objectType.childByArg(ADDRESS_FAMILY);
        return addressFamily == null ? null : serializeObjectType(objectType, (ChoiceNode) addressFamily);
    }

    private static @NonNull TeLspCase serializeObjectType(final ChoiceNode objectType, final ChoiceNode addressFamily) {
        return new TeLspCaseBuilder()
            .setLspId(new LspId((Uint32) objectType.getChildByArg(LSP_ID).body()))
            .setTunnelId(new TunnelId((Uint16) objectType.getChildByArg(TUNNEL_ID).body()))
            .setAddressFamily(serializeAddressFamily(addressFamily,
                addressFamily.childByArg(IPV4_TUNNEL_SENDER_ADDRESS) != null))
            .build();
    }

    @Override
    protected final void serializeObjectType(final ObjectType objectType, final ByteBuf buffer) {
        checkArgument(objectType instanceof TeLspCase);
        final TeLspCase teLSP = (TeLspCase) objectType;
        final AddressFamily addressFamily = teLSP.getAddressFamily();
        if (addressFamily instanceof Ipv4Case) {
            serializeIpv4Case(teLSP, (Ipv4Case) addressFamily, buffer);
        } else {
            serializeIpv6Case(teLSP, (Ipv6Case) addressFamily, buffer);
        }
    }

    private static void serializeIpv4Case(final TeLspCase teLSP, final Ipv4Case ipv4Case, final ByteBuf buffer) {
        Ipv4Util.writeIpv4Address(ipv4Case.getIpv4TunnelSenderAddress(), buffer);
        serializeTunnelIdAndLspId(buffer, teLSP);
        Ipv4Util.writeIpv4Address(ipv4Case.getIpv4TunnelEndpointAddress(), buffer);
    }

    private static void serializeIpv6Case(final TeLspCase teLSP, final Ipv6Case ipv6Case, final ByteBuf buffer) {
        Ipv6Util.writeIpv6Address(ipv6Case.getIpv6TunnelSenderAddress(), buffer);
        serializeTunnelIdAndLspId(buffer, teLSP);
        Ipv6Util.writeIpv6Address(ipv6Case.getIpv6TunnelEndpointAddress(), buffer);
    }

    private static void serializeTunnelIdAndLspId(final ByteBuf buffer, final TeLspCase teLSP) {
        ByteBufUtils.write(buffer, teLSP.getTunnelId().getValue());
        buffer.writeShort(teLSP.getLspId().getValue().intValue());
    }
}
