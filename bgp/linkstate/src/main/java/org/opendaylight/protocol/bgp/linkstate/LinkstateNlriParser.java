/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriUtil;
import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.AreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.DomainIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.Identifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.Ipv4InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.Ipv6InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.OspfInterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.OspfRouteType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.isis.lan.identifier.IsIsRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.isis.lan.identifier.IsIsRouterIdentifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.linkstate.destination.CLinkstateDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.linkstate.destination.c.linkstate.destination.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.linkstate.destination.c.linkstate.destination.LinkDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.linkstate.destination.c.linkstate.destination.LocalNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.linkstate.destination.c.linkstate.destination.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.linkstate.destination.c.linkstate.destination.PrefixDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.linkstate.destination.c.linkstate.destination.PrefixDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.linkstate.destination.c.linkstate.destination.RemoteNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.linkstate.destination.c.linkstate.destination.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.node.identifier.CRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.node.identifier.c.router.identifier.CIsisNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.node.identifier.c.router.identifier.CIsisPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.node.identifier.c.router.identifier.COspfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.node.identifier.c.router.identifier.COspfPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.node.identifier.c.router.identifier.c.isis.node.IsisNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.node.identifier.c.router.identifier.c.isis.pseudonode.IsisPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.node.identifier.c.router.identifier.c.ospf.node.OspfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.node.identifier.c.router.identifier.c.ospf.pseudonode.OspfPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nps.concepts.rev130930.IsoSystemIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedBytes;

public final class LinkstateNlriParser implements NlriParser {
	private static final Logger logger = LoggerFactory.getLogger(LinkstateNlriParser.class);
	private static final int ROUTE_DISTINGUISHER_LENGTH = 8;
	private static final int PROTOCOL_ID_LENGTH = 1;
	private static final int IDENTIFIER_LENGTH = 8;

	private static final int TYPE_LENGTH = 2;
	private static final int LENGTH_SIZE = 2;

	private final boolean isVpn;

	public LinkstateNlriParser(final boolean isVpn) {
		this.isVpn = isVpn;
	}

	private static NodeIdentifier parseLink(final CLinkstateDestinationBuilder builder, final byte[] bytes) throws BGPParsingException {
		int byteOffset = 0;
		final int type = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, TYPE_LENGTH));
		byteOffset += TYPE_LENGTH;
		final int length = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, LENGTH_SIZE));
		byteOffset += LENGTH_SIZE;
		final NodeIdentifier remote = null;
		if (type == 257) {
			builder.setRemoteNodeDescriptors((RemoteNodeDescriptors) parseNodeDescriptors(ByteArray.subByte(bytes, byteOffset, length),
					false));
			byteOffset += length;
		}
		builder.setLinkDescriptors(parseLinkDescriptors(ByteArray.subByte(bytes, byteOffset, bytes.length - byteOffset)));
		return remote;
	}

	private static LinkDescriptors parseLinkDescriptors(final byte[] bytes) throws BGPParsingException {
		int byteOffset = 0;
		final LinkDescriptorsBuilder builder = new LinkDescriptorsBuilder();
		while (byteOffset != bytes.length) {
			final int type = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, TYPE_LENGTH));
			byteOffset += TYPE_LENGTH;
			final int length = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, LENGTH_SIZE));
			byteOffset += LENGTH_SIZE;
			final byte[] value = ByteArray.subByte(bytes, byteOffset, length);
			logger.trace("Parsing Link Descriptor: {}", Arrays.toString(value));
			switch (type) {
			case 258:
				builder.setLinkLocalIdentifier(ByteArray.subByte(value, 0, 4));
				builder.setLinkRemoteIdentifier(ByteArray.subByte(value, 4, 4));
				logger.debug("Parsed link local {} remote {} Identifiers.", builder.getLinkLocalIdentifier(),
						builder.getLinkRemoteIdentifier());
				break;
			case 259:
				final Ipv4InterfaceIdentifier lipv4 = new Ipv4InterfaceIdentifier(Ipv4Util.addressForBytes(value));
				builder.setIpv4InterfaceAddress(lipv4);
				logger.debug("Parsed IPv4 interface address {}.", lipv4);
				break;
			case 260:
				final Ipv4InterfaceIdentifier ripv4 = new Ipv4InterfaceIdentifier(Ipv4Util.addressForBytes(value));
				builder.setIpv4NeighborAddress(ripv4);
				logger.debug("Parsed IPv4 neighbor address {}.", ripv4);
				break;
			case 261:
				final Ipv6InterfaceIdentifier lipv6 = new Ipv6InterfaceIdentifier(Ipv6Util.addressForBytes(value));
				builder.setIpv6InterfaceAddress(lipv6);
				logger.debug("Parsed IPv6 interface address {}.", lipv6);
				break;
			case 262:
				final Ipv6InterfaceIdentifier ripv6 = new Ipv6InterfaceIdentifier(Ipv6Util.addressForBytes(value));
				builder.setIpv6NeighborAddress(ripv6);
				logger.debug("Parsed IPv6 neighbor address {}.", ripv6);
				break;
			case 263:
				final TopologyIdentifier topId = new TopologyIdentifier(ByteArray.bytesToInt(value) & 0x3fff);
				builder.setMultiTopologyId(topId);
				logger.debug("Parsed topology identifier {}.", topId);
				break;
			default:
				throw new BGPParsingException("Link Descriptor not recognized, type: " + type);
			}
			byteOffset += length;
		}
		logger.trace("Finished parsing Link descriptors.");
		return builder.build();
	}

	private static NodeIdentifier parseNodeDescriptors(final byte[] bytes, final boolean local) throws BGPParsingException {
		int byteOffset = 0;
		AsNumber asnumber = null;
		DomainIdentifier bgpId = null;
		AreaIdentifier ai = null;
		CRouterIdentifier routerId = null;
		while (byteOffset != bytes.length) {
			final int type = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, TYPE_LENGTH));
			byteOffset += TYPE_LENGTH;
			final int length = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, LENGTH_SIZE));
			byteOffset += LENGTH_SIZE;
			final byte[] value = ByteArray.subByte(bytes, byteOffset, length);
			logger.trace("Parsing Node Descriptor: {}", Arrays.toString(value));
			switch (type) {
			case 512:
				asnumber = new AsNumber(ByteArray.bytesToLong(value));
				logger.debug("Parsed {}", asnumber);
				break;
			case 513:
				bgpId = new DomainIdentifier(value);
				logger.debug("Parsed {}", bgpId);
				break;
			case 514:
				ai = new AreaIdentifier(value);
				logger.debug("Parsed area identifier {}", ai);
				break;
			case 515:
				routerId = parseRouterId(value);
				logger.debug("Parsed Router Identifier {}", routerId);
				break;
			default:
				throw new BGPParsingException("Node Descriptor not recognized, type: " + type);
			}
			byteOffset += length;
		}
		logger.trace("Finished parsing Node descriptors.");
		return (local) ? new LocalNodeDescriptorsBuilder().setAsNumber(asnumber).setDomainId(bgpId).setAreaId(ai).setCRouterIdentifier(
				routerId).build()
				: new RemoteNodeDescriptorsBuilder().setAsNumber(asnumber).setDomainId(bgpId).setAreaId(ai).setCRouterIdentifier(routerId).build();
	}

	private static CRouterIdentifier parseRouterId(final byte[] value) throws BGPParsingException {
		if (value.length == 6) {
			return new CIsisNodeBuilder().setIsisNode(
					new IsisNodeBuilder().setIsoSystemId(new IsoSystemIdentifier(ByteArray.subByte(value, 0, 6))).build()).build();
		}
		if (value.length == 7) {
			if (value[6] == 0) {
				logger.warn("PSN octet is 0. Ignoring System ID.");
				return new CIsisNodeBuilder().setIsisNode(
						new IsisNodeBuilder().setIsoSystemId(new IsoSystemIdentifier(ByteArray.subByte(value, 0, 6))).build()).build();
			} else {
				final IsIsRouterIdentifier iri = new IsIsRouterIdentifierBuilder().setIsoSystemId(
						new IsoSystemIdentifier(ByteArray.subByte(value, 0, 6))).build();
				return new CIsisPseudonodeBuilder().setIsisPseudonode(
						new IsisPseudonodeBuilder().setIsIsRouterIdentifier(iri).setPsn((short) UnsignedBytes.toInt(value[6])).build()).build();
			}
		}
		if (value.length == 4) {
			return new COspfNodeBuilder().setOspfNode(new OspfNodeBuilder().setOspfRouterId(ByteArray.subByte(value, 0, 4)).build()).build();
		}
		if (value.length == 8) {
			final byte[] o = ByteArray.subByte(value, 0, 4); // FIXME: OSPFv3 vs OSPFv2
			final OspfInterfaceIdentifier a = new OspfInterfaceIdentifier(ByteArray.subByte(value, 4, 4));
			return new COspfPseudonodeBuilder().setOspfPseudonode(new OspfPseudonodeBuilder().setOspfRouterId(o).setLanInterface(a).build()).build();
		}
		throw new BGPParsingException("Router Id of invalid length " + value.length);
	}

	private static PrefixDescriptors parsePrefixDescriptors(final NodeIdentifier localDescriptor, final byte[] bytes)
			throws BGPParsingException {
		int byteOffset = 0;
		final PrefixDescriptorsBuilder builder = new PrefixDescriptorsBuilder();
		while (byteOffset != bytes.length) {
			final int type = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, TYPE_LENGTH));
			byteOffset += TYPE_LENGTH;
			final int length = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, LENGTH_SIZE));
			byteOffset += LENGTH_SIZE;
			final byte[] value = ByteArray.subByte(bytes, byteOffset, length);
			logger.trace("Parsing Prefix Descriptor: {}", Arrays.toString(value));
			switch (type) {
			case 263:
				final TopologyIdentifier topologyId = new TopologyIdentifier(ByteArray.bytesToInt(value) & 0x3fff);
				builder.setMultiTopologyId(topologyId);
				logger.trace("Parsed Topology Identifier: {}", topologyId);
				break;
			case 264:
				final int rt = ByteArray.bytesToInt(value);
				final OspfRouteType routeType = OspfRouteType.forValue(rt);
				if (routeType == null) {
					throw new BGPParsingException("Unknown OSPF Route Type: " + rt);
				}
				builder.setOspfRouteType(routeType);
				logger.trace("Parser RouteType: {}", routeType);
				break;
			case 265:
				IpPrefix prefix = null;
				final int prefixLength = UnsignedBytes.toInt(value[0]);
				final int size = prefixLength / 8 + ((prefixLength % 8 == 0) ? 0 : 1);
				if (size != value.length - 1) {
					logger.debug("Expected length {}, actual length {}.", size, value.length - 1);
					throw new BGPParsingException("Illegal length of IP reachability TLV: " + (value.length - 1));
				}
				if (size == 4) {
					prefix = new IpPrefix(Ipv4Util.prefixForBytes(ByteArray.subByte(value, 1, size), prefixLength));
				} else {
					prefix = new IpPrefix(Ipv6Util.prefixForBytes(ByteArray.subByte(value, 1, size), prefixLength));
				}
				builder.setIpReachabilityInformation(prefix);
				logger.trace("Parsed IP reachability info: {}", prefix);
				break;
			default:
				throw new BGPParsingException("Prefix Descriptor not recognized, type: " + type);
			}
			byteOffset += length;
		}
		logger.debug("Finished parsing Prefix descriptors.");
		return builder.build();
	}

	/**
	 * Parses common parts for Link State Nodes, Links and Prefixes, that includes protocol ID and identifier tlv.
	 * 
	 * @param nlri
	 * @return BGPLinkMP or BGPNodeMP
	 * @throws BGPParsingException
	 */
	private List<CLinkstateDestination> parseNlri(final byte[] nlri) throws BGPParsingException {
		if (nlri.length == 0) {
			return null;
		}
		int byteOffset = 0;

		final List<CLinkstateDestination> dests = Lists.newArrayList();

		CLinkstateDestinationBuilder builder = null;

		while (byteOffset != nlri.length) {
			builder = new CLinkstateDestinationBuilder();
			final NlriType type = NlriType.forValue(ByteArray.bytesToInt(ByteArray.subByte(nlri, byteOffset, TYPE_LENGTH)));
			builder.setNlriType(type);

			byteOffset += TYPE_LENGTH;
			// length means total length of the tlvs including route distinguisher not including the type field
			final int length = ByteArray.bytesToInt(ByteArray.subByte(nlri, byteOffset, LENGTH_SIZE));
			byteOffset += LENGTH_SIZE;
			RouteDistinguisher distinguisher = null;
			if (this.isVpn) {
				// this parses route distinguisher
				distinguisher = new RouteDistinguisher(BigInteger.valueOf(ByteArray.bytesToLong(ByteArray.subByte(nlri, byteOffset,
						ROUTE_DISTINGUISHER_LENGTH))));
				builder.setDistinguisher(distinguisher);
				byteOffset += ROUTE_DISTINGUISHER_LENGTH;
			}
			// parse source protocol
			final ProtocolId sp = ProtocolId.forValue(ByteArray.bytesToInt(ByteArray.subByte(nlri, byteOffset, PROTOCOL_ID_LENGTH)));
			byteOffset += PROTOCOL_ID_LENGTH;
			builder.setProtocolId(sp);

			// parse identifier
			final Identifier identifier = new Identifier(BigInteger.valueOf(ByteArray.bytesToLong(ByteArray.subByte(nlri, byteOffset,
					IDENTIFIER_LENGTH))));
			byteOffset += IDENTIFIER_LENGTH;
			builder.setIdentifier(identifier);

			// if we are dealing with linkstate nodes/links, parse local node descriptor
			NodeIdentifier localDescriptor = null;
			int locallength = 0;
			final int localtype = ByteArray.bytesToInt(ByteArray.subByte(nlri, byteOffset, TYPE_LENGTH));
			byteOffset += TYPE_LENGTH;
			locallength = ByteArray.bytesToInt(ByteArray.subByte(nlri, byteOffset, LENGTH_SIZE));
			byteOffset += LENGTH_SIZE;
			if (localtype == 256) {
				localDescriptor = parseNodeDescriptors(ByteArray.subByte(nlri, byteOffset, locallength), true);
			}
			byteOffset += locallength;
			builder.setLocalNodeDescriptors((LocalNodeDescriptors) localDescriptor);
			final int restLength = length - (this.isVpn ? ROUTE_DISTINGUISHER_LENGTH : 0) - PROTOCOL_ID_LENGTH - IDENTIFIER_LENGTH
					- TYPE_LENGTH - LENGTH_SIZE - locallength;
			logger.trace("Restlength {}", restLength);
			switch (type) {
			case Link:
				parseLink(builder, ByteArray.subByte(nlri, byteOffset, restLength));
				break;
			case Ipv4Prefix:
			case Ipv6Prefix:
				builder.setPrefixDescriptors(parsePrefixDescriptors(localDescriptor, ByteArray.subByte(nlri, byteOffset, restLength)));
				break;
			case Node:
				// node nlri is already parsed as it contains only the common fields for node and link nlri
				break;
			}
			byteOffset += restLength;
			dests.add(builder.build());
		}
		return dests;
	}

	@Override
	public final void parseNlri(final byte[] nlri, final MpUnreachNlriBuilder builder) throws BGPParsingException {
		if (nlri.length == 0) {
			return;
		}
		final List<CLinkstateDestination> dst = parseNlri(nlri);

		builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
				new DestinationLinkstateBuilder().setCLinkstateDestination(dst).build()).build());
	}

	@Override
	public void parseNlri(final byte[] nlri, final byte[] nextHop, final MpReachNlriBuilder builder) throws BGPParsingException {
		final List<CLinkstateDestination> dst = parseNlri(nlri);

		builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
				new DestinationLinkstateBuilder().setCLinkstateDestination(dst).build()).build());
		NlriUtil.parseNextHop(nextHop, builder);
	}

	public static byte[] serializeNlri(final CLinkstateDestination destination) {
		// FIXME: BUG-108: finish this
		throw new UnsupportedOperationException("BUG-108: not implemented");
	}
}
