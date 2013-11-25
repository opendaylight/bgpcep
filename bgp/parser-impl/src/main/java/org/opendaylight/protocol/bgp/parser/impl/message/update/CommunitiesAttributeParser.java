/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import java.util.List;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;

import com.google.common.collect.Lists;

public final class CommunitiesAttributeParser implements AttributeParser {
	public static final int TYPE = 8;

	@Override
	public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) throws BGPDocumentedException {
		final List<Communities> set = Lists.newArrayList();
		int i = 0;
		while (i < bytes.length) {
			set.add((Communities) CommunitiesParser.parseCommunity(ByteArray.subByte(bytes, i, CommunitiesParser.COMMUNITY_LENGTH)));
			i += CommunitiesParser.COMMUNITY_LENGTH;
		}

		builder.setCommunities(set);
	}
}