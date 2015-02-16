/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.AListCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.AListCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.ASetCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.ASetCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.AListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.a.list.AsSequence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.set._case.ASetBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AsPathAttributeParser implements AttributeParser, AttributeSerializer {

    public static final int TYPE = 2;

    private final ReferenceCache refCache;
    private static final Logger LOG = LoggerFactory.getLogger(AsPathAttributeParser.class);

    private static final AsPath EMPTY = new AsPathBuilder().setSegments(Collections.<Segments> emptyList()).build();

    public AsPathAttributeParser(final ReferenceCache refCache) {
        this.refCache = Preconditions.checkNotNull(refCache);
    }

    /**
     * Parses AS_PATH from bytes.
     *
     * @param buffer bytes to be parsed @return new ASPath object
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

            if (segmentType == SegmentType.AS_SEQUENCE) {
                final List<AsSequence> numbers = AsPathSegmentParser.parseAsSequence(refCache, count, buffer.readSlice(count * AsPathSegmentParser.AS_NUMBER_LENGTH));
                ases.add(new SegmentsBuilder().setCSegment(
                    new AListCaseBuilder().setAList(new AListBuilder().setAsSequence(numbers).build()).build()).build());
                isSequence = true;
            } else {
                final List<AsNumber> list = AsPathSegmentParser.parseAsSet(refCache, count, buffer.readSlice(count * AsPathSegmentParser.AS_NUMBER_LENGTH));
                ases.add(new SegmentsBuilder().setCSegment(new ASetCaseBuilder().setASet(new ASetBuilder().setAsSet(list).build()).build()).build());
            }
        }
        if (!isSequence) {
            throw new BGPDocumentedException("AS_SEQUENCE must be present in AS_PATH attribute.", BGPError.AS_PATH_MALFORMED);
        }

        ases.trimToSize();
        return new AsPathBuilder().setSegments(ases).build();
    }

    @Override
    public void parseAttribute(final ByteBuf buffer, final PathAttributesBuilder builder) throws BGPDocumentedException, BGPParsingException {
        builder.setAsPath(parseAsPath(this.refCache, buffer));
    }

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof PathAttributes, "Attribute parameter is not a PathAttribute object.");
        final PathAttributes pathAttributes = (PathAttributes) attribute;
        final AsPath asPath = pathAttributes.getAsPath();
        if (asPath == null) {
            return;
        }
        final ByteBuf segmentsBuffer = Unpooled.buffer();
        if (asPath.getSegments() != null) {
            for (final Segments segments : asPath.getSegments()) {
                if (segments.getCSegment() instanceof AListCase) {
                    final AListCase listCase = (AListCase) segments.getCSegment();
                    AsPathSegmentParser.serializeAsSequence(listCase, segmentsBuffer);
                } else if (segments.getCSegment() instanceof ASetCase) {
                    final ASetCase set = (ASetCase) segments.getCSegment();
                    AsPathSegmentParser.serializeAsSet(set, segmentsBuffer);
                } else {
                    LOG.warn("Segment class is neither AListCase nor ASetCase.");
                }
            }
        }
        AttributeUtil.formatAttribute(AttributeUtil.TRANSITIVE, TYPE, segmentsBuffer, byteAggregator);
    }
}
