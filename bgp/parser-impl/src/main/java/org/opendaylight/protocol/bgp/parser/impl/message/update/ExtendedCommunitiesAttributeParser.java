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
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public final class ExtendedCommunitiesAttributeParser implements AttributeParser {
	public static final int TYPE = 16;

	private final ReferenceCache refCache;

	public ExtendedCommunitiesAttributeParser(final ReferenceCache refCache) {
		this.refCache = Preconditions.checkNotNull(refCache);
	}

	@Override
	public void parseAttribute(final ByteBuf buffer, final PathAttributesBuilder builder) throws BGPDocumentedException {
		final List<ExtendedCommunities> set = Lists.newArrayList();
		while (buffer.isReadable()) {
			final ExtendedCommunities comm = CommunitiesParser.parseExtendedCommunity(this.refCache, buffer.slice(buffer.readerIndex(), CommunitiesParser.EXTENDED_COMMUNITY_LENGTH));
			buffer.skipBytes(CommunitiesParser.EXTENDED_COMMUNITY_LENGTH);
			set.add(comm);
		}
		builder.setExtendedCommunities(set);
	}
}