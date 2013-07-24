/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import org.opendaylight.protocol.bgp.linkstate.AreaIdentifier;
import org.opendaylight.protocol.bgp.linkstate.DomainIdentifier;
import org.opendaylight.protocol.bgp.linkstate.OSPFRouteType;
import org.opendaylight.protocol.bgp.linkstate.SourceProtocol;
import org.opendaylight.protocol.bgp.linkstate.TopologyIdentifier;
import org.opendaylight.protocol.bgp.linkstate.TopologyNodeInformation;

/**
 * DTO for the subTlvs from Identifier TLV.
 *
 * @see <a href="http://tools.ietf.org/html/draft-ietf-idr-ls-distribution-02#section-3.2.1">Identifier TLV</a>
 */
public class IdentifierTlv {

	private final SourceProtocol sourceProtocol;

	private final DomainIdentifier domainId;

	private final AreaIdentifier areaId;

	private final OSPFRouteType routeType;

	private final TopologyIdentifier topologyId;

	private final TopologyNodeInformation topologyNodeInfo;

	public IdentifierTlv(final SourceProtocol sourceProtocol,
			final DomainIdentifier domainId, final AreaIdentifier areaId,
			final OSPFRouteType routeType, final TopologyIdentifier topologyId,
			final TopologyNodeInformation topologyNodeInfo) {
		this.sourceProtocol = sourceProtocol;
		this.domainId = domainId;
		this.areaId = areaId;
		this.routeType = routeType;
		this.topologyId = topologyId;
		this.topologyNodeInfo = topologyNodeInfo;
	}

	/**
	 * @return the sourceProtocolId
	 */
	public final SourceProtocol getSourceProtocol() {
		return this.sourceProtocol;
	}

	/**
	 * @return the domainId
	 */
	public final DomainIdentifier getDomainId() {
		return this.domainId;
	}

	/**
	 * @return the areaId
	 */
	public final AreaIdentifier getAreaId() {
		return this.areaId;
	}

	/**
	 * @return the routeType
	 */
	public final OSPFRouteType getRouteType() {
		return this.routeType;
	}

	/**
	 * @return the topologyId
	 */
	public final TopologyIdentifier getTopologyId() {
		return this.topologyId;
	}

	/**
	 * @return the topologyNodeInfo
	 */
	public final TopologyNodeInformation getTopologyNodeInfo() {
		return this.topologyNodeInfo;
	}
}
