/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.opendaylight.protocol.concepts.AbstractMetric;
import org.opendaylight.protocol.concepts.IGPMetric;
import org.opendaylight.protocol.concepts.TEMetric;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.concepts.AggregateBandwidthConsumptionMetric;
import org.opendaylight.protocol.pcep.concepts.CumulativeIGPCostMetric;
import org.opendaylight.protocol.pcep.concepts.CumulativeTECostMetric;
import org.opendaylight.protocol.pcep.concepts.MostLoadedLinkLoadMetric;
import org.opendaylight.protocol.pcep.concepts.P2MPHopCountMetric;
import org.opendaylight.protocol.pcep.concepts.P2MPIGPMetric;
import org.opendaylight.protocol.pcep.concepts.P2MPTEMetric;
import org.opendaylight.protocol.pcep.spi.AbstractObjectParser;
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
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
public class PCEPMetricObjectParser extends AbstractObjectParser<MetricBuilder> {

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

	/**
	 * Bidirectional mapping for metrics. Maps metric class to integer and integer to metrics instantiable.
	 */
	public static class PCEPMetricsMapping {
		private static final PCEPMetricsMapping instance = new PCEPMetricsMapping();

		private final Map<Class<?>, Integer> metricsMap = new HashMap<Class<?>, Integer>();
		private final Map<Integer, InstantiableMetric> metrictTypesMap = new HashMap<Integer, InstantiableMetric>();

		private interface InstantiableMetric {
			public AbstractMetric<?> getMetric(long metric);
		}

		private PCEPMetricsMapping() {
			this.fillIn();
		}

		private void fillIn() {
			this.fillIn(1, IGPMetric.class, new InstantiableMetric() {

				@Override
				public AbstractMetric<?> getMetric(final long metric) {
					return new IGPMetric(metric);
				}

			});
			this.fillIn(2, TEMetric.class, new InstantiableMetric() {

				@Override
				public AbstractMetric<?> getMetric(final long metric) {
					return new TEMetric(metric);
				}

			});
			this.fillIn(4, AggregateBandwidthConsumptionMetric.class, new InstantiableMetric() {

				@Override
				public AbstractMetric<?> getMetric(final long metric) {
					return new AggregateBandwidthConsumptionMetric(metric);
				}

			});
			this.fillIn(5, MostLoadedLinkLoadMetric.class, new InstantiableMetric() {

				@Override
				public AbstractMetric<?> getMetric(final long metric) {
					return new MostLoadedLinkLoadMetric(metric);
				}

			});
			this.fillIn(6, CumulativeIGPCostMetric.class, new InstantiableMetric() {

				@Override
				public AbstractMetric<?> getMetric(final long metric) {
					return new CumulativeIGPCostMetric(metric);
				}

			});
			this.fillIn(7, CumulativeTECostMetric.class, new InstantiableMetric() {

				@Override
				public AbstractMetric<?> getMetric(final long metric) {
					return new CumulativeTECostMetric(metric);
				}

			});
			this.fillIn(8, P2MPIGPMetric.class, new InstantiableMetric() {

				@Override
				public AbstractMetric<?> getMetric(final long metric) {
					return new P2MPIGPMetric(metric);
				}

			});
			this.fillIn(9, P2MPTEMetric.class, new InstantiableMetric() {

				@Override
				public AbstractMetric<?> getMetric(final long metric) {
					return new P2MPHopCountMetric(metric);
				}

			});
			this.fillIn(10, P2MPHopCountMetric.class, new InstantiableMetric() {

				@Override
				public AbstractMetric<?> getMetric(final long metric) {
					return new P2MPHopCountMetric(metric);
				}

			});
		}

		private void fillIn(final int type, final Class<?> metricClazz, final InstantiableMetric instantiable) {
			this.metricsMap.put(metricClazz, type);
			this.metrictTypesMap.put(type, instantiable);
		}

		public int getFromMetricClass(final Class<? extends AbstractMetric<?>> clazz) {
			final Integer mi = this.metricsMap.get(clazz);
			if (mi == null)
				throw new NoSuchElementException("Unknown Metric: " + clazz);
			return mi;
		}

		public AbstractMetric<?> getFromMetricTypeIdentifier(final int identifier, final long metric) {
			final InstantiableMetric e = this.metrictTypesMap.get(identifier);
			if (e == null)
				throw new NoSuchElementException("Unknown metric type identifier. Passed: " + identifier);
			return e.getMetric(metric);
		}

		public static PCEPMetricsMapping getInstance() {
			return instance;
		}
	}

	public PCEPMetricObjectParser(final HandlerRegistry registry) {
		super(registry);
	}

	@Override
	public MetricObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException,
			PCEPDocumentedException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		if (bytes.length != SIZE)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.length + "; Expected: " + SIZE + ".");
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
		if (!(object instanceof MetricObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed MetricObject.");

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
