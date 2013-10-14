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
import org.opendaylight.protocol.pcep.spi.AbstractObjectParser;
import org.opendaylight.protocol.pcep.spi.SubobjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.BandwidthObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.segment.computation.p2p.reported.route.BandwidthBuilder;

/**
 * Parser for {@link BandwidthObject}
 */
public class PCEPBandwidthObjectParser extends AbstractObjectParser<BandwidthBuilder> {

	public static final int E_CLASS = 5;

	public static final int E_TYPE = 1;

	public static final int CLASS = 5;

	public static final int TYPE = 2;

	private static final int BANDWIDTH_F_LENGTH = 4;

	public PCEPBandwidthObjectParser(final SubobjectHandlerRegistry subobjReg, final TlvHandlerRegistry tlvReg) {
		super(subobjReg, tlvReg);
	}

	@Override
	public BandwidthObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException,
	PCEPDocumentedException {
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

		builder.setBandwidth(new Float32(bytes));

		return builder.build();
	}

	@Override
	public void addTlv(final BandwidthBuilder builder, final Tlv tlv) {
		// No tlvs defined
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof BandwidthObject)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed BandwidthObject.");
		}

		return ((BandwidthObject) object).getBandwidth().getValue();
	}

	@Override
	public int getObjectType() {
		return TYPE;
	}

	@Override
	public int getObjectClass() {
		return CLASS;
	}

	public int getEObjectType() {
		return E_TYPE;
	}

	public int getEObjectClass() {
		return E_CLASS;
	}
}
