package org.opendaylight.protocol.bgp.parser.impl.message.update;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.PathAttributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.PathAttributes2Builder;

import com.google.common.base.Preconditions;

final class MPUnreachAttributeParser implements AttributeParser {
	static final int TYPE = 15;

	private final NlriRegistry reg;

	MPUnreachAttributeParser(final NlriRegistry reg) {
		this.reg = Preconditions.checkNotNull(reg);
	}

	@Override
	public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) throws BGPDocumentedException {
		try {
			final PathAttributes2 a = new PathAttributes2Builder().setMpUnreachNlri(reg.parseMpUnreach(bytes)).build();
			builder.addAugmentation(PathAttributes2.class, a);
		} catch (final BGPParsingException e) {
			throw new BGPDocumentedException("Could not parse MP_UNREACH_NLRI: " + e.getMessage(), BGPError.OPT_ATTR_ERROR);
		}
	}
}