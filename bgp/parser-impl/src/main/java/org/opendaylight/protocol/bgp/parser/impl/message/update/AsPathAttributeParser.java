/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import io.netty.buffer.ByteBuf;

import java.util.List;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AsPathSegmentParser.SegmentType;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.AListCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.ASetCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.AListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.a.list.AsSequence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.set._case.ASetBuilder;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedBytes;

public final class AsPathAttributeParser implements AttributeParser {
	public static final int TYPE = 2;
	private final ReferenceCache refCache;

	public AsPathAttributeParser(final ReferenceCache refCache) {
		this.refCache = Preconditions.checkNotNull(refCache);
	}

	/**
	 * Parses AS_PATH from bytes.
	 * 
	 * @param bytes byte array to be parsed
	 * @return new ASPath object
	 * @throws BGPDocumentedException if there is no AS_SEQUENCE present (mandatory)
	 * @throws BGPParsingException
	 */
	private static AsPath parseAsPath(final ReferenceCache refCache, final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
		final List<Segments> ases = Lists.newArrayList();
		boolean isSequence = false;
		while (buffer.readableBytes() != 0) {
			final int type = UnsignedBytes.toInt(buffer.readByte());
			final SegmentType segmentType = AsPathSegmentParser.parseType(type);
			if (segmentType == null) {
				throw new BGPParsingException("AS Path segment type unknown : " + type);
			}
			final int count = UnsignedBytes.toInt(buffer.readByte());

			if (segmentType == SegmentType.AS_SEQUENCE) {
				final List<AsSequence> numbers = AsPathSegmentParser.parseAsSequence(refCache, count, buffer.slice(buffer.readerIndex(), count * AsPathSegmentParser.AS_NUMBER_LENGTH));
				ases.add(new SegmentsBuilder().setCSegment(
						new AListCaseBuilder().setAList(new AListBuilder().setAsSequence(numbers).build()).build()).build());
				isSequence = true;
			} else {
				final List<AsNumber> list = AsPathSegmentParser.parseAsSet(refCache, count, buffer.slice(buffer.readerIndex(), count * AsPathSegmentParser.AS_NUMBER_LENGTH));
				ases.add(new SegmentsBuilder().setCSegment(new ASetCaseBuilder().setASet(new ASetBuilder().setAsSet(list).build()).build()).build());

			}
			buffer.skipBytes(count * AsPathSegmentParser.AS_NUMBER_LENGTH);
		}

		if (!isSequence && buffer.readableBytes() != 0) {
			throw new BGPDocumentedException("AS_SEQUENCE must be present in AS_PATH attribute.", BGPError.AS_PATH_MALFORMED);
		}
		return new AsPathBuilder().setSegments(ases).build();
	}

	@Override
	public void parseAttribute(final ByteBuf buffer, final PathAttributesBuilder builder) throws BGPDocumentedException, BGPParsingException {
		builder.setAsPath(parseAsPath(this.refCache, buffer));
	}
}