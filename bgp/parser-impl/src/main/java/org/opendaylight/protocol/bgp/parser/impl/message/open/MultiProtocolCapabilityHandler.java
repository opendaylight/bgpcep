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
import org.opendaylight.protocol.bgp.parser.impl.ParserUtil;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.open.bgp.parameters.c.parameters.CMultiprotocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.open.bgp.parameters.c.parameters.CMultiprotocolBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.open.bgp.parameters.c.parameters.c.multiprotocol.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;

import com.google.common.primitives.UnsignedBytes;

final class MultiProtocolCapabilityHandler implements CapabilityParser, CapabilitySerializer {
	static final int CODE = 1;

	private static final int AFI_SIZE = 2; // bytes
	private static final int SAFI_SIZE = 1; // bytes

	@Override
	public CMultiprotocol parseCapability(final byte[] bytes) throws BGPDocumentedException, BGPParsingException {
		final Class<? extends AddressFamily> afi = ParserUtil.afiForValue(ByteArray.bytesToInt(ByteArray.subByte(bytes, 0, AFI_SIZE)));
		if (afi == null) {
			throw new BGPParsingException("Address Family Identifier: '" + ByteArray.bytesToInt(ByteArray.subByte(bytes, 0, AFI_SIZE))
					+ "' not supported.");
		}
		final Class<? extends SubsequentAddressFamily> safi = ParserUtil.safiForValue(ByteArray.bytesToInt(ByteArray.subByte(bytes,
				AFI_SIZE + 1, SAFI_SIZE)));
		if (safi == null) {
			throw new BGPParsingException("Subsequent Address Family Identifier: '"
					+ ByteArray.bytesToInt(ByteArray.subByte(bytes, AFI_SIZE + 1, SAFI_SIZE)) + "' not supported.");
		}

		return new CMultiprotocolBuilder().setMultiprotocolCapability(
				new MultiprotocolCapabilityBuilder().setAfi(afi).setSafi(safi).build()).build();
	}

	@Override
	public byte[] serializeCapability(final CParameters capability) {
		final CMultiprotocol mp = (CMultiprotocol) capability;

		final int afival = ParserUtil.valueForAfi(mp.getMultiprotocolCapability().getAfi());
		final int safival = ParserUtil.valueForSafi(mp.getMultiprotocolCapability().getSafi());

		return CapabilityUtil.formatCapability(CODE, new byte[] {
				UnsignedBytes.checkedCast(afival / 256),
				UnsignedBytes.checkedCast(afival % 256),
				0,
				UnsignedBytes.checkedCast(safival)
		});
	}
}