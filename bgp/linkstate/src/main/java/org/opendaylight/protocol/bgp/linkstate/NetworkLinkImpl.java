/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import java.util.Set;

import org.opendaylight.protocol.concepts.Bandwidth;
import org.opendaylight.protocol.concepts.Metric;
import org.opendaylight.protocol.concepts.SharedRiskLinkGroup;
import org.opendaylight.protocol.bgp.linkstate.AdministrativeGroup;
import org.opendaylight.protocol.bgp.linkstate.LinkIdentifier;
import org.opendaylight.protocol.bgp.linkstate.LinkProtectionType;
import org.opendaylight.protocol.bgp.linkstate.MPLSProtocol;
import org.opendaylight.protocol.bgp.linkstate.NetworkLink;
import org.opendaylight.protocol.bgp.linkstate.NetworkLinkState;

/**
 * Implementation of {@link NetworkLink}
 */
public final class NetworkLinkImpl extends NetworkObjectImpl<LinkIdentifier> implements NetworkLink {
	private static final long serialVersionUID = 5203163596015262211L;

	/**
	 *
	 * @param name
	 *            {@link LinkIdentifier}
	 */
	public NetworkLinkImpl(final LinkIdentifier name) {
		this(name, NetworkLinkState.EMPTY);
	}

	/**
	 *
	 * @param name {@link LinkIdentifier}
	 * @param template {@link NetworkLink}
	 */
	public NetworkLinkImpl(final LinkIdentifier name, final NetworkLinkState state) {
		super(name, state);
	}

	@Override
	public NetworkLinkState currentState() {
		return (NetworkLinkState) super.currentState();
	}

	/**
	 *
	 * @param administrativeGroup
	 *            {@link AdministrativeGroup}
	 */
	@Deprecated
	public synchronized void setAdministrativeGroup(final AdministrativeGroup administrativeGroup) {
		this.state = currentState().withAdministrativeGroup(administrativeGroup);
	}

	/**
	 *
	 * @param maximumBandwidth
	 *            {@link Bandwidth}
	 */
	@Deprecated
	public synchronized void setMaximumBandwidth(final Bandwidth maximumBandwidth) {
		this.state = currentState().withMaximumBandwidth(maximumBandwidth);
	}

	/**
	 *
	 * @param reservableBandwidth
	 *            {@link Bandwidth}
	 */
	@Deprecated
	public synchronized void setMaximumReservableBandwidth(final Bandwidth reservableBandwidth) {
		this.state = currentState().withReservableBandwidth(reservableBandwidth);
	}

	/**
	 *
	 * @param unreservedBandwidth
	 *            array of {@link Bandwidth}
	 */
	@Deprecated
	public synchronized void setUnreservedBandwidth(final Bandwidth[] unreservedBandwidth) {
		this.state = currentState().withUnreservedBandwidth(unreservedBandwidth);
	}

	/**
	 *
	 * @param protectionType
	 *            {@link LinkProtectionType}
	 */
	@Deprecated
	public synchronized void setProtectionType(final LinkProtectionType protectionType) {
		this.state = currentState().withProtectionType(protectionType);
	}

	/**
	 *
	 * @param enabledMPLSProtocols
	 *            set of {@link MPLSProtocol}
	 */
	@Deprecated
	public synchronized void setEnabledMPLSProtocols(final Set<MPLSProtocol> enabledMPLSProtocols) {
		this.state = currentState().withEnabledMPLSProtocols(enabledMPLSProtocols);
	}

	/**
	 *
	 * @param sharedRiskLinkGroups
	 *            set of {@link SharedRiskLinkGroup}
	 */
	@Deprecated
	public synchronized void setSharedRiskLinkGroups(final Set<SharedRiskLinkGroup> sharedRiskLinkGroups) {
		this.state = currentState().withSharedRiskLinkGroups(sharedRiskLinkGroups);
	}

	/**
	 *
	 * @param <T>
	 *            metric
	 * @param metric
	 *            T
	 */
	@Deprecated
	public synchronized <T extends Metric<?>> void setDefaultMetric(final T metric) {
		this.state = currentState().withDefaultMetric(metric);
	}

	/**
	 *
	 * @param <T> metric
	 * @param metricType class
	 * @param metric T
	 */
	@Deprecated
	public synchronized <T extends Metric<T>> void setMetric(final Class<T> metricType, final T metric) {
		this.state = currentState().withMetric(metricType, metric);
	}
}
