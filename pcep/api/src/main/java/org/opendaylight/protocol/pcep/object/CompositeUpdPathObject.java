/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.object;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPObject;

/**
 * Structure that combines set of related objects.
 *
 * @see draft-ietf-pce-stateful-pce-01 (sec. 6.2) - The PCUpd Message -
 *      &lt;path&gt;</br>
 */
public class CompositeUpdPathObject {

	private final PCEPExplicitRouteObject explicitRoute;

	private final PCEPLspaObject lspa;

	private final PCEPRequestedPathBandwidthObject bandwidth;

	private List<PCEPMetricObject> metrics;

	/**
	 * Constructs basic composite object only with mandatory objects.
	 * @param explicitRoute PCEPExplicitRouteObject
	 */
	public CompositeUpdPathObject(PCEPExplicitRouteObject explicitRoute) {
		this(explicitRoute, null, null, null);
	}

	/**
	 * Constructs composite object with optional objects.
	 * @param explicitRoute PCEPExplicitRouteObject
	 * @param lspa PCEPLspaObject
	 * @param bandwidth PCEPRequestedPathBandwidthObject
	 * @param metrics List<PCEPMetricObject>
	 */
	public CompositeUpdPathObject(PCEPExplicitRouteObject explicitRoute, PCEPLspaObject lspa, PCEPRequestedPathBandwidthObject bandwidth,
			List<PCEPMetricObject> metrics) {
		if (explicitRoute == null)
			throw new IllegalArgumentException("Explicit Route Object is mandatory.");
		this.explicitRoute = explicitRoute;
		this.lspa = lspa;
		this.bandwidth = bandwidth;
		if (metrics != null)
			this.metrics = metrics;
		else
			this.metrics = Collections.emptyList();
	}

	/**
	 * Gets list of all objects, which are in appropriate order.
	 * @return List<PCEPObject>
	 */
	public List<PCEPObject> getCompositeAsList() {
		final List<PCEPObject> list = new ArrayList<PCEPObject>();
		list.add(this.explicitRoute);
		if (this.lspa != null)
			list.add(this.lspa);
		if (this.bandwidth != null)
			list.add(this.bandwidth);
		if (this.metrics != null && !this.metrics.isEmpty())
			list.addAll(this.metrics);
		return list;
	}

	/**
	 * Creates this object from a list of PCEPObjects.
	 * @param objects List<PCEPObject> list of PCEPObjects from whose this
	 * object should be created.
	 * @return CompositeUpdPathObject constructed from objects
	 */
	public static CompositeUpdPathObject getCompositeFromList(List<PCEPObject> objects) {
		if (objects == null || objects.isEmpty()) {
			throw new IllegalArgumentException("List cannot be null or empty.");
		}

		PCEPExplicitRouteObject explicitRoute = null;
		if (objects.get(0) instanceof PCEPExplicitRouteObject) {
			explicitRoute = (PCEPExplicitRouteObject) objects.get(0);
			objects.remove(explicitRoute);
		} else
			return null;

		PCEPLspaObject lspa = null;
		PCEPRequestedPathBandwidthObject bandwidth = null;
		final List<PCEPMetricObject> metrics = new ArrayList<PCEPMetricObject>();

		int state = 1;
		while (!objects.isEmpty()) {
			final PCEPObject obj = objects.get(0);

			switch (state) {
				case 1:
					state = 2;
					if (obj instanceof PCEPLspaObject) {
						lspa = (PCEPLspaObject) obj;
						break;
					}
				case 2:
					state = 3;
					if (obj instanceof PCEPRequestedPathBandwidthObject) {
						bandwidth = (PCEPRequestedPathBandwidthObject) obj;
						break;
					}
				case 3:
					if (obj instanceof PCEPMetricObject) {
						metrics.add((PCEPMetricObject) obj);
						state = 3;
						break;
					} else
						state = 4;
			}

			if (state == 4) {
				break;
			}

			objects.remove(obj);
		}

		return new CompositeUpdPathObject(explicitRoute, lspa, bandwidth, metrics);
	}

	/**
	 * Gets {@link PCEPExplicitRouteObject}
	 *
	 * @return PCEPExplicitRouteObject
	 */
	public PCEPExplicitRouteObject getExcludedRoute() {
		return this.explicitRoute;
	}

	/**
	 * Gets {@link PCEPLspaObject}
	 *
	 * @return PCEPLspaObject
	 */
	public PCEPLspaObject getLspa() {
		return this.lspa;
	}

	/**
	 * Gets bandwidth.
	 *
	 * @return PCEPBandwidthObject
	 */
	public PCEPBandwidthObject getBandwidth() {
		return this.bandwidth;
	}

	/**
	 * Gets list of {@link PCEPMetricObject}.
	 *
	 * @return List<PCEPMetricObject>
	 */
	public List<PCEPMetricObject> getMetrics() {
		return this.metrics;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.bandwidth == null) ? 0 : this.bandwidth.hashCode());
		result = prime * result + ((this.explicitRoute == null) ? 0 : this.explicitRoute.hashCode());
		result = prime * result + ((this.lspa == null) ? 0 : this.lspa.hashCode());
		result = prime * result + ((this.metrics == null) ? 0 : this.metrics.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final CompositeUpdPathObject other = (CompositeUpdPathObject) obj;
		if (this.bandwidth == null) {
			if (other.bandwidth != null)
				return false;
		} else if (!this.bandwidth.equals(other.bandwidth))
			return false;
		if (this.explicitRoute == null) {
			if (other.explicitRoute != null)
				return false;
		} else if (!this.explicitRoute.equals(other.explicitRoute))
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

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("CompositeUpdPathObject [explicitRoute=");
		builder.append(this.explicitRoute);
		builder.append(", lspa=");
		builder.append(this.lspa);
		builder.append(", bandwidth=");
		builder.append(this.bandwidth);
		builder.append(", metrics=");
		builder.append(this.metrics);
		builder.append("]");
		return builder.toString();
	}
}
