/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPTreatAsWithdrawException;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AsPathSegmentParser.SegmentType;
import org.opendaylight.protocol.bgp.parser.spi.AbstractAttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.bgp.parser.spi.RevisedErrorHandling;
import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.as.path.SegmentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for AS_PATH attribute.
 */
public final class AsPathAttributeParser extends AbstractAttributeParser implements AttributeSerializer {

    public static final int TYPE = 2;

    private final ReferenceCache refCache;
    private static final Logger LOG = LoggerFactory.getLogger(AsPathAttributeParser.class);

    private static final AsPath EMPTY = new AsPathBuilder().setSegments(Collections.emptyList()).build();

    public AsPathAttributeParser(final ReferenceCache refCache) {
        this.refCache = requireNonNull(refCache);
    }

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder,
            final RevisedErrorHandling errorHandling, final PeerSpecificParserConstraint constraint)
                    throws BGPDocumentedException, BGPTreatAsWithdrawException {
        builder.setAsPath(parseAsPath(refCache, buffer, errorHandling));
    }

    @Override
    public void serializeAttribute(final Attributes pathAttributes, final ByteBuf byteAggregator) {
        final AsPath asPath = pathAttributes.getAsPath();
        if (asPath == null) {
            return;
        }
        final ByteBuf segmentsBuffer = Unpooled.buffer();
        if (asPath.getSegments() != null) {
            for (final Segments segments : asPath.getSegments()) {
                if (segments.getAsSequence() != null) {
                    AsPathSegmentParser.serializeAsList(segments.getAsSequence(), SegmentType.AS_SEQUENCE,
                        segmentsBuffer);
                } else if (segments.getAsSet() != null) {
                    AsPathSegmentParser.serializeAsList(segments.getAsSet(), SegmentType.AS_SET, segmentsBuffer);
                } else {
                    LOG.warn("Segment doesn't have AsSequence nor AsSet list.");
                }
            }
        }
        AttributeUtil.formatAttribute(AttributeUtil.TRANSITIVE, TYPE, segmentsBuffer, byteAggregator);
    }

    /**
     * Parses AS_PATH from bytes.
     *
     * @param refCache ReferenceCache shared reference of object
     * @param buffer bytes to be parsed
     * @return new ASPath object
     * @throws BGPDocumentedException if there is no AS_SEQUENCE present (mandatory)
     */
    private static AsPath parseAsPath(final ReferenceCache refCache, final ByteBuf buffer,
            final RevisedErrorHandling errorHandling) throws BGPDocumentedException, BGPTreatAsWithdrawException {
        if (!buffer.isReadable()) {
            return EMPTY;
        }

        final List<Segments> ases = new ArrayList<>();
        boolean isSequence = false;
        for (int readable = buffer.readableBytes(); readable != 0; readable = buffer.readableBytes()) {
            if (readable < 2) {
                throw errorHandling.reportError(BGPError.AS_PATH_MALFORMED,
                    "Insufficient AS PATH segment header length %s", readable);
            }

            final int type = buffer.readUnsignedByte();
            final SegmentType segmentType = AsPathSegmentParser.parseType(type);
            if (segmentType == null) {
                throw errorHandling.reportError(BGPError.AS_PATH_MALFORMED, "Unknown AS PATH segment type %s", type);
            }
            final int count = buffer.readUnsignedByte();
            if (count == 0 && errorHandling != RevisedErrorHandling.NONE) {
                throw new BGPTreatAsWithdrawException(BGPError.AS_PATH_MALFORMED, "Empty AS_PATH segment");
            }

            // We read 2 bytes of header at this point
            readable -= 2;
            final int segmentLength = count * AsPathSegmentParser.AS_NUMBER_LENGTH;
            if (segmentLength > readable) {
                throw errorHandling.reportError(BGPError.AS_PATH_MALFORMED,
                    "Calculated segment length %s would overflow available buffer %s", segmentLength, readable);
            }

            final List<AsNumber> asList = AsPathSegmentParser.parseAsSegment(refCache, count,
                buffer.readSlice(segmentLength));
            if (segmentType == SegmentType.AS_SEQUENCE) {
                ases.add(new SegmentsBuilder().setAsSequence(asList).build());
                isSequence = true;
            } else {
                ases.add(new SegmentsBuilder().setAsSet(Set.copyOf(asList)).build());
            }
        }

        if (!isSequence) {
            throw errorHandling.reportError(BGPError.AS_PATH_MALFORMED,
                "AS_SEQUENCE must be present in AS_PATH attribute.");
        }

        return new AsPathBuilder().setSegments(ImmutableList.copyOf(ases)).build();
    }
}
