/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriRegistry;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;

public final class SimpleNlriRegistry implements NlriRegistry {
	private static final class Holder {
		private static final NlriRegistry INSTANCE =
				new SimpleNlriRegistry(SimpleAddressFamilyRegistry.getInstance(),
						SimpleSubsequentAddressFamilyRegistry.getInstance());
	}

	private final ConcurrentMap<BgpTableType, NlriParser> handlers = new ConcurrentHashMap<>();
	private final SubsequentAddressFamilyRegistry safiReg;
	private final AddressFamilyRegistry afiReg;

	private SimpleNlriRegistry(final AddressFamilyRegistry afiReg, final SubsequentAddressFamilyRegistry safiReg) {
		this.afiReg = Preconditions.checkNotNull(afiReg);
		this.safiReg = Preconditions.checkNotNull(safiReg);
	}

	public static NlriRegistry getInstance() {
		return Holder.INSTANCE;
	}

	private static BgpTableType createKey(final Class<? extends AddressFamily> afi,
			final Class<? extends SubsequentAddressFamily> safi) {
		Preconditions.checkNotNull(afi);
		Preconditions.checkNotNull(safi);
		return new BgpTableTypeImpl(afi, safi);
	}

	@Override
	public synchronized AutoCloseable registerNlriParser(final Class<? extends AddressFamily> afi,
			final Class<? extends SubsequentAddressFamily> safi, final NlriParser parser) {
		final BgpTableType key = createKey(afi, safi);
		final NlriParser prev = handlers.get(key);
		Preconditions.checkState(prev == null, "AFI/SAFI is already bound to parser " + prev);

		handlers.put(key, parser);
		final Object lock = this;
		return new AbstractRegistration() {
			@Override
			protected void removeRegistration() {
				synchronized (lock) {
					handlers.remove(key);
				}
			}
		};
	}

	private Class<? extends AddressFamily> getAfi(final byte[] header) throws BGPParsingException {
		final int afiVal = UnsignedBytes.toInt(header[0]) * 256 + UnsignedBytes.toInt(header[1]);
		final Class<? extends AddressFamily> afi = afiReg.classForFamily(afiVal);
		if (afi == null) {
			throw new BGPParsingException("Address Family Identifier: '" + afiVal + "' not supported.");
		}

		return afi;
	}

	private Class<? extends SubsequentAddressFamily> getSafi(final byte[] header) throws BGPParsingException {
		final int safiVal = UnsignedBytes.toInt(header[2]);
		final Class<? extends SubsequentAddressFamily> safi = safiReg.classForFamily(safiVal);
		if (safi == null) {
			throw new BGPParsingException("Subsequent Address Family Identifier: '" + safiVal + "' not supported.");
		}

		return safi;
	}

	@Override
	public MpUnreachNlri parseMpUnreach(final byte[] bytes) throws BGPParsingException {
		final MpUnreachNlriBuilder builder = new MpUnreachNlriBuilder();
		builder.setAfi(getAfi(bytes));
		builder.setSafi(getSafi(bytes));

		final NlriParser parser = handlers.get(createKey(builder.getAfi(), builder.getSafi()));
		parser.parseNlri(ByteArray.subByte(bytes, 3, bytes.length - 3), builder);

		return builder.build();
	}

	@Override
	public MpReachNlri parseMpReach(final byte[] bytes) throws BGPParsingException {
		final MpReachNlriBuilder builder = new MpReachNlriBuilder();
		builder.setAfi(getAfi(bytes));
		builder.setSafi(getSafi(bytes));

		final NlriParser parser = handlers.get(createKey(builder.getAfi(), builder.getSafi()));

		final int nextHopLength = UnsignedBytes.toInt(bytes[3]);
		int byteOffset = 4;

		final byte[] nextHop = ByteArray.subByte(bytes, byteOffset, nextHopLength);
		byteOffset += nextHopLength + 1;

		final byte[] nlri = ByteArray.subByte(bytes, byteOffset, bytes.length - byteOffset);
		parser.parseNlri(nlri, nextHop, builder);

		return builder.build();
	}
}
