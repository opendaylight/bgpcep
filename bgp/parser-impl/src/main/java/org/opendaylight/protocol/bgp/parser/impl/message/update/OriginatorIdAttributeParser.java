package org.opendaylight.protocol.bgp.parser.impl.message.update;

import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributesBuilder;

final class OriginatorIdAttributeParser implements AttributeParser {
	static final int TYPE = 9;

	@Override
	public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) {
		if (bytes.length != 4) {
			throw new IllegalArgumentException("Length of byte array for ORIGINATOR_ID should be 4, but is " + bytes.length);
		}

		builder.setOriginatorId(bytes);
	}
}