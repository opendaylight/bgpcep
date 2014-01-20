/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.BandwidthBuilder;

abstract class AbstractBandwidthParser extends AbstractObjectWithTlvsParser<BandwidthBuilder> {

	private static final int BANDWIDTH_F_LENGTH = 4;

	AbstractBandwidthParser(final TlvHandlerRegistry tlvReg) {
		super(tlvReg);
	}

	@Override
	public Bandwidth parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}
		if (bytes.length != BANDWIDTH_F_LENGTH) {
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.length + "; Expected: "
					+ BANDWIDTH_F_LENGTH + ".");
		}
		final BandwidthBuilder builder = new BandwidthBuilder();
		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());
		builder.setBandwidth(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth(bytes));
		return builder.build();
	}

	@Override
	public void addTlv(final BandwidthBuilder builder, final Tlv tlv) {
		// No tlvs defined
	}
}
