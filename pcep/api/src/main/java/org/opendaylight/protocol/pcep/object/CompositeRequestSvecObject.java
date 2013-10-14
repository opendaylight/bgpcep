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
import org.opendaylight.protocol.pcep.message.PCEPRequestMessage;

/**
 * Composite SvecObject used in {@link PCEPRequestMessage}
 */
public class CompositeRequestSvecObject {

	private final PCEPSvecObject svec;
	private final PCEPObjectiveFunctionObject objectiveFunction;
	private final PCEPGlobalConstraintsObject globalConstraints;
	private final PCEPExcludeRouteObject excludeRoute;
	private final List<PCEPMetricObject> metrics;

	/**
	 * Constructs basic composite object only with mandatory objects.
	 * 
	 * @param svec
	 *            PCEPSvecObject
	 */
	public CompositeRequestSvecObject(PCEPSvecObject svec) {
		this(svec, null, null, null, null);
	}

	/**
	 * Constructs composite object with optional objects.
	 * 
	 * @param svec
	 *            PCEPSvecObject
	 * @param objectiveFunction
	 *            PCEPObjectiveFunctionObject
	 * @param globalConstraints
	 *            PCEPGlobalConstraints
	 * @param excludeRoute
	 *            PCEPExcludeRouteObject
	 * @param metrics
	 *            list of PCEPMetricObject
	 */
	public CompositeRequestSvecObject(PCEPSvecObject svec, PCEPObjectiveFunctionObject objectiveFunction, PCEPGlobalConstraintsObject globalConstraints,
			PCEPExcludeRouteObject excludeRoute, List<PCEPMetricObject> metrics) {
		if (svec == null)
			throw new IllegalArgumentException("Svec object is mandatory.");
		this.svec = svec;
		this.objectiveFunction = objectiveFunction;
		this.globalConstraints = globalConstraints;
		this.excludeRoute = excludeRoute;
		if (metrics != null)
			this.metrics = metrics;
		else
			this.metrics = Collections.emptyList();
	}

	/**
	 * Gets list of all objects, which are in appropriate order.
	 * 
	 * @return List<PCEPObject>. Can't be null or empty.
	 */
	public List<PCEPObject> getCompositeAsList() {
		final List<PCEPObject> list = new ArrayList<PCEPObject>();
		list.add(this.svec);
		if (this.objectiveFunction != null)
			list.add(this.objectiveFunction);
		if (this.globalConstraints != null)
			list.add(this.globalConstraints);
		if (this.excludeRoute != null)
			list.add(this.excludeRoute);
		if (this.metrics != null && !this.metrics.isEmpty())
			list.addAll(this.metrics);
		return list;
	}

	/**
	 * Creates this object from a list of PCEPObjects.
	 * 
	 * @param objects
	 *            List<PCEPObject> list of PCEPObjects from whose this object
	 *            should be created.
	 * @return CompositePathObject
	 */
	public static CompositeRequestSvecObject getCompositeFromList(List<PCEPObject> objects) {
		if (objects == null || objects.isEmpty()) {
			throw new IllegalArgumentException("List cannot be null or empty.");
		}

		PCEPSvecObject svec = null;
		if (objects.get(0) instanceof PCEPSvecObject) {
			svec = (PCEPSvecObject) objects.get(0);
			objects.remove(svec);
		} else
			return null;

		PCEPObjectiveFunctionObject of = null;
		PCEPGlobalConstraintsObject gc = null;
		PCEPExcludeRouteObject xro = null;
		final List<PCEPMetricObject> metrics = new ArrayList<PCEPMetricObject>();

		int state = 1;
		while (!objects.isEmpty()) {
			final PCEPObject obj = objects.get(0);

			switch (state) {
				case 1:
					state = 2;
					if (obj instanceof PCEPObjectiveFunctionObject) {
						of = (PCEPObjectiveFunctionObject) obj;
						break;
					}
				case 2:
					state = 3;
					if (obj instanceof PCEPGlobalConstraintsObject) {
						gc = (PCEPGlobalConstraintsObject) obj;
						break;
					}
				case 3:
					state = 4;
					if (obj instanceof PCEPExcludeRouteObject) {
						xro = (PCEPExcludeRouteObject) obj;
						break;
					}
				case 4:
					state = 5;
					if (obj instanceof PCEPMetricObject) {
						metrics.add((PCEPMetricObject) obj);
						state = 4;

						break;
					}
			}

			if (state == 5)
				break;

			objects.remove(obj);
		}

		return new CompositeRequestSvecObject(svec, of, gc, xro, metrics);
	}

	/**
	 * Gets {@link PCEPSvecObject}
	 * 
	 * @return PCEPSvecObject. Can't be null.
	 */
	public PCEPSvecObject getSvec() {
		return this.svec;
	}

	/**
	 * Gets {@link PCEPObjectiveFunctionObject}
	 * 
	 * @return PCEPObjectiveFunctionObject. May be null.
	 */
	public PCEPObjectiveFunctionObject getObjectiveFunction() {
		return this.objectiveFunction;
	}

	/**
	 * Gets {@link PCEPGlobalConstraintsObject}
	 * 
	 * @return PCEPGlobalConstraints. May be null.
	 */
	public PCEPGlobalConstraintsObject getGlobalConstraints() {
		return this.globalConstraints;
	}

	/**
	 * Gets {@link PCEPExcludeRouteObject}
	 * 
	 * @return PCEPExcludeRouteObject. May be null.
	 */
	public PCEPExcludeRouteObject getExcludeRoute() {
		return this.excludeRoute;
	}

	/**
	 * @return the metrics
	 */
	public List<PCEPMetricObject> getMetrics() {
		return this.metrics;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("CompositeSvecObject [svec=");
		builder.append(this.svec);
		builder.append(", objectiveFunction=");
		builder.append(this.objectiveFunction);
		builder.append(", globalConstraints=");
		builder.append(this.globalConstraints);
		builder.append(", excludeRoute=");
		builder.append(this.excludeRoute);
		builder.append(", metrics=");
		builder.append(this.metrics);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.excludeRoute == null) ? 0 : this.excludeRoute.hashCode());
		result = prime * result + ((this.globalConstraints == null) ? 0 : this.globalConstraints.hashCode());
		result = prime * result + ((this.metrics == null) ? 0 : this.metrics.hashCode());
		result = prime * result + ((this.objectiveFunction == null) ? 0 : this.objectiveFunction.hashCode());
		result = prime * result + ((this.svec == null) ? 0 : this.svec.hashCode());
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
		final CompositeRequestSvecObject other = (CompositeRequestSvecObject) obj;
		if (this.excludeRoute == null) {
			if (other.excludeRoute != null)
				return false;
		} else if (!this.excludeRoute.equals(other.excludeRoute))
			return false;
		if (this.globalConstraints == null) {
			if (other.globalConstraints != null)
				return false;
		} else if (!this.globalConstraints.equals(other.globalConstraints))
			return false;
		if (this.metrics == null) {
			if (other.metrics != null)
				return false;
		} else if (!this.metrics.equals(other.metrics))
			return false;
		if (this.objectiveFunction == null) {
			if (other.objectiveFunction != null)
				return false;
		} else if (!this.objectiveFunction.equals(other.objectiveFunction))
			return false;
		if (this.svec == null) {
			if (other.svec != null)
				return false;
		} else if (!this.svec.equals(other.svec))
			return false;
		return true;
	}

}
