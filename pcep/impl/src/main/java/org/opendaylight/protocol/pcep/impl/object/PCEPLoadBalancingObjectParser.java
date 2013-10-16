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
import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.LoadBalancingObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.segment.computation.p2p.LoadBalancingBuilder;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.object.PCEPLoadBalancingObject PCEPLoadBalancingObject}
 */
public class PCEPLoadBalancingObjectParser extends AbstractObjectWithTlvsParser<LoadBalancingBuilder> {

	public static final int CLASS = 14;

	public static final int TYPE = 1;

	public static final int FLAGS_F_LENGTH = 1;
	public static final int MAX_LSP_F_LENGTH = 1;
	public static final int MIN_BAND_F_LENGTH = 4;

	public static final int FLAGS_F_OFFSET = 2;
	public static final int MAX_LSP_F_OFFSET = FLAGS_F_OFFSET + FLAGS_F_LENGTH;
	public static final int MIN_BAND_F_OFFSET = MAX_LSP_F_OFFSET + MAX_LSP_F_LENGTH;

	public static final int SIZE = MIN_BAND_F_OFFSET + MIN_BAND_F_LENGTH;

	public PCEPLoadBalancingObjectParser(final TlvHandlerRegistry tlvReg) {
		super(tlvReg);
	}

	@Override
	public LoadBalancingObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException,
	PCEPDocumentedException {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("Byte array is mandatory. Can't be null or empty.");
		}

		if (bytes.length != SIZE) {
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.length + "; Expected: " + SIZE + ".");
		}

		final LoadBalancingBuilder builder = new LoadBalancingBuilder();

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());

		builder.setMaxLsp((short) (bytes[MAX_LSP_F_OFFSET] & 0xFF));
		builder.setMinBandwidth(new Float32(ByteArray.subByte(bytes, MIN_BAND_F_OFFSET, MIN_BAND_F_LENGTH)));

		return builder.build();
	}

	@Override
	public void addTlv(final LoadBalancingBuilder builder, final Tlv tlv) {
		// No tlvs defined
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof LoadBalancingObject)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass()
					+ ". Needed LoadBalancingObject.");
		}

		final LoadBalancingObject specObj = (LoadBalancingObject) object;

		final byte[] retBytes = new byte[SIZE];

		retBytes[MAX_LSP_F_OFFSET] = ByteArray.shortToBytes(specObj.getMaxLsp())[1];
		ByteArray.copyWhole(specObj.getMinBandwidth().getValue(), retBytes, MIN_BAND_F_OFFSET);

		return retBytes;
	}

	@Override
	public int getObjectType() {
		return TYPE;
	}

	@Override
	public int getObjectClass() {
		return CLASS;
	}
}
