/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.opendaylight.protocol.concepts.Metric;
import org.opendaylight.protocol.concepts.SharedRiskLinkGroup;
import org.opendaylight.protocol.util.DefaultingTypesafeContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nps.concepts.rev130930.Bandwidth;

import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

/**
 * 
 * A single link in network topology. Network link is a connecting line between two network nodes with bunch of
 * attributes.
 */
public final class NetworkLinkState extends NetworkObjectState {
	public static final NetworkLinkState EMPTY = new NetworkLinkState();
	private static final long serialVersionUID = 1L;
	private final DefaultingTypesafeContainer<Metric<?>> metrics;
	private Set<SharedRiskLinkGroup> sharedRiskLinkGroups;
	private AdministrativeGroup administrativeGroup;
	private Set<MPLSProtocol> enabledMPLSProtocols;
	private LinkProtectionType protectionType;
	private Bandwidth[] unreservedBandwidth;
	private Bandwidth reservableBandwidth;
	private Bandwidth maximumBandwidth;
	private String symbolicName;
	private Set<RouterIdentifier> localIds;
	private Set<RouterIdentifier> remoteIds;

	private NetworkLinkState() {
		this(NetworkObjectState.EMPTY, new DefaultingTypesafeContainer<Metric<?>>(), null, LinkProtectionType.UNPROTECTED, null, null, null);
	}

	public NetworkLinkState(final NetworkObjectState orig, final DefaultingTypesafeContainer<Metric<?>> metrics,
			final Set<SharedRiskLinkGroup> sharedRiskLinkGroups, final AdministrativeGroup administrativeGroup,
			final Set<MPLSProtocol> enabledMPLSProtocols, final LinkProtectionType protectionType, final String symbolicName,
			final Bandwidth[] unreservedBandwidth, final Bandwidth reservableBandwidth, final Bandwidth maximumBandwidth,
			final Set<RouterIdentifier> localIds, final Set<RouterIdentifier> remoteIds) {
		super(orig);
		this.metrics = Preconditions.checkNotNull(metrics, "Metric is mandatory.");
		this.sharedRiskLinkGroups = sharedRiskLinkGroups;
		this.administrativeGroup = administrativeGroup;
		this.enabledMPLSProtocols = enabledMPLSProtocols;
		this.protectionType = Preconditions.checkNotNull(protectionType);
		this.symbolicName = symbolicName;
		this.unreservedBandwidth = unreservedBandwidth;
		this.reservableBandwidth = reservableBandwidth;
		this.maximumBandwidth = maximumBandwidth;
		this.localIds = localIds;
		this.remoteIds = remoteIds;
	}

	public NetworkLinkState(final NetworkObjectState orig, final DefaultingTypesafeContainer<Metric<?>> metrics,
			final AdministrativeGroup administrativeGroup, final LinkProtectionType protectionType, final Bandwidth[] unreservedBandwidth,
			final Bandwidth reservableBandwidth, final Bandwidth maximumBandwidth) {
		this(orig, metrics, Collections.<SharedRiskLinkGroup> emptySet(), administrativeGroup, Collections.<MPLSProtocol> emptySet(), protectionType, null, unreservedBandwidth, reservableBandwidth, maximumBandwidth, Collections.<RouterIdentifier> emptySet(), Collections.<RouterIdentifier> emptySet());
	}

	protected NetworkLinkState(final NetworkLinkState orig) {
		super(orig);
		this.metrics = orig.metrics;
		this.sharedRiskLinkGroups = orig.sharedRiskLinkGroups;
		this.administrativeGroup = orig.administrativeGroup;
		this.enabledMPLSProtocols = orig.enabledMPLSProtocols;
		this.protectionType = orig.protectionType;
		this.symbolicName = orig.symbolicName;
		this.unreservedBandwidth = orig.unreservedBandwidth;
		this.reservableBandwidth = orig.reservableBandwidth;
		this.maximumBandwidth = orig.maximumBandwidth;
		this.localIds = orig.localIds;
		this.remoteIds = orig.remoteIds;
	}

	@Override
	protected NetworkLinkState newInstance() {
		return new NetworkLinkState(this);
	}

	/**
	 * Get AdministrativeGroup attribute of network link {@link AdministrativeGroup}.
	 * 
	 * @return AdministrativeGroup of network link.
	 */
	public final AdministrativeGroup getAdministrativeGroup() {
		return this.administrativeGroup;
	}

	public final NetworkLinkState withAdministrativeGroup(final AdministrativeGroup administrativeGroup) {
		final NetworkLinkState ret = newInstance();
		ret.administrativeGroup = administrativeGroup;
		return ret;
	}

	/**
	 * Get maximum bandwidth attribute of network link {@link Bandwidth}.
	 * 
	 * @return Bandwidth maximum bandwidth of network link.
	 */
	public final Bandwidth getMaximumBandwidth() {
		return this.maximumBandwidth;
	}

	public final NetworkLinkState withMaximumBandwidth(final Bandwidth maximumBandwidth) {
		final NetworkLinkState ret = newInstance();
		ret.maximumBandwidth = maximumBandwidth;
		return ret;
	}

	/**
	 * Get maximum reservable bandwidth attribute of network link {@link Bandwidth}.
	 * 
	 * @return Bandwidth maximum reservable bandwidth of network link.
	 */
	public final Bandwidth getMaximumReservableBandwidth() {
		return this.reservableBandwidth;
	}

	public final NetworkLinkState withReservableBandwidth(final Bandwidth reservableBandwidth) {
		final NetworkLinkState ret = newInstance();
		ret.reservableBandwidth = reservableBandwidth;
		return ret;
	}

	/**
	 * Get unreserved bandwidth attribute of network link {@link Bandwidth}.
	 * 
	 * @return Bandwidth[] unreserved bandwidth of network link.
	 */
	public final Bandwidth[] getUnreservedBandwidth() {
		return this.unreservedBandwidth;
	}

	public final NetworkLinkState withUnreservedBandwidth(final Bandwidth[] unreservedBandwidth) {
		Preconditions.checkNotNull(unreservedBandwidth);
		Preconditions.checkArgument(unreservedBandwidth.length == 8);

		final NetworkLinkState ret = newInstance();
		ret.unreservedBandwidth = unreservedBandwidth;
		return ret;
	}

	/**
	 * Get link protection type attribute of network link {@link LinkProtectionType}.
	 * 
	 * @return LinkProtectionType of network link.
	 */
	public final LinkProtectionType getProtectionType() {
		return this.protectionType;
	}

	public final NetworkLinkState withProtectionType(final LinkProtectionType protectionType) {
		final NetworkLinkState ret = newInstance();
		ret.protectionType = protectionType;
		return ret;
	}

	/**
	 * Get set of enabled MPLSProtocols of network link {@link MPLSProtocol}.
	 * 
	 * @return Set<MPLSProtocol> enabled MPLSProtocols of network link.
	 */
	public final Set<MPLSProtocol> getEnabledMPLSProtocols() {
		return this.enabledMPLSProtocols;
	}

	public final NetworkLinkState withEnabledMPLSProtocols(final Set<MPLSProtocol> enabledMPLSProtocols) {
		final NetworkLinkState ret = newInstance();
		ret.enabledMPLSProtocols = enabledMPLSProtocols;
		return ret;
	}

	/**
	 * Get set of SharedRiskLinkGroups of network link {@link SharedRiskLinkGroup}.
	 * 
	 * @return Set<SharedRiskLinkGroup> shared risk link groups of network link.
	 */
	public final Set<SharedRiskLinkGroup> getSharedRiskLinkGroups() {
		return this.sharedRiskLinkGroups;
	}

	public final NetworkLinkState withSharedRiskLinkGroups(final Set<SharedRiskLinkGroup> sharedRiskLinkGroups) {
		Preconditions.checkNotNull(sharedRiskLinkGroups);
		final NetworkLinkState ret = newInstance();
		ret.sharedRiskLinkGroups = sharedRiskLinkGroups;
		return ret;
	}

	/**
	 * Get default Metric attribute of network link {@link Metric}.
	 * 
	 * @return default Metric of network link.
	 */
	public final Metric<?> getDefaultMetric() {
		return this.metrics.getDefaultEntry();
	}

	public final <T extends Metric<?>> NetworkLinkState withDefaultMetric(final T metric) {
		// FIXME: this violates API contract of State!
		this.metrics.setDefaultEntry(metric);
		return this;
	}

	/**
	 * Get metric attribute of submitted metric type of network link {@link Metric}.
	 * 
	 * @param <T> sub-type of metric.
	 * @param metricType which value should be returned.
	 * @return metric of submitted type of network link.
	 */
	public final <T extends Metric<?>> T getMetric(final Class<? extends T> metricType) {
		return this.metrics.getEntry(metricType);
	}

	public final <T extends Metric<T>> NetworkLinkState withMetric(final Class<T> metricType, final T metric) {
		// FIXME: this violates API contract of State!
		this.metrics.setEntry(metricType, metric);
		return this;
	}

	public final String getSymbolicName() {
		return this.symbolicName;
	}

	public final NetworkLinkState withSymbolicName(final String symbolicName) {
		final NetworkLinkState ret = newInstance();
		ret.symbolicName = symbolicName;
		return ret;
	}

	public final Set<RouterIdentifier> getLocalRouterIdentifiers() {
		return this.localIds;
	}

	public final NetworkLinkState withLocalRouterIdentifiers(final Set<RouterIdentifier> localIds) {
		final NetworkLinkState ret = newInstance();
		ret.localIds = localIds;
		return ret;
	}

	public final Set<RouterIdentifier> getRemoteRouterIdentifiers() {
		return this.remoteIds;
	}

	public final NetworkLinkState withRemoteRouterIdentifiers(final Set<RouterIdentifier> remoteIds) {
		final NetworkLinkState ret = newInstance();
		ret.remoteIds = remoteIds;
		return ret;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("metrics", this.metrics);
		toStringHelper.add("SRLGs", this.sharedRiskLinkGroups);
		toStringHelper.add("administrativeGroup", this.administrativeGroup);
		toStringHelper.add("enabledMPLSProtocols", this.enabledMPLSProtocols);
		toStringHelper.add("protectionType", this.protectionType);
		toStringHelper.add("unreservedBandwidth", this.unreservedBandwidth);
		toStringHelper.add("reservableBandwidth", this.reservableBandwidth);
		toStringHelper.add("maximumBandwidth", this.maximumBandwidth);
		toStringHelper.add("symbolicName", this.symbolicName);
		toStringHelper.add("localIds", this.localIds);
		toStringHelper.add("remoteIds", this.remoteIds);
		return super.addToStringAttributes(toStringHelper);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.administrativeGroup == null) ? 0 : this.administrativeGroup.hashCode());
		result = prime * result + ((this.enabledMPLSProtocols == null) ? 0 : this.enabledMPLSProtocols.hashCode());
		result = prime * result + ((this.maximumBandwidth == null) ? 0 : this.maximumBandwidth.hashCode());
		result = prime * result + ((this.metrics == null) ? 0 : this.metrics.hashCode());
		result = prime * result + ((this.protectionType == null) ? 0 : this.protectionType.hashCode());
		result = prime * result + ((this.symbolicName == null) ? 0 : this.symbolicName.hashCode());
		result = prime * result + ((this.reservableBandwidth == null) ? 0 : this.reservableBandwidth.hashCode());
		result = prime * result + ((this.sharedRiskLinkGroups == null) ? 0 : this.sharedRiskLinkGroups.hashCode());
		result = prime * result + Arrays.hashCode(this.unreservedBandwidth);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		final NetworkLinkState other = (NetworkLinkState) obj;
		if (this.administrativeGroup == null) {
			if (other.administrativeGroup != null)
				return false;
		} else if (!this.administrativeGroup.equals(other.administrativeGroup))
			return false;
		if (this.enabledMPLSProtocols == null) {
			if (other.enabledMPLSProtocols != null)
				return false;
		} else if (!this.enabledMPLSProtocols.equals(other.enabledMPLSProtocols))
			return false;
		if (this.maximumBandwidth == null) {
			if (other.maximumBandwidth != null)
				return false;
		} else if (!this.maximumBandwidth.equals(other.maximumBandwidth))
			return false;
		if (this.metrics == null) {
			if (other.metrics != null)
				return false;
		} else if (!this.metrics.equals(other.metrics))
			return false;
		if (this.symbolicName == null) {
			if (other.symbolicName != null)
				return false;
		} else if (!this.symbolicName.equals(other.symbolicName))
			return false;
		if (this.protectionType != other.protectionType)
			return false;
		if (this.reservableBandwidth == null) {
			if (other.reservableBandwidth != null)
				return false;
		} else if (!this.reservableBandwidth.equals(other.reservableBandwidth))
			return false;
		if (this.sharedRiskLinkGroups == null) {
			if (other.sharedRiskLinkGroups != null)
				return false;
		} else if (!this.sharedRiskLinkGroups.equals(other.sharedRiskLinkGroups))
			return false;
		if (!Arrays.equals(this.unreservedBandwidth, other.unreservedBandwidth))
			return false;
		return true;
	}
}
