/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;

public abstract class AbstractFlowspecNlriParser implements NlriParser, NlriSerializer {

    protected static final int NLRI_LENGTH = 1;
    protected static final int NLRI_LENGTH_EXTENDED = 2;

    protected SimpleFlowspecTypeRegistry flowspecTypeRegistry;

    /**
     * Add this constant to length value to achieve all ones in the leftmost nibble.
     */
    private static final int LENGTH_MAGIC = 61440;
    private static final int MAX_NLRI_LENGTH = 4095;
    private static final int MAX_NLRI_LENGTH_ONE_BYTE = 240;

    protected abstract void serializeMpReachNlri(final Attributes1 pathAttributes, final ByteBuf byteAggregator);

    protected abstract void serializeMpUnreachNlri(final Attributes2 pathAttributes, final ByteBuf byteAggregator);

    abstract DestinationType createWidthdrawnRoutesDestinationType(final List<Flowspec> dst);

    abstract DestinationType createAdvertizedRoutesDestinationType(final List<Flowspec> dst);

    @Override
    public final void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes, "Attribute parameter is not a PathAttribute object.");
        final Attributes pathAttributes = (Attributes) attribute;
        final Attributes1 pathAttributes1 = pathAttributes.getAugmentation(Attributes1.class);
        final Attributes2 pathAttributes2 = pathAttributes.getAugmentation(Attributes2.class);
        serializeMpReachNlri(pathAttributes1, byteAggregator);
        serializeMpUnreachNlri(pathAttributes2, byteAggregator);
    }

    @Override
    public final void parseNlri(final ByteBuf nlri, final MpUnreachNlriBuilder builder) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<Flowspec> dst = parseNlri(nlri);

        builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(createWidthdrawnRoutesDestinationType(dst)).build());
    }

    @Override
    public final void parseNlri(final ByteBuf nlri, final MpReachNlriBuilder builder) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<Flowspec> dst = parseNlri(nlri);
        builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(createAdvertizedRoutesDestinationType(dst)).build());
    }

    /**
     * Serializes Flowspec NLRI to ByteBuf.
     *
     * @param flows flowspec NLRI to be serialized
     * @param buffer where flowspec NLRI will be serialized
     */
    public final void serializeNlri(final List<Flowspec> flows, final ByteBuf buffer) {
        final ByteBuf nlriByteBuf = Unpooled.buffer();
        for (final Flowspec flow : flows) {
            this.flowspecTypeRegistry.serializeFlowspecType(flow.getFlowspecType(), nlriByteBuf);
        }
        Preconditions.checkState(nlriByteBuf.readableBytes() <= MAX_NLRI_LENGTH, "Maximum length of Flowspec NLRI reached.");
        if (nlriByteBuf.readableBytes() <= MAX_NLRI_LENGTH_ONE_BYTE) {
            buffer.writeByte(nlriByteBuf.readableBytes());
        } else {
            buffer.writeShort(nlriByteBuf.readableBytes() + LENGTH_MAGIC);
        }
        buffer.writeBytes(nlriByteBuf);
    }

    /**
     * Parses Flowspec NLRI into list of Flowspec.
     *
     * @param nlri byte representation of NLRI which will be parsed
     * @return list of Flowspec
     */
    public final List<Flowspec> parseNlri(final ByteBuf nlri) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return null;
        }
        final List<Flowspec> fss = new ArrayList<>();

        // length field can be one or two bytes (if needed)
        // check the length of nlri to see how many bytes we can skip
        final int length = nlri.readableBytes();
        nlri.skipBytes(length > MAX_NLRI_LENGTH_ONE_BYTE ? NLRI_LENGTH_EXTENDED : NLRI_LENGTH);

        while (nlri.isReadable()) {
            final FlowspecBuilder builder = new FlowspecBuilder();
            builder.setFlowspecType(this.flowspecTypeRegistry.parseFlowspecType(nlri));
            fss.add(builder.build());
        }
        return fss;
    }

}
