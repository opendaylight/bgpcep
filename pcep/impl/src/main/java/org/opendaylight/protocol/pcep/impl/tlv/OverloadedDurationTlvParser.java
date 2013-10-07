/**
 * 
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import io.netty.buffer.ByteBuf;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OverloadDurationTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.notification.object.tlvs.OverloadDurationBuilder;

/**
 * Parser for {@link OverloadDurationTlv}
 */
public class OverloadedDurationTlvParser implements TlvParser, TlvSerializer {

	private static final int OVERLOADED_DURATION_LENGTH = 4;

	@Override
	public OverloadDurationTlv parseTlv(final byte[] buffer) throws PCEPDeserializerException {
		final long l = ByteArray.bytesToInt(ByteArray.subByte(buffer, 0, OVERLOADED_DURATION_LENGTH));
		return new OverloadDurationBuilder().setDuration(l).build();
	}

	@Override
	public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
		if (tlv == null)
			throw new IllegalArgumentException("OverloadedTlv is mandatory.");
		final OverloadDurationTlv odt = (OverloadDurationTlv) tlv;
		final byte[] bytes = ByteArray.subByte(ByteArray.longToBytes(odt.getDuration()), 4, OVERLOADED_DURATION_LENGTH);
		buffer.writeBytes(bytes);
	}
}
