package org.opendaylight.protocol.bgp.parser.impl.message.update;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.PathAttributes1Builder;

final class MPReachAttributeParser implements AttributeParser {
	static final int TYPE = 14;

	@Override
	public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) throws BGPDocumentedException {
		try {
			final PathAttributes1 a = new PathAttributes1Builder().setMpReachNlri(MPReachParser.parseMPReach(bytes)).build();

			builder.addAugmentation(PathAttributes1.class, a);
		} catch (final BGPParsingException e) {
			throw new BGPDocumentedException("Could not parse MP_REACH_NLRI: " + e.getMessage(), BGPError.OPT_ATTR_ERROR);
		}
	}
}