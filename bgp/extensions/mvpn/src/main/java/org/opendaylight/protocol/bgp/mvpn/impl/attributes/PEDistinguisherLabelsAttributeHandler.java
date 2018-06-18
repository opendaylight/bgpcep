/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mvpn.impl.attributes;

import static org.opendaylight.protocol.bgp.parser.spi.AttributeUtil.formatAttribute;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.protocol.util.MplsLabelUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.bgp.rib.route.PeDistinguisherLabelsAttributeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.bgp.rib.route.PeDistinguisherLabelsAttributeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.pe.distinguisher.labels.attribute.PeDistinguisherLabelsAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.pe.distinguisher.labels.attribute.PeDistinguisherLabelsAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.pe.distinguisher.labels.attribute.pe.distinguisher.labels.attribute.PeDistinguisherLabelAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.pe.distinguisher.labels.attribute.pe.distinguisher.labels.attribute.PeDistinguisherLabelAttributeBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * PE Distinguisher Labels Attribute Handler.
 * https://tools.ietf.org/html/rfc6514#section-8
 *
 * @author Claudio D. Gasparini
 */
public final class PEDistinguisherLabelsAttributeHandler implements AttributeParser, AttributeSerializer {

    private static final int TYPE = 27;

    public PEDistinguisherLabelsAttributeHandler() {
    }

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder) {
        if (!buffer.isReadable()) {
            return;
        }
        final boolean isIpv4 = buffer.readableBytes() % 7 == 0;
        final boolean isIpv6 = buffer.readableBytes() % 19 == 0;
        Preconditions.checkArgument(isIpv4 || isIpv6,
                "Length of byte array should be multiple of 7 or multiple of 19");

        final List<PeDistinguisherLabelAttribute> list = Lists.newArrayList();
        while (buffer.isReadable()) {
            final PeDistinguisherLabelAttributeBuilder attribute = new PeDistinguisherLabelAttributeBuilder();
            if (isIpv4) {
                attribute.setPeAddress(new IpAddress(Ipv4Util.addressForByteBuf(buffer)));
            } else {
                attribute.setPeAddress(new IpAddress(Ipv6Util.addressForByteBuf(buffer)));
            }
            attribute.setMplsLabel(MplsLabelUtil.mplsLabelForByteBuf(buffer));
            list.add(attribute.build());
        }

        builder.addAugmentation(PeDistinguisherLabelsAttributeAugmentation.class,
                new PeDistinguisherLabelsAttributeAugmentationBuilder()
                        .setPeDistinguisherLabelsAttribute(new PeDistinguisherLabelsAttributeBuilder()
                                .setPeDistinguisherLabelAttribute(list).build()).build());
    }

    @Override
    public void serializeAttribute(final Attributes attribute, final ByteBuf byteAggregator) {
        final PeDistinguisherLabelsAttributeAugmentation att =
                attribute.augmentation(PeDistinguisherLabelsAttributeAugmentation.class);

        if (att == null) {
            return;
        }

        final List<PeDistinguisherLabelAttribute> distinguishers
                = att.getPeDistinguisherLabelsAttribute().getPeDistinguisherLabelAttribute();
        final ByteBuf buffer = Unpooled.buffer();
        for (final PeDistinguisherLabelAttribute peDist : distinguishers) {
            if (peDist.getPeAddress().getIpv4Address() != null) {
                buffer.writeBytes(Ipv4Util.bytesForAddress(peDist.getPeAddress().getIpv4Address()));
            } else {
                buffer.writeBytes(Ipv6Util.bytesForAddress(peDist.getPeAddress().getIpv6Address()));
            }
            buffer.writeBytes(MplsLabelUtil.byteBufForMplsLabel(peDist.getMplsLabel()));
        }
        formatAttribute(AttributeUtil.OPTIONAL | AttributeUtil.TRANSITIVE, TYPE, buffer, byteAggregator);
    }

    public int getType() {
        return TYPE;
    }

    public Class<? extends DataObject> getClazz() {
        return PeDistinguisherLabelsAttribute.class;
    }
}
