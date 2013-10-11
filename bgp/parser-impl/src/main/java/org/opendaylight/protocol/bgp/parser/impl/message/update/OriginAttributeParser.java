package org.opendaylight.protocol.bgp.parser.impl.message.update;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;

import com.google.common.primitives.UnsignedBytes;

final class OriginAttributeParser implements AttributeParser {
	static final int TYPE = 1;

	@Override
	public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) throws BGPDocumentedException {
		final BgpOrigin borigin = BgpOrigin.forValue(UnsignedBytes.toInt(bytes[0]));
		if (borigin == null) {
			throw new BGPDocumentedException("Unknown Origin type.", BGPError.ORIGIN_ATTR_NOT_VALID, new byte[] { (byte) 0x01, (byte) 0x01, bytes[0] });
		}

		builder.setOrigin(new OriginBuilder().setValue(borigin).build());
	}
}