package org.opendaylight.protocol.bgp.parser.impl.message;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.impl.BGPMessageFactoryImpl;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.KeepaliveBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;

import com.google.common.base.Preconditions;

public class BGPKeepAliveMessageParser implements MessageParser, MessageSerializer {
	private static final Keepalive msg = new KeepaliveBuilder().build();
	private static final byte[] bytes = new byte[0];

	public static final MessageParser PARSER;
	public static final MessageSerializer SERIALIZER;

	static {
		final BGPKeepAliveMessageParser p = new BGPKeepAliveMessageParser();
		PARSER = p;
		SERIALIZER = p;
	}

	private BGPKeepAliveMessageParser() {

	}

	@Override
	public Keepalive parseMessage(final byte[] bytes, final int messageLength) throws BGPDocumentedException {
		if (messageLength != BGPMessageFactoryImpl.COMMON_HEADER_LENGTH) {
			throw new BGPDocumentedException("Message length field not within valid range.",
					BGPError.BAD_MSG_LENGTH,  ByteArray.subByte(ByteArray.intToBytes(messageLength),
							4 - BGPMessageFactoryImpl.LENGTH_FIELD_LENGTH,
							BGPMessageFactoryImpl.LENGTH_FIELD_LENGTH));
		}

		return msg;
	}

	@Override
	public int messageType() {
		return 4;
	}

	@Override
	public byte[] serializeMessage(final Notification message) {
		Preconditions.checkArgument(message instanceof Keepalive);
		return bytes;
	}
}
