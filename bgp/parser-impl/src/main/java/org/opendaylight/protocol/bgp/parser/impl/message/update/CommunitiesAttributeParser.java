package org.opendaylight.protocol.bgp.parser.impl.message.update;

import java.util.List;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributesBuilder;

import com.google.common.collect.Lists;

final class CommunitiesAttributeParser implements AttributeParser {
	static final int TYPE = 8;

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