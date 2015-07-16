/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.labeled_unicast;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.Label;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.LabelStack;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.LabelStackBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.destination.CLabeledUnicastDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.destination.CLabeledUnicastDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLabeledUnicastCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.labeled.unicast._case.DestinationLabeledUnicastBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLabeledUnicastCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LUNlriParser implements NlriParser, NlriSerializer{

    private static final Logger LOG = LoggerFactory.getLogger(LUNlriParser.class);

    static final NodeIdentifier PREFIX_TYPE_NID = new NodeIdentifier(QName.cachedReference(QName.create(CLabeledUnicastDestination.QNAME, "prefix-type")));
    static final NodeIdentifier LABEL_STACK_NID = new NodeIdentifier(LabelStack.QNAME);
    static final NodeIdentifier LABEL_NID = new NodeIdentifier(Label.QNAME);
    static final NodeIdentifier LV_NID = new NodeIdentifier(QName.create(Label.QNAME,"label-value"));

    private static final int LABEL_LENGTH = 3;
    @Override
    public void serializeAttribute(DataObject attribute, ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes, "Attribute parameter is not a PathAttribute object");
        final Attributes pathAttributes = (Attributes) attribute;
        final Attributes1 pathAttributes1 = pathAttributes.getAugmentation(Attributes1.class);
        final Attributes2 pathAttributes2 = pathAttributes.getAugmentation(Attributes2.class);
        if(pathAttributes1 != null) {
            final AdvertizedRoutes routes = (pathAttributes1.getMpReachNlri()).getAdvertizedRoutes();
            if(routes != null && routes.getDestinationType() instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLabeledUnicastCase) {
                final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLabeledUnicastCase labeledUnicastCase = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLabeledUnicastCase) routes.getDestinationType();
                serializeNlri(labeledUnicastCase.getDestinationLabeledUnicast().getCLabeledUnicastDestination(), byteAggregator, pathAttributes1.getMpReachNlri().getAfi());
            }
        } else if(pathAttributes2 != null) {
            final MpUnreachNlri mpUnreachNlri = pathAttributes2.getMpUnreachNlri();
            if(mpUnreachNlri.getWithdrawnRoutes() != null && mpUnreachNlri.getWithdrawnRoutes().getDestinationType() instanceof DestinationLabeledUnicastCase) {
                final DestinationLabeledUnicastCase labeledUnicastCase = (DestinationLabeledUnicastCase) mpUnreachNlri.getWithdrawnRoutes().getDestinationType();
                serializeNlri(labeledUnicastCase.getDestinationLabeledUnicast().getCLabeledUnicastDestination(), byteAggregator, mpUnreachNlri.getAfi());
            }
        }
    }

    public static void serializeNlri(final List<CLabeledUnicastDestination> dests, final ByteBuf buffer, final Class<? extends AddressFamily> afiClass) {
        final ByteBuf nlriByteBuf = Unpooled.buffer();
        for(final CLabeledUnicastDestination dest: dests) {
            final List<LabelStack> labelStack = dest.getLabelStack();
            final int labelNum = labelStack.size();
            int prefixLenInByte = 0;
            byte[] prefixBytes = null;
            if(afiClass.equals(Ipv4AddressFamily.class)) {
                prefixLenInByte = Ipv4Util.getPrefixLengthBytes(dest.getPrefixType().getIpv4Prefix().getValue());
            } else {
                prefixLenInByte = Ipv4Util.getPrefixLengthBytes(dest.getPrefixType().getIpv6Prefix().getValue());
            }
            // Serialize the length field
            // Length field contains one Byte which represents the length of labelstack and prefix in bit
            nlriByteBuf.writeByte(3*labelNum*Byte.SIZE + prefixLenInByte*Byte.SIZE);

            // Serialize the labelstack field
            // Labelstack field contains label(s), each label contains three Bytes
            for(int i = 0; i < labelNum; i++) {

                if(i == labelNum - 1) {
                    nlriByteBuf.writeMedium((int)(labelStack.get(i).getLabelValue() << 4 | 0x1));
                } else {
                    nlriByteBuf.writeMedium((int)(labelStack.get(i).getLabelValue() << 4));
                }
            }

            // Serialize the prefix field
            // Prefix field contains ipv4/ipv6 prefix
            if(afiClass.equals(Ipv4AddressFamily.class)) {
                prefixBytes = Ipv4Util.bytesForPrefixBegin(dest.getPrefixType().getIpv4Prefix());
            } else {
                prefixBytes = Ipv6Util.bytesForPrefixBegin(dest.getPrefixType().getIpv6Prefix());
            }
            nlriByteBuf.writeBytes(Arrays.copyOfRange(prefixBytes, 1, prefixBytes.length));
        }
        buffer.writeBytes(nlriByteBuf);
    }

    @Override
    public void parseNlri(ByteBuf nlri, MpUnreachNlriBuilder builder)
            throws BGPParsingException {
        if(!nlri.isReadable()) {
            return;
        }
        final List<CLabeledUnicastDestination> dst = parseNlri(nlri, builder.getAfi());

        builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLabeledUnicastCaseBuilder().setDestinationLabeledUnicast(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.labeled.unicast._case.DestinationLabeledUnicastBuilder().setCLabeledUnicastDestination(
                    dst).build()).build()).build());
    }

    public List<CLabeledUnicastDestination> parseNlri(ByteBuf nlri, Class<? extends AddressFamily> afiClass) {
        short length;
        if(!nlri.isReadable()) {
            return null;
        }
        final List<CLabeledUnicastDestination> dests = new ArrayList<>();

        while(nlri.isReadable()) {
            final CLabeledUnicastDestinationBuilder builder = new CLabeledUnicastDestinationBuilder();
            length = nlri.readByte();

            builder.setLabelStack(parseLabel(nlri));

            int labelNum = builder.getLabelStack().size();
            int prefixLen = length - LABEL_LENGTH * Byte.SIZE * labelNum;
            int prefixLenInByte = prefixLen/Byte.SIZE + ((prefixLen%Byte.SIZE == 0) ? 0 : 1);
            if(afiClass.equals(Ipv4AddressFamily.class)) {
                builder.setPrefixType(new IpPrefix(Ipv4Util.prefixForBytes(ByteArray.readBytes(nlri, prefixLenInByte), prefixLen)));
            } else {
                builder.setPrefixType(new IpPrefix(Ipv6Util.prefixForBytes(ByteArray.readBytes(nlri, prefixLenInByte), prefixLen)));
            }
            dests.add(builder.build());
        }
        return dests;
    }

    public List<LabelStack> parseLabel(ByteBuf nlri) {
        if(!nlri.isReadable()){
            return null;
        }
        final List<LabelStack> labels = new ArrayList<>();
        long bottomBit = 0;
        do {
            long label = nlri.readUnsignedMedium();
            bottomBit = label & 0x1;
            LabelStackBuilder labelStack = new LabelStackBuilder();
            labelStack.setLabelValue(label >> 4);
            labels.add(labelStack.build());
        } while (bottomBit != 1);
        return labels;
    }

    @Override
    public void parseNlri(ByteBuf nlri, MpReachNlriBuilder builder)
            throws BGPParsingException {
        if(!nlri.isReadable()) {
            return;
        }
        final List<CLabeledUnicastDestination> dst = parseNlri(nlri, builder.getAfi());

        builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationLabeledUnicastCaseBuilder().setDestinationLabeledUnicast(
                new DestinationLabeledUnicastBuilder().setCLabeledUnicastDestination(
                    dst).build()).build()).build());
    }

    // Conversion from DataContainer to LabeledUnicastDestination Object
    public static CLabeledUnicastDestination extractCLabeledUnicastDestination(final DataContainerNode<? extends PathArgument> route) {
        final CLabeledUnicastDestinationBuilder builder = new CLabeledUnicastDestinationBuilder();

        if(route.getChild(PREFIX_TYPE_NID).isPresent()){
            final String prefixType = (String) route.getChild(PREFIX_TYPE_NID).get().getValue();

            try {
                Ipv4Util.bytesForPrefixBegin(new Ipv4Prefix(prefixType));
                builder.setPrefixType(new IpPrefix(new Ipv4Prefix(prefixType)));
            } catch (final IllegalArgumentException e) {
                LOG.debug("Creating Ipv6 prefix because", e);
                builder.setPrefixType(new IpPrefix(new Ipv6Prefix(prefixType)));
            }
        }
        final Optional<DataContainerChild<? extends PathArgument, ?>> labelStacks = route.getChild(LABEL_STACK_NID);
        final List<LabelStack> labels = new ArrayList<>();
        final LabelStackBuilder labelStackbuilder = new LabelStackBuilder();
        if(labelStacks.isPresent()){
            for(final UnkeyedListEntryNode label : ((UnkeyedListNode)labelStacks.get()).getValue()){
                final Optional<DataContainerChild<? extends PathArgument, ?>> labelStack = label.getChild(LV_NID);
                if(labelStack.isPresent()) {
                    labelStackbuilder.setLabelValue((Long) labelStack.get().getValue());
                }
            }
        }
        labels.add(labelStackbuilder.build());
        builder.setLabelStack(labels);
        return builder.build();
    }
}
