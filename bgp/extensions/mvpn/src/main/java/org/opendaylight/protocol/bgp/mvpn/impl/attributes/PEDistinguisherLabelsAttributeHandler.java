/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mvpn.impl.attributes;

import static org.opendaylight.protocol.bgp.parser.spi.AttributeUtil.formatAttribute;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPTreatAsWithdrawException;
import org.opendaylight.protocol.bgp.parser.spi.AbstractAttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.bgp.parser.spi.RevisedErrorHandling;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.protocol.util.MplsLabelUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.bgp.rib.route.PeDistinguisherLabelsAttributeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.bgp.rib.route.PeDistinguisherLabelsAttributeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.pe.distinguisher.labels.attribute.PeDistinguisherLabelsAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.pe.distinguisher.labels.attribute.PeDistinguisherLabelsAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.pe.distinguisher.labels.attribute.pe.distinguisher.labels.attribute.PeDistinguisherLabelAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.pe.distinguisher.labels.attribute.pe.distinguisher.labels.attribute.PeDistinguisherLabelAttributeBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * PE Distinguisher Labels Attribute Handler.
 * https://tools.ietf.org/html/rfc6514#section-8
 *
 * @author Claudio D. Gasparini
 */
public final class PEDistinguisherLabelsAttributeHandler extends AbstractAttributeParser
        implements AttributeSerializer {

    private static final int TYPE = 27;

    public PEDistinguisherLabelsAttributeHandler() {
    }

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder,
            final RevisedErrorHandling errorHandling, final PeerSpecificParserConstraint constraint)
                    throws BGPTreatAsWithdrawException {
        final int readable = buffer.readableBytes();
        if (readable == 0) {
            return;
        }

        final boolean isIpv4;
        final int count;
        if (readable % 7 == 0) {
            count = readable / 7;
            isIpv4 = true;
        } else if (readable % 19 == 0) {
            count = readable / 19;
            isIpv4 = false;
        } else {
            // RFC-6514 page 16:
            //        When a router that receives a BGP Update that contains the PE
            //        Distinguisher Labels attribute with its Partial bit set determines
            //        that the attribute is malformed, the router SHOULD treat this Update
            //        as though all the routes contained in this Update had been withdrawn.
            throw new BGPTreatAsWithdrawException(BGPError.MALFORMED_ATTR_LIST,
                "PE Distinguisher Labels has incorrect length %s", readable);
        }

        final List<PeDistinguisherLabelAttribute> list = new ArrayList<>(count);
        for (int i = 0; i < count; ++i) {
            final PeDistinguisherLabelAttributeBuilder attribute = new PeDistinguisherLabelAttributeBuilder();
            if (isIpv4) {
                attribute.setPeAddress(new IpAddressNoZone(Ipv4Util.addressForByteBuf(buffer)));
            } else {
                attribute.setPeAddress(new IpAddressNoZone(Ipv6Util.addressForByteBuf(buffer)));
            }
            attribute.setMplsLabel(MplsLabelUtil.mplsLabelForByteBuf(buffer));
            list.add(attribute.build());
        }

        builder.addAugmentation(new PeDistinguisherLabelsAttributeAugmentationBuilder()
            .setPeDistinguisherLabelsAttribute(new PeDistinguisherLabelsAttributeBuilder()
                .setPeDistinguisherLabelAttribute(list)
                .build())
            .build());
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
            if (peDist.getPeAddress().getIpv4AddressNoZone() != null) {
                buffer.writeBytes(Ipv4Util.bytesForAddress(peDist.getPeAddress().getIpv4AddressNoZone()));
            } else {
                buffer.writeBytes(Ipv6Util.bytesForAddress(peDist.getPeAddress().getIpv6AddressNoZone()));
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
