/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.attributes;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.TunnelIdentifierHandler;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.util.MplsLabelUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.routes.evpn.routes.evpn.route.PmsiTunnelAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.routes.evpn.routes.evpn.route.PmsiTunnelAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.PmsiTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.PmsiTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.TunnelIdentifier;
import org.opendaylight.yangtools.yang.binding.DataObject;

public final class PMSITunnelAttributeHandler implements AttributeParser, AttributeSerializer {
    private static final int PMSI_ATTRIBUTE = 22;
    private static final int MPLS_LENGTH = 3;
    private final TunnelIdentifierHandler tunnelIdentifierHandler;

    public PMSITunnelAttributeHandler(final AddressFamilyRegistry addressFamilyRegistry) {
        this.tunnelIdentifierHandler = new TunnelIdentifierHandler(addressFamilyRegistry);
    }

    @Override
    public void parseAttribute(@Nonnull final ByteBuf buffer, @Nonnull final AttributesBuilder builder) {
        if (!buffer.isReadable()) {
            return;
        }
        final PmsiTunnelBuilder pmsiTunnelBuilder = new PmsiTunnelBuilder();
        pmsiTunnelBuilder.setLeafInformationRequired(buffer.readBoolean());
        final int tunnelType = buffer.readUnsignedByte();
        parseMpls(pmsiTunnelBuilder, buffer);
        final TunnelIdentifier tunnelIdentifier = this.tunnelIdentifierHandler.parse(tunnelType, buffer);
        if (tunnelIdentifier != null) {
            pmsiTunnelBuilder.setTunnelIdentifier(tunnelIdentifier);
        }
        builder.addAugmentation(PmsiTunnelAugmentation.class, new PmsiTunnelAugmentationBuilder()
                .setPmsiTunnel(pmsiTunnelBuilder.build()).build());
    }

    private static void parseMpls(final PmsiTunnelBuilder pmsiTunnelBuilder, final ByteBuf buffer) {
        final MplsLabel mpls = MplsLabelUtil.mplsLabelForByteBuf(buffer);
        if(mpls.getValue() != 0) {
            pmsiTunnelBuilder.setMplsLabel(mpls);
        }
    }

    public int getType() {
        return PMSI_ATTRIBUTE;
    }

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes,
                "Attribute parameter is not a PathAttribute object.");
        final PmsiTunnelAugmentation pmsiTunnelAugmentation = ((Attributes) attribute)
                .getAugmentation(PmsiTunnelAugmentation.class);
        if (pmsiTunnelAugmentation == null) {
            return;
        }

        final PmsiTunnel pmsiTunnelAttribute = pmsiTunnelAugmentation.getPmsiTunnel();
        final TunnelIdentifier tunnel = pmsiTunnelAttribute.getTunnelIdentifier();
        final ByteBuf tunnelBuffer = Unpooled.buffer();
        final int tunnelType = this.tunnelIdentifierHandler.serialize(tunnel, tunnelBuffer);
        final ByteBuf body = Unpooled.buffer();
        serializeFlag(pmsiTunnelAttribute, body);
        body.writeByte(tunnelType);
        serializeMpls(pmsiTunnelAttribute.getMplsLabel(), body);
        body.writeBytes(tunnelBuffer);
        AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL, getType(), body, byteAggregator);
    }

    private static void serializeMpls(final MplsLabel mplsLabel, final ByteBuf body) {
        if(mplsLabel == null) {
            body.writeZero(MPLS_LENGTH);
        }
        body.writeBytes(MplsLabelUtil.byteBufForMplsLabel(mplsLabel));
    }

    private static void serializeFlag(final PmsiTunnel pmsiTunnelAttribute, final ByteBuf body) {
        body.writeBoolean(pmsiTunnelAttribute.isLeafInformationRequired());
    }

    public Class<? extends DataObject> getClazz() {
        return org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.PmsiTunnel.class;
    }
}
