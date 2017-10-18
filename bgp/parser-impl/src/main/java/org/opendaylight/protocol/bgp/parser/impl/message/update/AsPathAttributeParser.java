/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AsPathSegmentParser.SegmentType;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AsPathAttributeParser implements AttributeParser, AttributeSerializer {

    public static final int TYPE = 2;

    private final ReferenceCache refCache;
    private static final Logger LOG = LoggerFactory.getLogger(AsPathAttributeParser.class);

    private static final AsPath EMPTY = new AsPathBuilder().setSegments(Collections.emptyList()).build();

    public AsPathAttributeParser(final ReferenceCache refCache) {
        this.refCache = requireNonNull(refCache);
    }

    /**
     * Parses AS_PATH from bytes.
     *
     * @param refCache ReferenceCache shared reference of object
     * @param buffer bytes to be parsed
     * @return new ASPath object
     * @throws BGPDocumentedException if there is no AS_SEQUENCE present (mandatory)
     * @throws BGPParsingException
     */
    private static AsPath parseAsPath(final ReferenceCache refCache, final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        if (!buffer.isReadable()) {
            return EMPTY;
        }
        final ArrayList<Segments> ases = new ArrayList<>();
        boolean isSequence = false;
        while (buffer.isReadable()) {
            final int type = buffer.readUnsignedByte();
            final SegmentType segmentType = AsPathSegmentParser.parseType(type);
            if (segmentType == null) {
                throw new BGPParsingException("AS Path segment type unknown : " + type);
            }
            final int count = buffer.readUnsignedByte();

            final List<AsNumber> asList = AsPathSegmentParser.parseAsSegment(refCache, count, buffer.readSlice(count * AsPathSegmentParser.AS_NUMBER_LENGTH));
            if (segmentType == SegmentType.AS_SEQUENCE) {
                ases.add(new SegmentsBuilder().setAsSequence(asList).build());
                isSequence = true;
            } else {
                ases.add(new SegmentsBuilder().setAsSet(asList).build());
            }
        }
        if (!isSequence) {
            throw new BGPDocumentedException("AS_SEQUENCE must be present in AS_PATH attribute.", BGPError.AS_PATH_MALFORMED);
        }

        ases.trimToSize();
        return new AsPathBuilder().setSegments(ases).build();
    }

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder) throws BGPDocumentedException, BGPParsingException {
        builder.setAsPath(parseAsPath(this.refCache, buffer));
    }

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes, "Attribute parameter is not a PathAttribute object.");
        final Attributes pathAttributes = (Attributes) attribute;
        final AsPath asPath = pathAttributes.getAsPath();
        if (asPath == null) {
            return;
        }
        final ByteBuf segmentsBuffer = Unpooled.buffer();
        if (asPath.getSegments() != null) {
            for (final Segments segments : asPath.getSegments()) {
                if (segments.getAsSequence() != null) {
                    AsPathSegmentParser.serializeAsList(segments.getAsSequence(), SegmentType.AS_SEQUENCE, segmentsBuffer);
                } else if (segments.getAsSet() != null) {
                    AsPathSegmentParser.serializeAsList(segments.getAsSet(), SegmentType.AS_SET, segmentsBuffer);
                } else {
                    LOG.warn("Segment doesn't have AsSequence nor AsSet list.");
                }
            }
        }
        AttributeUtil.formatAttribute(AttributeUtil.TRANSITIVE, TYPE, segmentsBuffer, byteAggregator);
    }
}
