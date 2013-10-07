/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.object.PCEPRequestedPathBandwidthObject;
import org.opendaylight.protocol.pcep.spi.AbstractObjectParser;
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.BandwidthObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.segment.computation.p2p.reported.route.BandwidthBuilder;

/**
 * Parser for {@link BandwidthObject}
 */
public class PCEPBandwidthObjectParser extends AbstractObjectParser<BandwidthBuilder> {

	private static final int BANDWIDTH_F_LENGTH = 4;

	public PCEPBandwidthObjectParser(final HandlerRegistry registry) {
		super(registry);
	}

	@Override
	public BandwidthObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException,
			PCEPDocumentedException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		if (bytes.length != BANDWIDTH_F_LENGTH)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.length + "; Expected: "
					+ BANDWIDTH_F_LENGTH + ".");

		final BandwidthBuilder builder = new BandwidthBuilder();

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());

		builder.setBandwidth(new Float32(bytes));

		return builder.build();
	}

	@Override
	public void addTlv(final BandwidthBuilder builder, final Tlv tlv) {
		// No tlvs defined
	}

	public byte[] put(final PCEPObject obj) {
		if (!(obj instanceof PCEPRequestedPathBandwidthObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass()
					+ ". Needed PCEPRequestedPathBandwidthObject.");

		return ((PCEPRequestedPathBandwidthObject) obj).getBandwidth().getValue();
	}
}
