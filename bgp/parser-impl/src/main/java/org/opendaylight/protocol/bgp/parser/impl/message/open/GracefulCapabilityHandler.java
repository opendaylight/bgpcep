/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.open;

import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.CParameters;

final class GracefulCapabilityHandler implements CapabilitySerializer {
	static final int CODE = 64;

	private static final int RESTART_FLAGS_SIZE = 4; // bits
	private static final int TIMER_SIZE = 12; // bits
	private static final int AFI_SIZE = 2; // bytes
	private static final int SAFI_SIZE = 1; // bytes
	private static final int AF_FLAGS_SIZE = 1; // bytes

	@Override
	public byte[] serializeCapability(final CParameters capability) {
		final byte[] bytes = null;

		// final GracefulCapability param = (GracefulCapability) capability;
		// final byte[] bytes = new byte[(RESTART_FLAGS_SIZE + TIMER_SIZE + (AFI_SIZE * Byte.SIZE + SAFI_SIZE *
		// Byte.SIZE +
		// AF_FLAGS_SIZE
		// * Byte.SIZE)
		// * param.getTableTypes().size())
		// / Byte.SIZE];
		// if (param.isRestartFlag()) {
		// bytes[0] = (byte) 0x80;
		// }
		// int index = (RESTART_FLAGS_SIZE + TIMER_SIZE) / Byte.SIZE;
		// for (final Entry<BGPTableType, Boolean> entry : param.getTableTypes().entrySet()) {
		// final byte[] a = putAfi(entry.getKey().getAddressFamily());
		// final byte s = putSafi(entry.getKey().getSubsequentAddressFamily());
		// final byte f = (entry.getValue()) ? (byte) 0x80 : (byte) 0x00;
		// System.arraycopy(a, 0, bytes, index, AFI_SIZE);
		// index += AFI_SIZE;
		// bytes[index] = s;
		// index += SAFI_SIZE;
		// bytes[index] = f;
		// index += AF_FLAGS_SIZE;
		// }

		return CapabilityUtil.formatCapability(CODE, bytes);
	}

}
