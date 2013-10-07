/**
 * 
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.notification.object.tlvs.OverloadDuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.notification.object.tlvs.OverloadDurationBuilder;

public class OverloadedDurationTlvParser implements TlvParser {

	private static final int OVERLOADED_DURATION_LENGTH = 4;
	
	@Override
	public OverloadDuration parseTlv(byte[] buffer) throws PCEPDeserializerException {
		long l = ByteArray.bytesToInt(ByteArray.subByte(buffer, 0, OVERLOADED_DURATION_LENGTH));
		return new OverloadDurationBuilder().setDuration(l).build();
	}
}
