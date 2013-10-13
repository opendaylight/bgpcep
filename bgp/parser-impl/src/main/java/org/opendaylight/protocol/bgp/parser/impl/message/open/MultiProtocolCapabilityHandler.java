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
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityUtil;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.open.bgp.parameters.c.parameters.CMultiprotocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.open.bgp.parameters.c.parameters.CMultiprotocolBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.open.bgp.parameters.c.parameters.c.multiprotocol.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;

public final class MultiProtocolCapabilityHandler implements CapabilityParser, CapabilitySerializer {
	public static final int CODE = 1;

	private static final int AFI_SIZE = 2; // bytes
	private static final int SAFI_SIZE = 1; // bytes

	private final AddressFamilyRegistry afiReg;
	private final SubsequentAddressFamilyRegistry safiReg;

	public MultiProtocolCapabilityHandler(final AddressFamilyRegistry afiReg, final SubsequentAddressFamilyRegistry safiReg) {
		this.afiReg = Preconditions.checkNotNull(afiReg);
		this.safiReg = Preconditions.checkNotNull(safiReg);
	}

	@Override
	public CMultiprotocol parseCapability(final byte[] bytes) throws BGPDocumentedException, BGPParsingException {
		final int afiVal = ByteArray.bytesToInt(ByteArray.subByte(bytes, 0, AFI_SIZE));
		final Class<? extends AddressFamily> afi = afiReg.classForFamily(afiVal);
		if (afi == null) {
			throw new BGPParsingException("Address Family Identifier: '" + afiVal + "' not supported.");
		}

		final int safiVal = ByteArray.bytesToInt(ByteArray.subByte(bytes, AFI_SIZE + 1, SAFI_SIZE));
		final Class<? extends SubsequentAddressFamily> safi = safiReg.classForFamily(safiVal);
		if (safi == null) {
			throw new BGPParsingException("Subsequent Address Family Identifier: '" + safiVal + "' not supported.");
		}

		return new CMultiprotocolBuilder().setMultiprotocolCapability(
				new MultiprotocolCapabilityBuilder().setAfi(afi).setSafi(safi).build()).build();
	}

	@Override
	public byte[] serializeCapability(final CParameters capability) {
		final CMultiprotocol mp = (CMultiprotocol) capability;

		final Class<? extends AddressFamily> afi = mp.getMultiprotocolCapability().getAfi();
		final Integer afival = afiReg.numberForClass(afi);
		Preconditions.checkArgument(afival != null, "Unhandled address family " + afi);

		final Class<? extends SubsequentAddressFamily> safi = mp.getMultiprotocolCapability().getSafi();
		final Integer safival = safiReg.numberForClass(safi);
		Preconditions.checkArgument(safival != null, "Unhandled subsequent address family " + safi);

		return CapabilityUtil.formatCapability(CODE, new byte[] {
				UnsignedBytes.checkedCast(afival / 256),
				UnsignedBytes.checkedCast(afival % 256),
				0,
				UnsignedBytes.checkedCast(safival)
		});
	}
}