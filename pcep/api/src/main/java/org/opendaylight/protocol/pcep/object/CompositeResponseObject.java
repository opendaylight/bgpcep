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
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-6.5">Path
 *      Computation Reply (PCRep) Message</a> - &lt;response&gt;</br>
 */
public class CompositeResponseObject {

	private final PCEPRequestParameterObject requestParameter;

	private final PCEPNoPathObject noPath;

	private final PCEPLspObject lsp;

	private final PCEPLspaObject lspa;

	private final PCEPRequestedPathBandwidthObject bandwidth;

	private List<PCEPMetricObject> metrics;

	private final PCEPIncludeRouteObject includeRoute;

	private List<CompositePathObject> paths;

	/**
	 * Constructs basic composite object only with mandatory objects.
	 *
	 * @param requestParameter
	 *            PCEPRequestParameterObject. Can't be null.
	 */
	public CompositeResponseObject(PCEPRequestParameterObject requestParameter) {
		this(requestParameter, null, null, null, null, null, null, null);
	}

	/**
	 * Constructs composite object with optional objects.
	 *
	 * @param requestParameter
	 *            PCEPRequestParameterObject. Can't be null.
	 * @param noPath
	 *            PCEPNoPathObject
	 * @param lsp
	 *            PCEPLspObject
	 * @param lspa
	 *            PCEPLspaObject
	 * @param bandwidth
	 *            PCEPRequestedPathBandwidthObject
	 * @param metrics
	 *            List<PCEPMetricObject>
	 * @param includeRoute
	 *            PCEPIncludeRouteObject
	 * @param paths
	 *            List<CompositePathObject>
	 */
	public CompositeResponseObject(PCEPRequestParameterObject requestParameter, PCEPNoPathObject noPath, PCEPLspObject lsp, PCEPLspaObject lspa,
			PCEPRequestedPathBandwidthObject bandwidth, List<PCEPMetricObject> metrics, PCEPIncludeRouteObject includeRoute, List<CompositePathObject> paths) {
		if (requestParameter == null)
			throw new IllegalArgumentException("Request Parameter Object is mandatory.");
		this.requestParameter = requestParameter;
		this.noPath = noPath;
		this.lsp = lsp;
		this.lspa = lspa;
		this.bandwidth = bandwidth;
		if (metrics != null)
			this.metrics = metrics;
		else
			this.metrics = Collections.emptyList();
		this.includeRoute = includeRoute;
		if (paths != null)
			this.paths = paths;
		else
			this.paths = Collections.emptyList();
	}

	/**
	 * Gets list of all objects, which are in appropriate order.
	 *
	 * @return List<PCEPObject>
	 */
	public List<PCEPObject> getCompositeAsList() {
		final List<PCEPObject> list = new ArrayList<PCEPObject>();
		list.add(this.requestParameter);
		if (this.noPath != null)
			list.add(this.noPath);
		if (this.lsp != null)
			list.add(this.lsp);
		if (this.lspa != null)
			list.add(this.lspa);
		if (this.bandwidth != null)
			list.add(this.bandwidth);
		if (this.metrics != null && !this.metrics.isEmpty())
			list.addAll(this.metrics);
		if (this.includeRoute != null)
			list.add(this.includeRoute);
		if (this.paths != null && !this.paths.isEmpty())
			for (final CompositePathObject cpo : this.paths)
				list.addAll(cpo.getCompositeAsList());
		return list;
	}

	/**
	 * Creates this object from a list of PCEPObjects.
	 * @param objects List<PCEPObject> list of PCEPObjects from whose this
	 * object should be created.
	 * @return CompositeResponseObject
	 */
	public static CompositeResponseObject getCompositeFromList(List<PCEPObject> objects) {
		if (objects == null || objects.isEmpty()) {
			throw new IllegalArgumentException("List cannot be null or empty.");
		}
		PCEPRequestParameterObject requestParameter = null;
		if (objects.get(0) instanceof PCEPRequestParameterObject) {
			requestParameter = (PCEPRequestParameterObject) objects.get(0);
			objects.remove(requestParameter);
		} else
			return null;
		PCEPNoPathObject noPath = null;
		PCEPLspObject lsp = null;
		PCEPLspaObject lspa = null;
		PCEPRequestedPathBandwidthObject bandwidth = null;
		final List<PCEPMetricObject> metrics = new ArrayList<PCEPMetricObject>();
		PCEPIncludeRouteObject iro = null;
		final List<CompositePathObject> paths = new ArrayList<CompositePathObject>();

		int state = 1;
		while (!objects.isEmpty()) {
			final PCEPObject obj = objects.get(0);
			switch (state) {
				case 1:
					state = 2;
					if (obj instanceof PCEPNoPathObject) {
						noPath = (PCEPNoPathObject) obj;
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
					if (obj instanceof PCEPMetricObject) {
						metrics.add((PCEPMetricObject) obj);
						state = 5;
						break;
					} else
						state = 6;
				case 6:
					state = 8;
					if (obj instanceof PCEPIncludeRouteObject) {
						iro = (PCEPIncludeRouteObject) obj;
						break;
					}
					state = 7;
			}

			if (state == 7) {
				break;
			}
			objects.remove(obj);
			if (state == 8) {
				break;
			}
		}
		if (!objects.isEmpty()) {
			CompositePathObject path = CompositePathObject.getCompositeFromList(objects);
			while (path != null) {
				paths.add(path);
				if (objects.isEmpty())
					break;
				path = CompositePathObject.getCompositeFromList(objects);
			}
		}
		return new CompositeResponseObject(requestParameter, noPath, lsp, lspa, bandwidth, metrics, iro, paths);
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
	 * Gets {@link PCEPNoPathObject}.
	 *
	 * @return PCEPNoPathObject. May be null.
	 */
	public PCEPNoPathObject getNoPath() {
		return this.noPath;
	}

	/**
	 * Gets {@link PCEPLspObject}
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
	 * @return List<PCEPMetricObject>. Can't be null, but may be empty.
	 */
	public List<PCEPMetricObject> getMetrics() {
		return this.metrics;
	}

	/**
	 * Gets list of {@link CompositePathObject}.
	 *
	 * @return PCEPIncludeRouteObject. Can't be null, but may be empty.
	 */
	public PCEPIncludeRouteObject getIncludeRoute() {
		return this.includeRoute;
	}

	/**
	 * Gets list of {@link CompositePathObject}.
	 *
	 * @return List<CompositePathObject>. Can't be null, but may be empty.
	 */
	public List<CompositePathObject> getPaths() {
		return this.paths;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.bandwidth == null) ? 0 : this.bandwidth.hashCode());
		result = prime * result + ((this.includeRoute == null) ? 0 : this.includeRoute.hashCode());
		result = prime * result + ((this.lsp == null) ? 0 : this.lsp.hashCode());
		result = prime * result + ((this.lspa == null) ? 0 : this.lspa.hashCode());
		result = prime * result + ((this.metrics == null) ? 0 : this.metrics.hashCode());
		result = prime * result + ((this.noPath == null) ? 0 : this.noPath.hashCode());
		result = prime * result + ((this.paths == null) ? 0 : this.paths.hashCode());
		result = prime * result + ((this.requestParameter == null) ? 0 : this.requestParameter.hashCode());
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
		final CompositeResponseObject other = (CompositeResponseObject) obj;
		if (this.bandwidth == null) {
			if (other.bandwidth != null)
				return false;
		} else if (!this.bandwidth.equals(other.bandwidth))
			return false;
		if (this.includeRoute == null) {
			if (other.includeRoute != null)
				return false;
		} else if (!this.includeRoute.equals(other.includeRoute))
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
		if (this.noPath == null) {
			if (other.noPath != null)
				return false;
		} else if (!this.noPath.equals(other.noPath))
			return false;
		if (this.paths == null) {
			if (other.paths != null)
				return false;
		} else if (!this.paths.equals(other.paths))
			return false;
		if (this.requestParameter == null) {
			if (other.requestParameter != null)
				return false;
		} else if (!this.requestParameter.equals(other.requestParameter))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("CompositeResponseObject [requestParameter=");
		builder.append(this.requestParameter);
		builder.append(", noPath=");
		builder.append(this.noPath);
		builder.append(", lsp=");
		builder.append(this.lsp);
		builder.append(", lspa=");
		builder.append(this.lspa);
		builder.append(", bandwidth=");
		builder.append(this.bandwidth);
		builder.append(", metrics=");
		builder.append(this.metrics);
		builder.append(", includeRoute=");
		builder.append(this.includeRoute);
		builder.append(", paths=");
		builder.append(this.paths);
		builder.append("]");
		return builder.toString();
	}
}
