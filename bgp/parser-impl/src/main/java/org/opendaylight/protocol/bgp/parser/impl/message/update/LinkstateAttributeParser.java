package org.opendaylight.protocol.bgp.parser.impl.message.update;

import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.PathAttributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributesBuilder;

final class LinkstateAttributeParser implements AttributeParser {
	// FIXME: update to IANA number once it is known
	static final int TYPE = 99;

	@Override
	public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) throws BGPParsingException {
		final PathAttributes1 a = new PathAttributes1Builder().setLinkstatePathAttribute(LinkStateParser.parseLinkState(bytes)).build();
		builder.addAugmentation(PathAttributes1.class, a);
	}
}