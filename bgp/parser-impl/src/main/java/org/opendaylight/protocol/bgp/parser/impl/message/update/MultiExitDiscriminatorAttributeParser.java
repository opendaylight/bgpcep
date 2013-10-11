package org.opendaylight.protocol.bgp.parser.impl.message.update;

import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributesBuilder;

final class MultiExitDiscriminatorAttributeParser implements AttributeParser {
	static final int TYPE = 4;

	@Override
	public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) {
		builder.setMultiExitDisc(new MultiExitDiscBuilder().setMed(ByteArray.bytesToLong(bytes)).build());
	}
}