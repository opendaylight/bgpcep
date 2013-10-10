package org.opendaylight.protocol.bgp.parser.impl.message;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageSerializer;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.KeepaliveBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;

import com.google.common.base.Preconditions;

public class BGPKeepAliveMessageParser implements MessageParser, MessageSerializer {
	private static final Keepalive msg = new KeepaliveBuilder().build();
	private static final byte[] bytes = MessageUtil.formatMessage(4, new byte[0]);

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
	public Keepalive parseMessageBody(final byte[] body, final int messageLength) throws BGPDocumentedException {
		if (body.length != 0) {
			throw  BGPDocumentedException.badMessageLength("Message length field not within valid range.", messageLength);
		}

		return msg;
	}

	@Override
	public byte[] serializeMessage(final Notification message) {
		Preconditions.checkArgument(message instanceof Keepalive);
		return bytes;
	}
}
