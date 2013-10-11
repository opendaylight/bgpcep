package org.opendaylight.protocol.bgp.parser.impl.message.update;

import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributesBuilder;

final class AS4PathAttributeParser implements AttributeParser {
	static final int TYPE = 18;

	@Override
	public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) {
		// AS4 Path is ignored
	}
}