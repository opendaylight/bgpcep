package org.opendaylight.protocol.bgp.parser.impl.message.update;

import java.util.List;

import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;

import com.google.common.collect.Lists;

final class ClusterIdAttributeParser implements AttributeParser {
	static final int TYPE = 10;

	@Override
	public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) {
		final List<ClusterIdentifier> list = Lists.newArrayList();
		int i = 0;
		while (i < bytes.length) {
			list.add(new ClusterIdentifier(ByteArray.subByte(bytes, i, 4)));
			i += 4;
		}

		builder.setClusterId(list);
	}
}