/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.object;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPObject;

import com.google.common.base.Preconditions;

/**
 * Structure that combines set of related objects.
 * 
 * @see <a href="http://www.ietf.org/id/draft-crabbe-pce-pce-initiated-lsp-00.txt">PCCreate Message</a>
 */
public class CompositeInstantiationObject {

	private final PCEPEndPointsObject<?> endPoints;

	private final PCEPLspaObject lspa;

	private final PCEPExplicitRouteObject ero;

	private final PCEPRequestedPathBandwidthObject bandwidth;

	private final List<PCEPMetricObject> metrics;

	/**
	 * Constructs basic composite object only with mandatory objects.
	 * 
	 * @param endPoints PCEPEndPointsObject<?>. Can't be null.
	 * @param lspa PCEPLspaObject. Can't be null.
	 */
	public CompositeInstantiationObject(final PCEPEndPointsObject<?> endPoints, final PCEPLspaObject lspa) {
		this(endPoints, lspa, null, null, null);
	}

	/**
	 * Constructs composite object with optional objects.
	 * 
	 * @param endPoints PCEPEndPointsObject<?>. Can't be null.
	 * @param lspa PCEPLspaObject. Can't be null.
	 * @param ero PCEPExplicitRouteObject
	 * @param bandwidth PCEPRequestedPathBandwidthObject
	 * @param metrics List<PCEPMetricObject>
	 */
	public CompositeInstantiationObject(final PCEPEndPointsObject<?> endPoints, final PCEPLspaObject lspa,
			final PCEPExplicitRouteObject ero, final PCEPRequestedPathBandwidthObject bandwidth, final List<PCEPMetricObject> metrics) {
		this.endPoints = Preconditions.checkNotNull(endPoints);
		this.lspa = Preconditions.checkNotNull(lspa);
		this.ero = ero;
		this.bandwidth = bandwidth;
		this.metrics = metrics;
	}

	/**
	 * Gets list of all objects, which are in appropriate order.
	 * 
	 * @return List<PCEPObject>. Can't be null or empty.
	 */
	public List<PCEPObject> getCompositeAsList() {
		final List<PCEPObject> list = new ArrayList<PCEPObject>();
		list.add(this.endPoints);
		list.add(this.lspa);
		if (this.ero != null)
			list.add(this.ero);
		if (this.bandwidth != null)
			list.add(this.bandwidth);
		if (this.metrics != null && !this.metrics.isEmpty())
			list.addAll(this.metrics);
		return list;
	}

	/**
	 * Creates this object from a list of PCEPObjects.
	 * 
	 * @param objects List<PCEPObject> list of PCEPObjects from whose this object should be created.
	 * @return CompositeInstantiationObject
	 */
	public static CompositeInstantiationObject getCompositeFromList(final List<PCEPObject> objects) {
		if (objects == null || objects.isEmpty()) {
			throw new IllegalArgumentException("List cannot be null or empty.");
		}

		PCEPEndPointsObject<?> endPoints = null;
		if (objects.get(0) instanceof PCEPEndPointsObject<?>) {
			endPoints = (PCEPEndPointsObject<?>) objects.get(0);
			objects.remove(endPoints);
		} else
			throw new IllegalArgumentException("End Points object must be first.");

		PCEPLspaObject lspa = null;
		if (objects.get(0) instanceof PCEPLspaObject) {
			lspa = (PCEPLspaObject) objects.get(0);
			objects.remove(lspa);
		} else
			throw new IllegalArgumentException("LSPA object must be second.");

		PCEPExplicitRouteObject ero = null;
		PCEPRequestedPathBandwidthObject bandwidth = null;
		final List<PCEPMetricObject> metrics = new ArrayList<PCEPMetricObject>();

		int state = 1;
		while (!objects.isEmpty()) {
			final PCEPObject obj = objects.get(0);
			switch (state) {
			case 1:
				state = 2;
				if (obj instanceof PCEPExplicitRouteObject) {
					ero = (PCEPExplicitRouteObject) obj;
					break;
				}
			case 2:
				state = 3;
				if (obj instanceof PCEPRequestedPathBandwidthObject) {
					bandwidth = (PCEPRequestedPathBandwidthObject) obj;
					break;
				}
			case 3:
				state = 4;
				if (obj instanceof PCEPMetricObject) {
					metrics.add((PCEPMetricObject) obj);
					state = 3;

					break;
				}
			}

			if (state == 4) {
				break;
			}

			objects.remove(obj);
		}

		return new CompositeInstantiationObject(endPoints, lspa, ero, bandwidth, metrics);
	}

	/**
	 * @return the endPoints
	 */
	public final PCEPEndPointsObject<?> getEndPoints() {
		return this.endPoints;
	}

	/**
	 * @return the lspa
	 */
	public final PCEPLspaObject getLspa() {
		return this.lspa;
	}

	/**
	 * @return the ero
	 */
	public final PCEPExplicitRouteObject getEro() {
		return this.ero;
	}

	/**
	 * @return the bandwidth
	 */
	public final PCEPRequestedPathBandwidthObject getBandwidth() {
		return this.bandwidth;
	}

	/**
	 * @return the metrics
	 */
	public final List<PCEPMetricObject> getMetrics() {
		return this.metrics;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.bandwidth == null) ? 0 : this.bandwidth.hashCode());
		result = prime * result + ((this.endPoints == null) ? 0 : this.endPoints.hashCode());
		result = prime * result + ((this.ero == null) ? 0 : this.ero.hashCode());
		result = prime * result + ((this.lspa == null) ? 0 : this.lspa.hashCode());
		result = prime * result + ((this.metrics == null) ? 0 : this.metrics.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof CompositeInstantiationObject))
			return false;
		final CompositeInstantiationObject other = (CompositeInstantiationObject) obj;
		if (this.bandwidth == null) {
			if (other.bandwidth != null)
				return false;
		} else if (!this.bandwidth.equals(other.bandwidth))
			return false;
		if (this.endPoints == null) {
			if (other.endPoints != null)
				return false;
		} else if (!this.endPoints.equals(other.endPoints))
			return false;
		if (this.ero == null) {
			if (other.ero != null)
				return false;
		} else if (!this.ero.equals(other.ero))
			return false;
		if (this.lspa == null) {
			if (other.lspa != null)
				return false;
		} else if (!this.lspa.equals(other.lspa))
			return false;
		if (this.metrics == null) {
			if (other.metrics != null)
				return false;
		} else if (!this.metrics.equals(other.metrics))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("CompositeInstantiationObject [endPoints=");
		builder.append(this.endPoints);
		builder.append(", lspa=");
		builder.append(this.lspa);
		builder.append(", ero=");
		builder.append(this.ero);
		builder.append(", bandwidth=");
		builder.append(this.bandwidth);
		builder.append(", metrics=");
		builder.append(this.metrics);
		builder.append("]");
		return builder.toString();
	}
}
