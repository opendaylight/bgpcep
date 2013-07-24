/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import java.util.Map;
import java.util.Set;

import org.opendaylight.protocol.bgp.linkstate.ISISAreaIdentifier;
import org.opendaylight.protocol.bgp.linkstate.NodeIdentifier;
import org.opendaylight.protocol.bgp.linkstate.RouterIdentifier;
import org.opendaylight.protocol.bgp.linkstate.TopologyIdentifier;
import org.opendaylight.protocol.bgp.linkstate.TopologyNodeInformation;
import org.opendaylight.protocol.bgp.linkstate.NetworkNode;
import org.opendaylight.protocol.bgp.linkstate.NetworkNodeState;

/**
 * Implementation of {@link NetworkNode}
 */
public class NetworkNodeImpl extends NetworkObjectImpl<NodeIdentifier> implements NetworkNode {
	private static final long serialVersionUID = -7999816386632869087L;

	/**
	 * 
	 * @param name {@link NodeIdentifier}
	 */
	public NetworkNodeImpl(final NodeIdentifier name) {
		this(name, NetworkNodeState.EMPTY);
	}

	/**
	 * 
	 * @param name {@link NodeIdentifier}
	 * @param template {@link NetworkNode}
	 */
	public NetworkNodeImpl(final NodeIdentifier name, final NetworkNodeState state) {
		super(name, state);
	}

	/**
	 * Standard setter for NetworkNode identifierAlterinatives attribute.
	 * 
	 * @param identifierAlternatives a set of all alternatives, has to include the primary identifier.
	 */
	@Deprecated
	public synchronized void setAlternativeIdentifiers(final Set<RouterIdentifier> identifierAlternatives) {
		this.state = currentState().withIdentifierAlternatives(identifierAlternatives);
	}

	/**
	 * Standard setter for NetworkNode topologyMembership attribute.
	 * 
	 * @param topologyMembership map of {@link TopologyIdentifier} and {@link TopologyNodeInformation}
	 */
	@Deprecated
	public synchronized void setTopologyMembership(final Map<TopologyIdentifier, TopologyNodeInformation> topologyMembership) {
		this.state = currentState().withTopologyMembership(topologyMembership);
	}

	/**
	 * Standard setter for NetworkNode areaMembership attribute.
	 * 
	 * @param areaMembership set of {@link NodeAreaIdentifier}
	 */
	@Deprecated
	public synchronized void setAreaMembership(final Set<ISISAreaIdentifier> areaMembership) {
		this.state = currentState().withAreaMembership(areaMembership);
	}

	/**
	 * Standard setter for NetworkNode isAreaBorderRouter attribute.
	 * 
	 * @param isAreaBorderRouter boolean
	 */
	@Deprecated
	public synchronized void setAreaBorderRouter(final boolean isAreaBorderRouter) {
		this.state = currentState().withAreaBorderRouter(isAreaBorderRouter);
	}

	/**
	 * Standard setter for NetworkNode isExternal attribute.
	 * 
	 * @param isExternal boolean
	 */
	@Deprecated
	public synchronized void setExternal(final boolean isExternal) {
		this.state = currentState().withExternal(isExternal);
	}

	/**
	 * 
	 * @param value hostName
	 */
	@Deprecated
	public synchronized void setDynamicHostname(final String value) {
		this.state = currentState().withDynamicHostname(value);
	}

	@Override
	public synchronized NetworkNodeState currentState() {
		return (NetworkNodeState) super.currentState();
	}
}
