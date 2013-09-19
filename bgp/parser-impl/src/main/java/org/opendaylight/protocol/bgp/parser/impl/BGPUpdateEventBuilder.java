/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.concurrent.NotThreadSafe;

import org.opendaylight.protocol.bgp.concepts.ASPath;
import org.opendaylight.protocol.bgp.concepts.BGPAggregator;
import org.opendaylight.protocol.bgp.concepts.BGPObject;
import org.opendaylight.protocol.bgp.concepts.BaseBGPObjectState;
import org.opendaylight.protocol.bgp.concepts.Community;
import org.opendaylight.protocol.bgp.concepts.ExtendedCommunity;
import org.opendaylight.protocol.bgp.concepts.IPv4NextHop;
import org.opendaylight.protocol.bgp.concepts.IPv6NextHop;
import org.opendaylight.protocol.bgp.linkstate.IPv4PrefixIdentifier;
import org.opendaylight.protocol.bgp.linkstate.IPv6PrefixIdentifier;
import org.opendaylight.protocol.bgp.linkstate.LinkIdentifier;
import org.opendaylight.protocol.bgp.linkstate.NetworkLinkImpl;
import org.opendaylight.protocol.bgp.linkstate.NetworkNodeImpl;
import org.opendaylight.protocol.bgp.linkstate.NetworkObjectState;
import org.opendaylight.protocol.bgp.linkstate.NetworkPrefixState;
import org.opendaylight.protocol.bgp.linkstate.NetworkRouteState;
import org.opendaylight.protocol.bgp.linkstate.NodeIdentifier;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BGPUpdateEvent;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPUpdateMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.LinkStateParser;
import org.opendaylight.protocol.bgp.util.BGPIPv4PrefixImpl;
import org.opendaylight.protocol.bgp.util.BGPIPv4RouteImpl;
import org.opendaylight.protocol.bgp.util.BGPIPv6PrefixImpl;
import org.opendaylight.protocol.bgp.util.BGPIPv6RouteImpl;
import org.opendaylight.protocol.bgp.util.BGPLinkImpl;
import org.opendaylight.protocol.bgp.util.BGPNodeImpl;
import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.IPv6Address;
import org.opendaylight.protocol.concepts.Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * 
 * Builds BGPUpdateEvent. This code was originally in {@link BGPUpdateMessageParser}. Moved here during refactoring.
 * 
 * Withdrawn routes or nlri that contain directly prefixes, can contain only IPv4 Prefixes.
 */
@NotThreadSafe
public class BGPUpdateEventBuilder {

	private static final Logger log = LoggerFactory.getLogger(BGPUpdateEventBuilder.class);

	/**
	 * 
	 * Length of the withdrawn_routes field, in bytes.
	 */

	private int withdrawnRoutesLength;

	/**
	 * 
	 * List of IP address prefixes for the routes that are being withdrawn. Can be empty when there are no routes to
	 * 
	 * withdraw.
	 */

	private Set<Prefix<IPv4Address>> withdrawnRoutes;

	/**
	 * 
	 * Length of the total_path_attributes field, in bytes.
	 */

	private int totalPathAttrLength;

	/**
	 * 
	 * List of path attributes. Can be empty when there are only withdrawn routes present.
	 */

	private List<PathAttribute> pathAttributes;

	/**
	 * 
	 * List of IP address prefixes of routes that are advertised.
	 */

	private Set<Prefix<IPv4Address>> nlri;

	/**
	 * 
	 * Fills in BGP Objects that need to be added to topology. The method first checks and sets Path Attributes. If the
	 * 
	 * NLRI field is not empty, create for each of the prefixes present in NLRI, a BGPRoute with the attributes and
	 * 
	 * corresponding Prefix. If MP_REACH_NLRI attribute is found, check its NLRI, if its not of type Link State, do the
	 * 
	 * same.
	 * 
	 * 
	 * 
	 * @param pathAttributes
	 * 
	 * @param nlri
	 * 
	 * 
	 * 
	 * @return set of BGP Objects that need to be added
	 * 
	 * @throws BGPParsingException
	 */

	private Set<BGPObject> fillAddedObjects(final List<PathAttribute> pathAttributes, final Set<Prefix<IPv4Address>> nlri)
			throws BGPParsingException {
		BgpOrigin origin = null;
		ASPath aspath = null;
		IPv4NextHop nextHop = null;
		BGPAggregator aggregator = null;
		final Set<ExtendedCommunity> ecomm = Sets.newHashSet();
		final Set<Community> comm = Sets.newHashSet();
		final Map<Integer, ByteList> linkstate = Maps.newHashMap();
		for (final PathAttribute pa : pathAttributes) {
			if (pa.getValue() instanceof BgpOrigin) {
				origin = (BgpOrigin) pa.getValue();
			} else if (pa.getValue() instanceof ASPath) {
				aspath = (ASPath) pa.getValue();
			} else if (pa.getValue() instanceof IPv4NextHop) {
				nextHop = (IPv4NextHop) pa.getValue();
			} else if (pa.getValue() instanceof BGPAggregator) {
				aggregator = (BGPAggregator) pa.getValue();
			} else if (pa.getValue() instanceof Set) {
				for (final Object o : (Set<?>) pa.getValue()) {
					if (o instanceof ExtendedCommunity) {
						ecomm.add((ExtendedCommunity) o);
					} else if (o instanceof Community) {
						comm.add((Community) o);
					}
				}
			} else if (pa.getValue() instanceof Map) {
				for (final Entry<?, ?> entry : ((Map<?, ?>) pa.getValue()).entrySet()) {
					if (entry.getValue() instanceof ByteList) {
						final ByteList lb = (ByteList) entry.getValue();
						linkstate.put((Integer) entry.getKey(), lb);
					}
				}
			}
		}

		final BaseBGPObjectState base = new BaseBGPObjectState(origin, aggregator);
		final NetworkObjectState nos = new NetworkObjectState(aspath, comm, ecomm);
		final Set<BGPObject> added = new HashSet<BGPObject>();
		if (!nlri.isEmpty()) {
			final NetworkRouteState<IPv4Address> nrs = new NetworkRouteState<>(nos, nextHop);
			for (final Prefix<IPv4Address> p : nlri) {
				added.add(new BGPIPv4RouteImpl(p, base, nrs));
			}
		}

		final MPReach<?> mpreach = findMP(pathAttributes, true);
		if (mpreach != null) {
			if (mpreach instanceof IPv4MP) {
				final IPv4MP ipv4mp = (IPv4MP) mpreach;
				final IPv4NextHop v4nextHop = ipv4mp.getNextHop();
				final NetworkRouteState<IPv4Address> nrs = new NetworkRouteState<>(nos, v4nextHop);
				for (final Prefix<IPv4Address> p : ipv4mp.getNlri()) {
					added.add(new BGPIPv4RouteImpl(p, base, nrs));
				}
			} else if (mpreach instanceof IPv6MP) {
				final IPv6MP ipv6mp = (IPv6MP) mpreach;
				final IPv6NextHop v6nextHop = ipv6mp.getNextHop();
				final NetworkRouteState<IPv6Address> nrs = new NetworkRouteState<>(nos, v6nextHop);
				for (final Prefix<IPv6Address> p : ipv6mp.getNlri()) {
					added.add(new BGPIPv6RouteImpl(p, base, nrs));
				}
			} else if (mpreach instanceof BGPNodeMP) {
				final Set<NodeIdentifier> nodes = ((BGPNodeMP) mpreach).getNlri();
				if (!LinkStateParser.verifyNode(linkstate.keySet()))
					throw new BGPParsingException("Some attributes from LINK_STATE Path attribute don't belong to advertised node.");
				for (final NodeIdentifier desc : nodes) {
					final NetworkNodeImpl n = LinkStateParser.parseNodeAttributes(desc, linkstate);
					n.setASPath(aspath);
					n.setExtendedCommunities(ecomm);
					n.setCommunities(comm);
					final BGPNodeImpl bgpNode = new BGPNodeImpl(base, desc, n.currentState());
					log.debug("Adding bgp node {}", bgpNode);
					added.add(bgpNode);
				}
			} else if (mpreach instanceof BGPLinkMP) {
				final Set<LinkIdentifier> links = ((BGPLinkMP) mpreach).getNlri();
				if (!LinkStateParser.verifyLink(linkstate.keySet()))
					throw new BGPParsingException("Some attributes from LINK_STATE Path attribute don't belong to advertised link.");
				for (final LinkIdentifier desc : links) {
					final NetworkLinkImpl l = LinkStateParser.parseLinkAttributes(desc, linkstate);
					l.setASPath(aspath);
					l.setExtendedCommunities(ecomm);
					l.setCommunities(comm);
					log.debug("Adding bgp link {}", l);
					added.add(new BGPLinkImpl(base, desc, l.currentState()));
				}
			} else if (mpreach instanceof BGPIPv4PrefixMP) {
				final Set<IPv4PrefixIdentifier> prefixes = ((BGPIPv4PrefixMP) mpreach).getNlri();
				if (!LinkStateParser.verifyPrefix(linkstate.keySet()))
					throw new BGPParsingException("Some attributes from LINK_STATE Path attribute don't belong to advertised prefix.");
				final NetworkPrefixState nps = LinkStateParser.parsePrefixAttributes(((BGPIPv4PrefixMP) mpreach).getSourceProtocol(), nos,
						linkstate);
				for (final IPv4PrefixIdentifier desc : prefixes) {
					log.debug("Adding IPv4 Prefix {} State {}", desc, nps);
					added.add(new BGPIPv4PrefixImpl(base, desc, nps));
				}
			} else if (mpreach instanceof BGPIPv6PrefixMP) {
				final Set<IPv6PrefixIdentifier> prefixes = ((BGPIPv6PrefixMP) mpreach).getNlri();
				if (!LinkStateParser.verifyPrefix(linkstate.keySet()))
					throw new BGPParsingException("Some attributes from LINK_STATE Path attribute don't belong to advertised prefix.");

				final NetworkPrefixState nps = LinkStateParser.parsePrefixAttributes(((BGPIPv6PrefixMP) mpreach).getSourceProtocol(), nos,
						linkstate);
				for (final IPv6PrefixIdentifier desc : prefixes) {
					log.debug("Adding IPv6 Prefix {} State {}", desc, nps);
					added.add(new BGPIPv6PrefixImpl(base, desc, nps));
				}
			}
		}
		return added;

	}

	/**
	 * Fills in Identifiers that need to be removed. First, check field withdrawn routes, that can contain only IPv4
	 * prefixes. Then, check the presence of MP_UNREACH_NLRI and if its NLRI contains Prefixes, add them to removed
	 * field. For link state information, Node & LinkIdentifiers are added to the Set.
	 * 
	 * @param pathAttributes
	 * @param withdrawnRoutes
	 * 
	 * @return set of identifiers that need to be removed
	 */

	private Set<?> fillRemovedObjects(final List<PathAttribute> pathAttributes, final Set<Prefix<IPv4Address>> withdrawnRoutes) {
		final Set<Object> removed = Sets.newHashSet();
		if (!withdrawnRoutes.isEmpty()) {
			removed.addAll(withdrawnRoutes);
		}
		final MPReach<?> mpunreach = findMP(pathAttributes, false);
		if (mpunreach != null) {
			if (mpunreach instanceof IPv4MP) {
				final IPv4MP ipv4mp = (IPv4MP) mpunreach;
				if (!ipv4mp.getNlri().isEmpty()) {
					removed.addAll(ipv4mp.getNlri());
				}
			} else if (mpunreach instanceof IPv6MP) {
				final IPv6MP ipv6mp = (IPv6MP) mpunreach;
				if (!ipv6mp.getNlri().isEmpty()) {
					removed.addAll(ipv6mp.getNlri());
				}
			} else if (mpunreach instanceof BGPNodeMP) {
				for (final NodeIdentifier node : ((BGPNodeMP) mpunreach).getNlri()) {
					removed.add(node);
				}
			} else if (mpunreach instanceof BGPLinkMP) {
				for (final LinkIdentifier link : ((BGPLinkMP) mpunreach).getNlri()) {
					removed.add(link);
				}
			} else if (mpunreach instanceof BGPIPv4PrefixMP) {
				for (final IPv4PrefixIdentifier pref : ((BGPIPv4PrefixMP) mpunreach).getNlri()) {
					removed.add(pref);
				}
			} else if (mpunreach instanceof BGPIPv6PrefixMP) {
				for (final IPv6PrefixIdentifier pref : ((BGPIPv6PrefixMP) mpunreach).getNlri()) {
					removed.add(pref);
				}
			}
		}
		return removed;

	}

	/**
	 * Finds MPReach object in Path Attribute list (depending on reachability boolean) and returns typecasted object.
	 * 
	 * @param arrayList list of path attributes
	 * @param reachable true if we search for MP_REACH_NLRI, false if we search for MP_UNREACH_NLRI
	 * 
	 * @return cated MPReach object
	 */
	private static <T> MPReach<?> findMP(final Collection<PathAttribute> arrayList, final boolean reachable) {
		for (final PathAttribute o : arrayList) {
			final Object v = o.getValue();
			if (v != null && v instanceof MPReach<?>) {
				final MPReach<?> t = (MPReach<?>) v;
				if (t.isReachable() == reachable)
					return t;
			}
		}
		return null;
	}

	int getWithdrawnRoutesLength() {
		return this.withdrawnRoutesLength;
	}

	public void setWithdrawnRoutesLength(final int withdrawnRoutesLength) {
		this.withdrawnRoutesLength = withdrawnRoutesLength;
	}

	Set<Prefix<IPv4Address>> getWithdrawnRoutes() {
		return this.withdrawnRoutes;
	}

	public void setWithdrawnRoutes(final Set<Prefix<IPv4Address>> withdrawnRoutes) {
		this.withdrawnRoutes = withdrawnRoutes;
	}

	int getTotalPathAttrLength() {
		return this.totalPathAttrLength;
	}

	public void setTotalPathAttrLength(final int totalPathAttrLength) {
		this.totalPathAttrLength = totalPathAttrLength;
	}

	List<PathAttribute> getPathAttributes() {
		return this.pathAttributes;
	}

	public void setPathAttributes(final List<PathAttribute> pathAttributes) {
		this.pathAttributes = pathAttributes;
	}

	Set<Prefix<IPv4Address>> getNlri() {
		return this.nlri;
	}

	public void setNlri(final Set<Prefix<IPv4Address>> nlri) {
		this.nlri = nlri;
	}

	/**
	 * Builds BGP Update message.
	 * 
	 * @return BGP Update message
	 * @throws BGPParsingException
	 */
	public BGPUpdateEvent buildEvent() throws BGPParsingException {
		final Set<BGPObject> added = fillAddedObjects(this.pathAttributes, this.nlri);
		final Set<?> removed = fillRemovedObjects(this.pathAttributes, this.withdrawnRoutes);
		return new BGPUpdateMessageImpl(added, removed);
	}
}
