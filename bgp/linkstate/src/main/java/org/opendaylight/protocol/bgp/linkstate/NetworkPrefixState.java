/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import java.util.SortedSet;
import java.util.TreeSet;

import org.opendaylight.protocol.concepts.Metric;
import org.opendaylight.protocol.bgp.linkstate.RouteTag;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

/**
 * Generic, IGP-independent prefix advertisement.
 * @param <T> Network Address type of the prefix
 */
public class NetworkPrefixState extends NetworkObjectState {
	public static final NetworkPrefixState EMPTY = new NetworkPrefixState();
	private static final long serialVersionUID = 1L;
	private SortedSet<RouteTag> routeTags;
	private Metric<?> metric;

	private NetworkPrefixState() {
		this(NetworkObjectState.EMPTY, new TreeSet<RouteTag>(), null);
	}

	protected NetworkPrefixState(NetworkPrefixState orig) {
		super(orig);
		this.metric = orig.metric;
		this.routeTags = orig.routeTags;
	}

	public NetworkPrefixState(NetworkObjectState orig, SortedSet<RouteTag> routeTags, Metric<?> metric) {
		super(orig);
		Preconditions.checkNotNull(routeTags);
		this.metric = metric;
		this.routeTags = routeTags;
	}

	/**
	 * Return the prefix metric attached to this advertisement.
	 *
	 * @return Prefix metric, possibly null
	 */
	public final Metric<?> getPrefixMetric() {
		return metric;
	}

	public final NetworkPrefixState withPrefixMetric(Metric<?> metric) {
		final NetworkPrefixState ret = newInstance();
		ret.metric = metric;
		return ret;
	}

	/**
	 * Return the route tag attached to this advertisement.
	 *
	 * @return Route tag, possibly null
	 */
	public final SortedSet<RouteTag> getRouteTags() {
		return routeTags;
	}

	public final NetworkPrefixState withRouteTags(SortedSet<RouteTag> routeTags) {
		final NetworkPrefixState ret = newInstance();
		ret.routeTags = routeTags;
		return ret;
	}

	@Override
	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("metric", metric);
		toStringHelper.add("routeTags", routeTags);
		return super.addToStringAttributes(toStringHelper);
	}

	@Override
	protected NetworkPrefixState newInstance() {
		return new NetworkPrefixState(this);
	}
}

