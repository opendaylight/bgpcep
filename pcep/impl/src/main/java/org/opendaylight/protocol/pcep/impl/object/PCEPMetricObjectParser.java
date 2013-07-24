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
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.concepts.AggregateBandwidthConsumptionMetric;
import org.opendaylight.protocol.pcep.concepts.CumulativeIGPCostMetric;
import org.opendaylight.protocol.pcep.concepts.CumulativeTECostMetric;
import org.opendaylight.protocol.pcep.concepts.MostLoadedLinkLoadMetric;
import org.opendaylight.protocol.pcep.concepts.P2MPHopCountMetric;
import org.opendaylight.protocol.pcep.concepts.P2MPIGPMetric;
import org.opendaylight.protocol.pcep.concepts.P2MPTEMetric;
import org.opendaylight.protocol.pcep.impl.PCEPObjectParser;
import org.opendaylight.protocol.pcep.object.PCEPMetricObject;
import org.opendaylight.protocol.util.ByteArray;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.object.PCEPMetricObject
 * PCEPMetricObject}
 */
public class PCEPMetricObjectParser implements PCEPObjectParser {

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
	 * Bidirectional mapping for metrics. Maps metric class to integer and
	 * integer to metrics instantiable.
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
				public AbstractMetric<?> getMetric(long metric) {
					return new IGPMetric(metric);
				}

			});
			this.fillIn(2, TEMetric.class, new InstantiableMetric() {

				@Override
				public AbstractMetric<?> getMetric(long metric) {
					return new TEMetric(metric);
				}

			});
			this.fillIn(4, AggregateBandwidthConsumptionMetric.class, new InstantiableMetric() {

				@Override
				public AbstractMetric<?> getMetric(long metric) {
					return new AggregateBandwidthConsumptionMetric(metric);
				}

			});
			this.fillIn(5, MostLoadedLinkLoadMetric.class, new InstantiableMetric() {

				@Override
				public AbstractMetric<?> getMetric(long metric) {
					return new MostLoadedLinkLoadMetric(metric);
				}

			});
			this.fillIn(6, CumulativeIGPCostMetric.class, new InstantiableMetric() {

				@Override
				public AbstractMetric<?> getMetric(long metric) {
					return new CumulativeIGPCostMetric(metric);
				}

			});
			this.fillIn(7, CumulativeTECostMetric.class, new InstantiableMetric() {

				@Override
				public AbstractMetric<?> getMetric(long metric) {
					return new CumulativeTECostMetric(metric);
				}

			});
			this.fillIn(8, P2MPIGPMetric.class, new InstantiableMetric() {

				@Override
				public AbstractMetric<?> getMetric(long metric) {
					return new P2MPIGPMetric(metric);
				}

			});
			this.fillIn(9, P2MPTEMetric.class, new InstantiableMetric() {

				@Override
				public AbstractMetric<?> getMetric(long metric) {
					return new P2MPHopCountMetric(metric);
				}

			});
			this.fillIn(10, P2MPHopCountMetric.class, new InstantiableMetric() {

				@Override
				public AbstractMetric<?> getMetric(long metric) {
					return new P2MPHopCountMetric(metric);
				}

			});
		}

		private void fillIn(int type, Class<?> metricClazz, InstantiableMetric instantiable) {
			this.metricsMap.put(metricClazz, type);
			this.metrictTypesMap.put(type, instantiable);
		}

		public int getFromMetricClass(Class<? extends AbstractMetric<?>> clazz) {
			final Integer mi = this.metricsMap.get(clazz);
			if (mi == null)
				throw new NoSuchElementException("Unknown Metric: " + clazz);
			return mi;
		}

		public AbstractMetric<?> getFromMetricTypeIdentifier(int identifier, long metric) {
			final InstantiableMetric e = this.metrictTypesMap.get(identifier);
			if (e == null)
				throw new NoSuchElementException("Unknown metric type identifier. Passed: " + identifier);
			return e.getMetric(metric);
		}

		public static PCEPMetricsMapping getInstance() {
			return instance;
		}
	}

	@Override
	public PCEPObject parse(byte[] bytes, boolean processed, boolean ignored) throws PCEPDeserializerException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");

		if (bytes.length != SIZE)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.length + "; Expected: " + SIZE + ".");

		final byte[] flagBytes = { bytes[FLAGS_F_OFFSET] };
		final BitSet flags = ByteArray.bytesToBitSet(flagBytes);
		try {
			return new PCEPMetricObject(flags.get(B_FLAG_OFFSET), flags.get(C_FLAG_OFFSET), PCEPMetricsMapping.getInstance().getFromMetricTypeIdentifier(
					(short) (bytes[TYPE_F_OFFSET] & 0xFF),
					(long) ByteArray.bytesToFloat(ByteArray.subByte(bytes, METRIC_VALUE_F_OFFSET, METRIC_VALUE_F_LENGTH))), processed, ignored);
		} catch (final NoSuchElementException e) {
			throw new PCEPDeserializerException(e, "Metric object has unknown identifier.");
		}
	}

	@Override
	public byte[] put(PCEPObject obj) {
		if (!(obj instanceof PCEPMetricObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass() + ". Needed PCEPMetricObject.");

		final PCEPMetricObject mObj = (PCEPMetricObject) obj;

		final byte[] retBytes = new byte[SIZE];
		final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
		flags.set(C_FLAG_OFFSET, mObj.isComputedMetric());
		flags.set(B_FLAG_OFFSET, mObj.isBound());

		ByteArray.copyWhole(ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH), retBytes, FLAGS_F_OFFSET);

		final AbstractMetric<?> metric = mObj.getMetric();
		@SuppressWarnings("unchecked")
		final Class<? extends AbstractMetric<?>> metricClazz = (Class<? extends AbstractMetric<?>>) metric.getClass();
		retBytes[TYPE_F_OFFSET] = (byte) PCEPMetricsMapping.getInstance().getFromMetricClass(metricClazz);

		System.arraycopy(ByteArray.floatToBytes(mObj.getMetric().getValue()), 0, retBytes, METRIC_VALUE_F_OFFSET, METRIC_VALUE_F_LENGTH);

		return retBytes;
	}

}
