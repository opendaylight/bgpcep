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
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-6.4">Path
 *      Computation Request (PCReq) Message</a> - &lt;request&gt;</br>
 * 
 * @see <a href="tools.ietf.org/html/rfc5455#section-3.2">Path Computation
 *      Request Message with CLASSTYPE Object</a>
 */
public class CompositeRequestObject {

	private final PCEPRequestParameterObject requestParameter;

	private final PCEPEndPointsObject<?> endPoints;

	private final PCEPClassTypeObject classType;

	private final PCEPLspObject lsp;

	private final PCEPLspaObject lspa;

	private final PCEPRequestedPathBandwidthObject bandwidth;

	private final List<PCEPMetricObject> metrics;

	private final PCEPReportedRouteObject reportedRoute;

	private final PCEPExistingPathBandwidthObject rroBandwidth;

	private final PCEPIncludeRouteObject includeRoute;

	private final PCEPLoadBalancingObject loadBalancing;

	/**
	 * Constructs basic composite object only with mandatory objects.
	 * 
	 * @param requestParameter
	 *            PCEPRequestParameterObject. Can't be null.
	 * @param endPoints
	 *            PCEPEndPointsObject<?>. Can't be null.
	 */
	public CompositeRequestObject(PCEPRequestParameterObject requestParameter, PCEPEndPointsObject<?> endPoints) {
		this(requestParameter, endPoints, null, null, null, null, null, null, null, null, null);
	}

	/**
	 * Constructs composite object with optional objects.
	 * 
	 * @param requestParameter
	 *            PCEPRequestParameterObject. Can't be null.
	 * @param endPoints
	 *            PCEPEndPointsObject<?>. Can't be null.
	 * @param classType
	 *            PCEPClassTypeObject
	 * @param lsp
	 *            PCEPLspObject
	 * @param lspa
	 *            PCEPLspaObject
	 * @param bandwidth
	 *            PCEPRequestedPathBandwidthObject
	 * @param metrics
	 *            List<PCEPMetricObject>
	 * @param reportedRoute
	 *            PCEPReportedRouteObject
	 * @param rroBandwidth
	 *            PCEPExistingPathBandwidthObject
	 * @param includeRoute
	 *            PCEPIncludeRouteObject
	 * @param loadBalancing
	 *            PCEPLoadBalancingObject
	 */
	public CompositeRequestObject(PCEPRequestParameterObject requestParameter, PCEPEndPointsObject<?> endPoints, PCEPClassTypeObject classType,
			PCEPLspObject lsp, PCEPLspaObject lspa, PCEPRequestedPathBandwidthObject bandwidth, List<PCEPMetricObject> metrics,
			PCEPReportedRouteObject reportedRoute, PCEPExistingPathBandwidthObject rroBandwidth, PCEPIncludeRouteObject includeRoute,
			PCEPLoadBalancingObject loadBalancing) {
		if (requestParameter == null)
			throw new IllegalArgumentException("Request Parameter Object is mandatory.");
		if (endPoints == null)
			throw new IllegalArgumentException("End-Points Object is mandatory.");
		this.requestParameter = requestParameter;
		this.endPoints = endPoints;
		this.classType = classType;
		this.lsp = lsp;
		this.lspa = lspa;
		this.bandwidth = bandwidth;
		if (metrics != null)
			this.metrics = metrics;
		else
			this.metrics = Collections.emptyList();
		this.reportedRoute = reportedRoute;
		this.rroBandwidth = rroBandwidth;
		this.includeRoute = includeRoute;
		this.loadBalancing = loadBalancing;
	}

	/**
	 * Gets list of all objects, which are in appropriate order.
	 * 
	 * @return List<PCEPObject>. Can't be null or empty.
	 */
	public List<PCEPObject> getCompositeAsList() {
		final List<PCEPObject> list = new ArrayList<PCEPObject>();
		list.add(this.requestParameter);
		list.add(this.endPoints);
		if (this.classType != null)
			list.add(this.classType);
		if (this.lsp != null)
			list.add(this.lsp);
		if (this.lspa != null)
			list.add(this.lspa);
		if (this.bandwidth != null)
			list.add(this.bandwidth);
		if (this.metrics != null && !this.metrics.isEmpty())
			list.addAll(this.metrics);
		if (this.reportedRoute != null) {
			list.add(this.reportedRoute);
			if (this.rroBandwidth != null)
				list.add(this.rroBandwidth);
		}
		if (this.includeRoute != null)
			list.add(this.includeRoute);
		if (this.loadBalancing != null)
			list.add(this.loadBalancing);
		return list;
	}

	/**
	 * Creates this object from a list of PCEPObjects.
	 * 
	 * @param objects
	 *            List<PCEPObject> list of PCEPObjects from whose this object
	 *            should be created.
	 * @return CompositeRequestObject
	 */
	public static CompositeRequestObject getCompositeFromList(List<PCEPObject> objects) {
		if (objects == null || objects.isEmpty()) {
			throw new IllegalArgumentException("List cannot be null or empty.");
		}
		PCEPRequestParameterObject requestParameter = null;
		if (objects.get(0) instanceof PCEPRequestParameterObject) {
			requestParameter = (PCEPRequestParameterObject) objects.get(0);
			objects.remove(requestParameter);
		} else
			return null;

		PCEPEndPointsObject<?> endPoints = null;
		if (objects.get(0) instanceof PCEPEndPointsObject<?>) {
			endPoints = (PCEPEndPointsObject<?>) objects.get(0);
			objects.remove(endPoints);
		} else
			throw new IllegalArgumentException("End Points object must be second.");

		PCEPClassTypeObject classType = null;
		PCEPLspObject lsp = null;
		PCEPLspaObject lspa = null;
		PCEPRequestedPathBandwidthObject bandwidth = null;
		final List<PCEPMetricObject> metrics = new ArrayList<PCEPMetricObject>();
		PCEPReportedRouteObject rro = null;
		PCEPExistingPathBandwidthObject rroBandwidth = null;
		PCEPIncludeRouteObject iro = null;
		PCEPLoadBalancingObject loadBalancing = null;

		int state = 1;
		while (!objects.isEmpty()) {
			final PCEPObject obj = objects.get(0);
			switch (state) {
				case 1:
					state = 2;
					if (obj instanceof PCEPClassTypeObject) {
						classType = (PCEPClassTypeObject) obj;
						break;
					}
				case 2:
					state = 3;
					if (obj instanceof PCEPLspObject) {
						lsp = (PCEPLspObject) obj;
						break;
					}
				case 3:
					state = 4;
					if (obj instanceof PCEPLspaObject) {
						lspa = (PCEPLspaObject) obj;
						break;
					}
				case 4:
					state = 5;
					if (obj instanceof PCEPRequestedPathBandwidthObject) {
						bandwidth = (PCEPRequestedPathBandwidthObject) obj;
						break;
					}
				case 5:
					state = 6;
					if (obj instanceof PCEPMetricObject) {
						metrics.add((PCEPMetricObject) obj);
						state = 5;

						break;
					}
				case 6:
					state = 8;
					if (obj instanceof PCEPReportedRouteObject) {
						rro = (PCEPReportedRouteObject) obj;
						state = 7;
						break;
					}
				case 7:
					state = 8;
					if (obj instanceof PCEPExistingPathBandwidthObject) {
						rroBandwidth = (PCEPExistingPathBandwidthObject) obj;
						break;
					}
				case 8:
					state = 9;
					if (obj instanceof PCEPIncludeRouteObject) {
						iro = (PCEPIncludeRouteObject) obj;
						break;
					}
				case 9:
					if (obj instanceof PCEPLoadBalancingObject) {
						loadBalancing = (PCEPLoadBalancingObject) obj;
						break;
					}
					state = 10;
			}

			if (state == 10) {
				break;
			}

			objects.remove(obj);
		}

		return new CompositeRequestObject(requestParameter, endPoints, classType, lsp, lspa, bandwidth, metrics, rro, rroBandwidth, iro, loadBalancing);
	}

	/**
	 * Gets {@link PCEPRequestParameterObject}.
	 * 
	 * @return PCEPRequestParameterObject. Can't be null.
	 */
	public PCEPRequestParameterObject getRequestParameter() {
		return this.requestParameter;
	}

	/**
	 * Gets {@link PCEPEndPointsObject}.
	 * 
	 * @return PCEPEndPointsObject<?>. Can't be null.
	 */
	public PCEPEndPointsObject<?> getEndPoints() {
		return this.endPoints;
	}

	/**
	 * Gets {@link PCEPClassTypeObject}.
	 * 
	 * @return PCEPClassTypeObject. May be null.
	 */
	public PCEPClassTypeObject getClassType() {
		return this.classType;
	}

	/**
	 * Gets {@link PCEPLspObject}.
	 * 
	 * @return PCEPLspObject. May be null.
	 */
	public PCEPLspObject getLsp() {
		return this.lsp;
	}

	/**
	 * Gets {@link PCEPLspaObject}.
	 * 
	 * @return PCEPLspaObject. May be null.
	 */
	public PCEPLspaObject getLspa() {
		return this.lspa;
	}

	/**
	 * Gets {@link PCEPBandwidthObject}.
	 * 
	 * @return PCEPBandwidthObject. May be null.
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

	/**
	 * Gets {@link PCEPReportedRouteObject}.
	 * 
	 * @return PCEPReportedRouteObject. May be null.
	 */
	public PCEPReportedRouteObject getReportedRoute() {
		return this.reportedRoute;
	}

	/**
	 * Gets {@link PCEPBandwidthObject}.
	 * 
	 * @return PCEPBandwidthObject. May be null.
	 */
	public PCEPBandwidthObject getRroBandwidth() {
		return this.rroBandwidth;
	}

	/**
	 * Gets {@link PCEPIncludeRouteObject}.
	 * 
	 * @return PCEPIncludeRouteObject. May be null.
	 */
	public PCEPIncludeRouteObject getIncludeRoute() {
		return this.includeRoute;
	}

	/**
	 * Gets {@link PCEPLoadBalancingObject}.
	 * 
	 * @return PCEPLoadBalancingObject. May be null.
	 */
	public PCEPLoadBalancingObject getLoadBalancing() {
		return this.loadBalancing;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.bandwidth == null) ? 0 : this.bandwidth.hashCode());
		result = prime * result + ((this.classType == null) ? 0 : this.classType.hashCode());
		result = prime * result + ((this.endPoints == null) ? 0 : this.endPoints.hashCode());
		result = prime * result + ((this.includeRoute == null) ? 0 : this.includeRoute.hashCode());
		result = prime * result + ((this.loadBalancing == null) ? 0 : this.loadBalancing.hashCode());
		result = prime * result + ((this.lsp == null) ? 0 : this.lsp.hashCode());
		result = prime * result + ((this.lspa == null) ? 0 : this.lspa.hashCode());
		result = prime * result + ((this.metrics == null) ? 0 : this.metrics.hashCode());
		result = prime * result + ((this.reportedRoute == null) ? 0 : this.reportedRoute.hashCode());
		result = prime * result + ((this.requestParameter == null) ? 0 : this.requestParameter.hashCode());
		result = prime * result + ((this.rroBandwidth == null) ? 0 : this.rroBandwidth.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof CompositeRequestObject))
			return false;
		final CompositeRequestObject other = (CompositeRequestObject) obj;
		if (this.bandwidth == null) {
			if (other.bandwidth != null)
				return false;
		} else if (!this.bandwidth.equals(other.bandwidth))
			return false;
		if (this.classType == null) {
			if (other.classType != null)
				return false;
		} else if (!this.classType.equals(other.classType))
			return false;
		if (this.endPoints == null) {
			if (other.endPoints != null)
				return false;
		} else if (!this.endPoints.equals(other.endPoints))
			return false;
		if (this.includeRoute == null) {
			if (other.includeRoute != null)
				return false;
		} else if (!this.includeRoute.equals(other.includeRoute))
			return false;
		if (this.loadBalancing == null) {
			if (other.loadBalancing != null)
				return false;
		} else if (!this.loadBalancing.equals(other.loadBalancing))
			return false;
		if (this.lsp == null) {
			if (other.lsp != null)
				return false;
		} else if (!this.lsp.equals(other.lsp))
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
		if (this.reportedRoute == null) {
			if (other.reportedRoute != null)
				return false;
		} else if (!this.reportedRoute.equals(other.reportedRoute))
			return false;
		if (this.requestParameter == null) {
			if (other.requestParameter != null)
				return false;
		} else if (!this.requestParameter.equals(other.requestParameter))
			return false;
		if (this.rroBandwidth == null) {
			if (other.rroBandwidth != null)
				return false;
		} else if (!this.rroBandwidth.equals(other.rroBandwidth))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("CompositeRequestObject [requestParameter=");
		builder.append(this.requestParameter);
		builder.append(", endPoints=");
		builder.append(this.endPoints);
		builder.append(", classType=");
		builder.append(this.classType);
		builder.append(", lsp=");
		builder.append(this.lsp);
		builder.append(", lspa=");
		builder.append(this.lspa);
		builder.append(", bandwidth=");
		builder.append(this.bandwidth);
		builder.append(", metrics=");
		builder.append(this.metrics);
		builder.append(", reportedRoute=");
		builder.append(this.reportedRoute);
		builder.append(", rroBandwidth=");
		builder.append(this.rroBandwidth);
		builder.append(", includeRoute=");
		builder.append(this.includeRoute);
		builder.append(", loadBalancing=");
		builder.append(this.loadBalancing);
		builder.append("]");
		return builder.toString();
	}
}
