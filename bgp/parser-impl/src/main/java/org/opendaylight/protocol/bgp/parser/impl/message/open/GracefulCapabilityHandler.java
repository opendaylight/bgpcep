/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.open;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityUtil;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.CGracefulRestart;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.CGracefulRestartBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.c.graceful.restart.GracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.c.graceful.restart.GracefulRestartCapability.RestartFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.c.graceful.restart.GracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.c.graceful.restart.graceful.restart.capability.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.c.graceful.restart.graceful.restart.capability.Tables.AfiFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.c.graceful.restart.graceful.restart.capability.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;

public final class GracefulCapabilityHandler implements CapabilityParser, CapabilitySerializer {
	public static final int CODE = 64;

	private static final Logger LOG = LoggerFactory.getLogger(GracefulCapabilityHandler.class);

	// Restart flag size, in bits
	private static final int RESTART_FLAGS_SIZE = 4;
	private static final int RESTART_FLAG_STATE = 0x80;

	// Restart timer size, in bits
	private static final int TIMER_SIZE = 12;
	private static final int TIMER_TOPBITS_MASK = 0x0F;

	// Size of the capability header
	private static final int HEADER_SIZE = (RESTART_FLAGS_SIZE + TIMER_SIZE) / Byte.SIZE;

	// Length of each AFI/SAFI array member, in bytes
	private static final int PER_AFI_SAFI_SIZE = 4;

	private static final int AFI_FLAG_FORWARDING_STATE = 0x80;

	private final AddressFamilyRegistry afiReg;
	private final SubsequentAddressFamilyRegistry safiReg;

	public GracefulCapabilityHandler(final AddressFamilyRegistry afiReg, final SubsequentAddressFamilyRegistry safiReg) {
		this.afiReg = Preconditions.checkNotNull(afiReg);
		this.safiReg = Preconditions.checkNotNull(safiReg);
	}

	@Override
	public byte[] serializeCapability(final CParameters capability) {
		final GracefulRestartCapability grace = ((CGracefulRestart) capability).getGracefulRestartCapability();
		final List<Tables> tables = grace.getTables();

		final byte[] bytes = new byte[HEADER_SIZE + PER_AFI_SAFI_SIZE * tables.size()];

		int flagBits = 0;
		final RestartFlags flags = grace.getRestartFlags();
		if (flags != null) {
			if (flags.isRestartState()) {
				flagBits |= RESTART_FLAG_STATE;
			}
		}

		int timeval = 0;
		final Integer time = grace.getRestartTime();
		if (time != null) {
			Preconditions.checkArgument(time >= 0 && time <= 4095);
			timeval = time;
		}

		bytes[0] = UnsignedBytes.checkedCast(flagBits + timeval / 256);
		bytes[1] = UnsignedBytes.checkedCast(timeval % 256);

		int index = HEADER_SIZE;
		for (final Tables t : tables) {
			final Class<? extends AddressFamily> afi = t.getAfi();
			final Integer afival = this.afiReg.numberForClass(afi);
			Preconditions.checkArgument(afival != null, "Unhandled address family " + afi);

			final Class<? extends SubsequentAddressFamily> safi = t.getSafi();
			final Integer safival = this.safiReg.numberForClass(safi);
			Preconditions.checkArgument(safival != null, "Unhandled subsequent address family " + safi);

			bytes[index] = UnsignedBytes.checkedCast(afival / 256);
			bytes[index + 1] = UnsignedBytes.checkedCast(afival % 256);
			bytes[index + 2] = UnsignedBytes.checkedCast(safival);
			if (t.getAfiFlags().isForwardingState()) {
				bytes[index + 3] = UnsignedBytes.checkedCast(AFI_FLAG_FORWARDING_STATE);
			}

			index += PER_AFI_SAFI_SIZE;
		}

		return CapabilityUtil.formatCapability(CODE, bytes);
	}

	@Override
	public CParameters parseCapability(final byte[] bytes) throws BGPDocumentedException, BGPParsingException {
		final GracefulRestartCapabilityBuilder cb = new GracefulRestartCapabilityBuilder();

		final int flagBits = (bytes[0] >> RESTART_FLAGS_SIZE);
		cb.setRestartFlags(new RestartFlags((flagBits & 8) != 0));

		final int timer = ((bytes[0] & TIMER_TOPBITS_MASK) << RESTART_FLAGS_SIZE) + UnsignedBytes.toInt(bytes[1]);
		cb.setRestartTime(timer);

		final List<Tables> tables = new ArrayList<>();
		for (int offset = HEADER_SIZE; offset < bytes.length; offset += PER_AFI_SAFI_SIZE) {
			final int afiVal = UnsignedBytes.toInt(bytes[offset]) * 256 + UnsignedBytes.toInt(bytes[offset + 1]);
			final Class<? extends AddressFamily> afi = afiReg.classForFamily(afiVal);
			if (afi == null) {
				LOG.debug("Ignoring GR capability for unknown address family {}", afiVal);
				continue;
			}

			final int safiVal = UnsignedBytes.toInt(bytes[offset + 2]);
			final Class<? extends SubsequentAddressFamily> safi = safiReg.classForFamily(safiVal);
			if (safi == null) {
				LOG.debug("Ignoring GR capability for unknown subsequent address family {}", safiVal);
				continue;
			}

			final int flags = UnsignedBytes.toInt(bytes[offset + 3]);
			tables.add(new TablesBuilder().setAfi(afi).setSafi(safi).setAfiFlags(new AfiFlags((flags & AFI_FLAG_FORWARDING_STATE) != 0)).build());
		}

		return new CGracefulRestartBuilder().setGracefulRestartCapability(cb.build()).build();
	}
}
