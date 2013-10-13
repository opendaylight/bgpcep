/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.open;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.c.parameters.CAs4Bytes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.c.parameters.CAs4BytesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.c.parameters.c.as4.bytes.As4BytesCapabilityBuilder;

public final class As4CapabilityHandler implements CapabilityParser, CapabilitySerializer {
	public static final int CODE = 65;

	@Override
	public CParameters parseCapability(final byte[] bytes) throws BGPDocumentedException, BGPParsingException {
		return new CAs4BytesBuilder().setAs4BytesCapability(
				new As4BytesCapabilityBuilder().setAsNumber(new AsNumber(ByteArray.bytesToLong(bytes))).build()).build();

	}

	@Override
	public byte[] serializeCapability(final CParameters capability) {
		return CapabilityUtil.formatCapability(CODE, putAS4BytesParameterValue((CAs4Bytes) capability));
	}

	private static byte[] putAS4BytesParameterValue(final CAs4Bytes param) {
		return ByteArray.subByte(ByteArray.longToBytes(param.getAs4BytesCapability().getAsNumber().getValue()), 4, 4);
	}
}