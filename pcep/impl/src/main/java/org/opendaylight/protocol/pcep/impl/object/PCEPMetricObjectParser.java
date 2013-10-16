/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import java.util.BitSet;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.impl.message.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.MetricObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.svec.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.svec.MetricBuilder;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.object.PCEPMetricObject PCEPMetricObject}
 */
public class PCEPMetricObjectParser extends AbstractObjectWithTlvsParser<MetricBuilder> {

	public static final int CLASS = 6;

	public static final int TYPE = 1;

	/*
	 * lengths of fields in bytes
	 */
	private static final int FLAGS_F_LENGTH = 1;
	private static final int TYPE_F_LENGTH = 1;
	private static final int METRIC_VALUE_F_LENGTH = 4;

	/*
	 * offsets of fields in bytes
	 */
	public static final int FLAGS_F_OFFSET = 2;
	public static final int TYPE_F_OFFSET = FLAGS_F_OFFSET + FLAGS_F_LENGTH;
	public static final int METRIC_VALUE_F_OFFSET = TYPE_F_OFFSET + TYPE_F_LENGTH;

	/*
	 * flags offsets inside flags field in bits
	 */
	private static final int C_FLAG_OFFSET = 6;
	private static final int B_FLAG_OFFSET = 7;

	public static final int SIZE = METRIC_VALUE_F_OFFSET + METRIC_VALUE_F_LENGTH;

	public PCEPMetricObjectParser(final TlvHandlerRegistry tlvReg) {
		super(tlvReg);
	}

	@Override
	public MetricObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException,
	PCEPDocumentedException {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}
		if (bytes.length != SIZE) {
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.length + "; Expected: " + SIZE + ".");
		}
		final byte[] flagBytes = { bytes[FLAGS_F_OFFSET] };
		final BitSet flags = ByteArray.bytesToBitSet(flagBytes);

		final MetricBuilder builder = new MetricBuilder();

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());

		builder.setBound(flags.get(B_FLAG_OFFSET));
		builder.setComputed(flags.get(C_FLAG_OFFSET));
		builder.setMetricType((short) (bytes[TYPE_F_OFFSET] & 0xFF));
		builder.setValue(new Float32(ByteArray.subByte(bytes, METRIC_VALUE_F_OFFSET, METRIC_VALUE_F_LENGTH)));

		return builder.build();
	}

	@Override
	public void addTlv(final MetricBuilder builder, final Tlv tlv) {
		// No tlvs defined
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof MetricObject)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed MetricObject.");
		}

		final MetricObject mObj = (MetricObject) object;

		final byte[] retBytes = new byte[SIZE];
		final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
		flags.set(C_FLAG_OFFSET, ((Metric) mObj).isComputed());
		flags.set(B_FLAG_OFFSET, mObj.isBound());

		ByteArray.copyWhole(ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH), retBytes, FLAGS_F_OFFSET);

		System.arraycopy(mObj.getValue().getValue(), 0, retBytes, METRIC_VALUE_F_OFFSET, METRIC_VALUE_F_LENGTH);

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
