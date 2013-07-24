/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.opendaylight.protocol.bgp.linkstate.ISISAreaIdentifier;
import org.opendaylight.protocol.bgp.linkstate.RouterIdentifier;
import org.opendaylight.protocol.bgp.linkstate.TopologyIdentifier;
import org.opendaylight.protocol.bgp.linkstate.TopologyNodeInformation;
import com.google.common.base.CharMatcher;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

/**
 * A single (router) node in the network topology. Nodes are interconnected by links and have a bunch of attributes. One
 * of the key attributes is the set of prefixes for which this node acts as a network edge router.
 */
public class NetworkNodeState extends NetworkObjectState {
	public static final NetworkNodeState EMPTY = new NetworkNodeState();
	private static final long serialVersionUID = 1L;
	private Map<TopologyIdentifier, TopologyNodeInformation> topologyMembership;
	private Set<ISISAreaIdentifier> areaMembership;
	private boolean areaBorderRouter;
	private boolean external;
	private Set<RouterIdentifier> identifierAlternatives;
	private String dynamicHostName;

	private NetworkNodeState() {
		this(NetworkObjectState.EMPTY, Collections.<TopologyIdentifier, TopologyNodeInformation> emptyMap(), Collections.<ISISAreaIdentifier> emptySet(), false, false, Collections.<RouterIdentifier> emptySet(), null);
	}

	public NetworkNodeState(final NetworkObjectState orig, final Map<TopologyIdentifier, TopologyNodeInformation> topologyMembership,
			final Set<ISISAreaIdentifier> areaMembership, final boolean areaBorderRouter, final boolean external,
			final Set<RouterIdentifier> identifierAlternatives, final String dynamicHostName) {
		super(orig);
		Preconditions.checkNotNull(areaMembership);
		Preconditions.checkNotNull(identifierAlternatives);
		Preconditions.checkNotNull(topologyMembership);
		this.topologyMembership = topologyMembership;
		this.areaMembership = areaMembership;
		this.areaBorderRouter = areaBorderRouter;
		this.external = external;
		this.identifierAlternatives = identifierAlternatives;
		this.dynamicHostName = dynamicHostName;
	}

	protected NetworkNodeState(final NetworkNodeState orig) {
		super(orig);
		this.topologyMembership = orig.topologyMembership;
		this.areaMembership = orig.areaMembership;
		this.areaBorderRouter = orig.areaBorderRouter;
		this.external = orig.external;
		this.identifierAlternatives = orig.identifierAlternatives;
		this.dynamicHostName = orig.dynamicHostName;
	}

	/**
	 * Get the per-topology information about this node.
	 * 
	 * @return An immutable map of per-topology state information
	 */
	public final Map<TopologyIdentifier, TopologyNodeInformation> getTopologyMembership() {
		return this.topologyMembership;
	}

	public final NetworkNodeState withTopologyMembership(final Map<TopologyIdentifier, TopologyNodeInformation> topologyMembership) {
		final NetworkNodeState ret = newInstance();
		ret.topologyMembership = Collections.unmodifiableMap(topologyMembership);
		return ret;
	}

	/**
	 * Get area membership information.
	 * 
	 * @return An immutable set containing identifiers of all node area this node is member of.
	 */
	public final Set<ISISAreaIdentifier> getAreaMembership() {
		return this.areaMembership;
	}

	public final NetworkNodeState withAreaMembership(final Set<ISISAreaIdentifier> areaMembership) {
		final NetworkNodeState ret = newInstance();
		ret.areaMembership = Collections.unmodifiableSet(areaMembership);
		return ret;
	}

	/**
	 * Get ABR flag value. Area Border Routers (e.g. routers connected to multiple areas) advertise this flag.
	 * 
	 * @return True if this router is an ABR.
	 */
	public final boolean isAreaBorderRouter() {
		return this.areaBorderRouter;
	}

	public final NetworkNodeState withAreaBorderRouter(final boolean value) {
		final NetworkNodeState ret = newInstance();
		ret.areaBorderRouter = value;
		return ret;
	}

	/**
	 * Get external flag value. This corresponds to <a href="http://tools.ietf.org/html/rfc2328">RFC 2328</a> definition
	 * of ExternalRoutingCapability. It is advertized by all routers connected to external ASes.
	 * 
	 * @return True if the router is an AS-border router
	 */
	public final boolean isExternal() {
		return this.external;
	}

	public final NetworkNodeState withExternal(final boolean value) {
		final NetworkNodeState ret = newInstance();
		ret.external = value;
		return ret;
	}

	/**
	 * http://tools.ietf.org/html/rfc5301#section-3, encoded as a String. The string is guaranteed to contain US-ASCII
	 * characters.
	 * 
	 * @return dynamic hostname of the router that is connected
	 */
	public final String getDynamicHostname() {
		return this.dynamicHostName;
	}

	public final NetworkNodeState withDynamicHostname(final String value) {
		Preconditions.checkNotNull(value);
		Preconditions.checkArgument(value.length() >= 1);
		Preconditions.checkArgument(value.length() <= 255);
		Preconditions.checkArgument(CharMatcher.ASCII.matchesAllOf(value));

		final NetworkNodeState ret = newInstance();
		ret.dynamicHostName = value;
		return ret;
	}

	/**
	 * Return the set of alternative identifiers to which this node responds. This set must contain the primary
	 * identifier.
	 * 
	 * @return set of identifier alternatives.
	 */
	public final Set<RouterIdentifier> getIdentifierAlternatives() {
		return this.identifierAlternatives;
	}

	public final NetworkNodeState withIdentifierAlternatives(final Set<RouterIdentifier> identifierAlternatives) {
		final NetworkNodeState ret = newInstance();
		ret.identifierAlternatives = identifierAlternatives;
		return ret;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("topologyMembership", this.topologyMembership);
		toStringHelper.add("areaMembership", this.areaMembership);
		toStringHelper.add("external", this.external);
		toStringHelper.add("ABR", this.areaBorderRouter);
		toStringHelper.add("dynamicHostname", this.dynamicHostName);
		toStringHelper.add("routerIdentifiers", this.identifierAlternatives);
		return super.addToStringAttributes(toStringHelper);
	}

	@Override
	protected NetworkNodeState newInstance() {
		return new NetworkNodeState(this);
	}
}
