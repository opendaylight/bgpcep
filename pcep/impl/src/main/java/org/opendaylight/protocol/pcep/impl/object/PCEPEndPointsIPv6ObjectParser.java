/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import org.opendaylight.protocol.concepts.IPv6Address;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.object.PCEPEndPointsObject;
import org.opendaylight.protocol.pcep.spi.AbstractObjectParser;
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.EndpointsObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.address.family.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcinitiate.message.pcinitiate.message.requests.EndpointsBuilder;

/**
 * Parser for IPv6 {@link EndpointsObject}
 */
public class PCEPEndPointsIPv6ObjectParser extends AbstractObjectParser<EndpointsBuilder> {

	public static final int SRC6_F_LENGTH = 16;
	public static final int DEST6_F_LENGTH = 16;

	public static final int SRC6_F_OFFSET = 0;
	public static final int DEST6_F_OFFSET = SRC6_F_OFFSET + SRC6_F_LENGTH;

	public PCEPEndPointsIPv6ObjectParser(final HandlerRegistry registry) {
		super(registry);
	}

	@Override
	public EndpointsObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException,
			PCEPDocumentedException {
		if (bytes == null)
			throw new IllegalArgumentException("Array of bytes is mandatory");
		if (bytes.length != SRC6_F_LENGTH + DEST6_F_LENGTH)
			throw new PCEPDeserializerException("Wrong length of array of bytes.");

		if (!header.isProcessingRule())
			throw new PCEPDocumentedException("Processed flag not set", PCEPErrors.P_FLAG_NOT_SET);

		final EndpointsBuilder builder = new EndpointsBuilder();

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());

		final Ipv6Builder b = new Ipv6Builder();
		b.setSourceIpv6Address(Ipv6Util.addressForBytes(ByteArray.subByte(bytes, SRC6_F_OFFSET, SRC6_F_LENGTH)));
		b.setDestinationIpv6Address((Ipv6Util.addressForBytes(ByteArray.subByte(bytes, DEST6_F_OFFSET, DEST6_F_LENGTH))));
		builder.setAddressFamily(b.build());

		return builder.build();
	}

	@Override
	public void addTlv(final EndpointsBuilder builder, final Tlv tlv) {
		// No tlvs defined
	}

	public byte[] put(final PCEPObject obj) {
		if (!(obj instanceof PCEPEndPointsObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass() + ". Needed PCEPEndPointsObject.");

		final PCEPEndPointsObject<?> ePObj = (PCEPEndPointsObject<?>) obj;

		if (!(ePObj.getSourceAddress() instanceof IPv6Address))
			throw new IllegalArgumentException("Wrong instance of NetworkAddress. Passed " + ePObj.getSourceAddress().getClass()
					+ ". Needed IPv6Address");

		final byte[] retBytes = new byte[SRC6_F_LENGTH + DEST6_F_LENGTH];
		ByteArray.copyWhole(((IPv6Address) ePObj.getSourceAddress()).getAddress(), retBytes, SRC6_F_OFFSET);
		ByteArray.copyWhole(((IPv6Address) ePObj.getDestinationAddress()).getAddress(), retBytes, DEST6_F_OFFSET);

		return retBytes;
	}
}
