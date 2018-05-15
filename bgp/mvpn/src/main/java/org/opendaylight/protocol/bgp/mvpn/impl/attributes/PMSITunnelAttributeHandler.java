/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mvpn.impl.attributes;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.mvpn.spi.pojo.attributes.tunnel.identifier.SimpleTunnelIdentifierRegistry;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.util.MplsLabelUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev180329.bgp.rib.route.PmsiTunnelAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev180329.bgp.rib.route.PmsiTunnelAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev180329.pmsi.tunnel.PmsiTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev180329.pmsi.tunnel.PmsiTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev180329.pmsi.tunnel.pmsi.tunnel.TunnelIdentifier;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * PE Distinguisher Labels Attribute Handler.
 * https://tools.ietf.org/html/rfc6514#section-5
 *
 * @author Claudio D. Gasparini
 */
public final class PMSITunnelAttributeHandler implements AttributeParser, AttributeSerializer {
    private static final int PMSI_ATTRIBUTE = 22;
    private static final int MPLS_LENGTH = 3;
    private final SimpleTunnelIdentifierRegistry tunnelIdentifierHandler =
            SimpleTunnelIdentifierRegistry.getInstance();

    private static void parseMpls(final PmsiTunnelBuilder pmsiTunnelBuilder, final ByteBuf buffer) {
        final MplsLabel mpls = MplsLabelUtil.mplsLabelForByteBuf(buffer);
        if (mpls.getValue() != 0) {
            pmsiTunnelBuilder.setMplsLabel(mpls);
        }
    }

    private static void serializeMpls(final MplsLabel mplsLabel, final ByteBuf body) {
        if (mplsLabel == null) {
            body.writeZero(MPLS_LENGTH);
            return;
        }
        body.writeBytes(MplsLabelUtil.byteBufForMplsLabel(mplsLabel));
    }

    private static void serializeFlag(final PmsiTunnel pmsiTunnelAttribute, final ByteBuf body) {
        body.writeBoolean(pmsiTunnelAttribute.isLeafInformationRequired());
    }

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder) {
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

    public int getType() {
        return PMSI_ATTRIBUTE;
    }

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes,
                "Attribute parameter is not a PathAttribute object.");
        final PmsiTunnelAugmentation pmsiTunnelAugmentation = ((Attributes) attribute)
                .augmentation(PmsiTunnelAugmentation.class);
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

    public Class<? extends DataObject> getClazz() {
        return org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev180329.PmsiTunnel.class;
    }
}
