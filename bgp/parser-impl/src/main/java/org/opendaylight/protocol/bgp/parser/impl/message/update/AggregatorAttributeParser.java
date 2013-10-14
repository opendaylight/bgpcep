package org.opendaylight.protocol.bgp.parser.impl.message.update;

import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.Aggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.AggregatorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributesBuilder;

final class AggregatorAttributeParser implements AttributeParser {
	static final int TYPE = 7;

	/**
	 * Parse AGGREGATOR from bytes
	 * 
	 * @param bytes byte array to be parsed
	 * @return {@link Aggregator} BGP Aggregator
	 */
	private static Aggregator parseAggregator(final byte[] bytes) {
		final AsNumber asNumber = new AsNumber(ByteArray.bytesToLong(ByteArray.subByte(bytes, 0, AsPathSegmentParser.AS_NUMBER_LENGTH)));
		final Ipv4Address address = Ipv4Util.addressForBytes(ByteArray.subByte(bytes, AsPathSegmentParser.AS_NUMBER_LENGTH, 4));
		return new AggregatorBuilder().setAsNumber(asNumber).setNetworkAddress(address).build();
	}

	@Override
	public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) {
		builder.setAggregator(parseAggregator(bytes));
	}
}