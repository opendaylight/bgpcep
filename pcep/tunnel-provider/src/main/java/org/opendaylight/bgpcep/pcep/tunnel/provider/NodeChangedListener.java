/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.tunnel.provider;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.AdministrativeStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.ReportedLsp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.identifiers.tlv.lsp.identifiers.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.ipv4._case.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.ipv6._case.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.PathComputationClient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.ReportedLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.Link1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.SupportingNode1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.SupportingNode1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.tunnel.pcep.supporting.node.attributes.PathComputationClientBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.IgpTerminationPointAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.igp.termination.point.attributes.TerminationPointType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.igp.termination.point.attributes.termination.point.type.Ip;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.igp.termination.point.attributes.termination.point.type.IpBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;

public final class NodeChangedListener implements DataChangeListener {
	public static final Logger LOG = LoggerFactory.getLogger(NodeChangedListener.class);
	private final InstanceIdentifier<Topology> target;
	private final DataProviderService dataProvider;

	NodeChangedListener(final DataProviderService dataProvider, final InstanceIdentifier<Topology> target) {
		this.dataProvider = Preconditions.checkNotNull(dataProvider);
		this.target = Preconditions.checkNotNull(target);
	}

	private static void categorizeIdentifier(final InstanceIdentifier<?> i, final Set<InstanceIdentifier<ReportedLsp>> changedLsps,
			final Set<InstanceIdentifier<Node>> changedNodes) {
		final InstanceIdentifier<ReportedLsp> li = i.firstIdentifierOf(ReportedLsp.class);
		if (li == null) {
			final InstanceIdentifier<Node> ni = i.firstIdentifierOf(Node.class);
			if (ni == null) {
				LOG.warn("Ignoring uncategorized identifier {}", i);
			} else {
				changedNodes.add(ni);
			}
		} else {
			changedLsps.add(li);
		}
	}

	private static void enumerateLsps(final InstanceIdentifier<Node> id, final Node node, final Set<InstanceIdentifier<ReportedLsp>> lsps) {
		if (node == null) {
			LOG.trace("Skipping null node", id);
			return;
		}
		final Node1 pccnode = node.getAugmentation(Node1.class);
		if (pccnode == null) {
			LOG.trace("Skipping non-PCEP-enabled node {}", id);
			return;
		}

		for (final ReportedLsp l : pccnode.getPathComputationClient().getReportedLsp()) {
			lsps.add(InstanceIdentifier.builder(id).augmentation(Node1.class).child(PathComputationClient.class).child(ReportedLsp.class,
					l.getKey()).toInstance());
		}
	}

	private static LinkId linkIdForLsp(final InstanceIdentifier<ReportedLsp> i, final ReportedLsp lsp) {
		return new LinkId(i.firstKeyOf(Node.class, NodeKey.class).getNodeId().getValue() + "/lsps/" + lsp.getName());
	}

	private InstanceIdentifier<Link> linkForLsp(final LinkId linkId) {
		return InstanceIdentifier.builder(this.target).child(Link.class, new LinkKey(linkId)).toInstance();
	}

	private SupportingNode createSupportingNode(final NodeId sni, final Boolean inControl) {
		final SupportingNodeKey sk = new SupportingNodeKey(sni);
		final SupportingNodeBuilder snb = new SupportingNodeBuilder();
		snb.setNodeRef(sni);
		snb.setKey(sk);
		snb.addAugmentation(
				SupportingNode1.class,
				new SupportingNode1Builder().setPathComputationClient(new PathComputationClientBuilder().setControlling(inControl).build()).build());

		return snb.build();
	}

	private InstanceIdentifier<TerminationPoint> getIpTerminationPoint(final DataModificationTransaction trans, final IpAddress addr,
			final InstanceIdentifier<Node> sni, final Boolean inControl) {
		for (final Node n : ((Topology) trans.readOperationalData(this.target)).getNode()) {
			for (final TerminationPoint tp : n.getTerminationPoint()) {
				final TerminationPoint1 tpa = tp.getAugmentation(TerminationPoint1.class);

				if (tpa != null) {
					final TerminationPointType tpt = tpa.getIgpTerminationPointAttributes().getTerminationPointType();

					if (tpt instanceof Ip) {
						for (final IpAddress a : ((Ip) tpt).getIpAddress()) {
							if (addr.equals(a.getIpv6Address())) {
								if (sni != null) {
									final NodeKey k = InstanceIdentifier.keyOf(sni);
									boolean have = false;

									/*
									 * We may have found a termination point which has been created as a destination,
									 * so it does not have a supporting node pointer. Since we now know what it is,
									 * fill it in.
									 */
									for (final SupportingNode sn : n.getSupportingNode()) {
										if (sn.getNodeRef().equals(k.getNodeId())) {
											have = true;
											break;
										}
									}

									if (!have) {
										final SupportingNode sn = createSupportingNode(k.getNodeId(), inControl);

										trans.putOperationalData(
												InstanceIdentifier.builder(this.target).child(Node.class, n.getKey()).child(
														SupportingNode.class, sn.getKey()).toInstance(), sn);
									}
								}
								return InstanceIdentifier.builder(this.target).child(Node.class, n.getKey()).child(TerminationPoint.class,
										tp.getKey()).toInstance();
							}
						}
					} else {
						LOG.debug("Ignoring termination point type {}", tpt);
					}
				}
			}
		}

		LOG.debug("Termination point for {} not found, creating a new one", addr);

		final String url = "ip://" + addr.toString();
		final TerminationPointKey tpk = new TerminationPointKey(new TpId(url));
		final TerminationPointBuilder tpb = new TerminationPointBuilder();
		tpb.setKey(tpk).setTpId(tpk.getTpId());
		tpb.addAugmentation(
				TerminationPoint1.class,
				new TerminationPoint1Builder().setIgpTerminationPointAttributes(
						new IgpTerminationPointAttributesBuilder().setTerminationPointType(
								new IpBuilder().setIpAddress(Lists.newArrayList(addr)).build()).build()).build());

		final NodeKey nk = new NodeKey(new NodeId(url));
		final NodeBuilder nb = new NodeBuilder();
		nb.setKey(nk).setNodeId(nk.getNodeId());
		nb.setTerminationPoint(Lists.newArrayList(tpb.build()));
		if (sni != null) {
			nb.setSupportingNode(Lists.newArrayList(createSupportingNode(InstanceIdentifier.keyOf(sni).getNodeId(), inControl)));
		}

		trans.putOperationalData(InstanceIdentifier.builder(this.target).child(Node.class, nb.getKey()).toInstance(), nb.build());
		return InstanceIdentifier.builder(this.target).child(Node.class, nb.getKey()).child(TerminationPoint.class, tpb.getKey()).toInstance();
	}

	private void create(final DataModificationTransaction trans, final InstanceIdentifier<ReportedLsp> i, final ReportedLsp value) {
		final InstanceIdentifier<Node> ni = i.firstIdentifierOf(Node.class);
		final ReportedLsp1 rl = value.getAugmentation(ReportedLsp1.class);

		final AddressFamily af = rl.getLsp().getTlvs().getLspIdentifiers().getAddressFamily();

		/*
		 * We are trying to ensure we have source and destination nodes.
		 */
		final IpAddress srcIp, dstIp;
		if (af instanceof Ipv4Case) {
			final Ipv4 ipv4 = ((Ipv4Case) af).getIpv4();
			srcIp = new IpAddress(ipv4.getSourceIpv4Address());
			dstIp = new IpAddress(ipv4.getDestinationIpv4Address());
		} else if (af instanceof Ipv6Case) {
			final Ipv6 ipv6 = ((Ipv6Case) af).getIpv6();
			srcIp = new IpAddress(ipv6.getSourceIpv6Address());
			dstIp = new IpAddress(ipv6.getDestinationIpv6Address());
		} else {
			throw new IllegalArgumentException("Unsupported address family: " + af.getImplementedInterface());
		}

		final Link1Builder lab = new Link1Builder(value.getPath().getLspa());
		lab.setBandwidth(value.getPath().getBandwidth().getBandwidth());
		lab.setClassType(value.getPath().getClassType().getClassType());
		lab.setSymbolicPathName(value.getName());

		final InstanceIdentifier<TerminationPoint> dst = getIpTerminationPoint(trans, dstIp, null, Boolean.FALSE);
		final InstanceIdentifier<TerminationPoint> src = getIpTerminationPoint(trans, srcIp, ni, rl.getLsp().isDelegate());

		final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Link1Builder slab = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Link1Builder();
		slab.setOperationalStatus(rl.getLsp().getOperational());
		slab.setAdministrativeStatus(rl.getLsp().isAdministrative() ? AdministrativeStatus.Active : AdministrativeStatus.Inactive);

		final LinkId id = linkIdForLsp(i, value);
		final LinkBuilder lb = new LinkBuilder();
		lb.setLinkId(id);

		lb.setSource(new SourceBuilder().setSourceNode(src.firstKeyOf(Node.class, NodeKey.class).getNodeId()).setSourceTp(
				src.firstKeyOf(TerminationPoint.class, TerminationPointKey.class).getTpId()).build());
		lb.setDestination(new DestinationBuilder().setDestNode(dst.firstKeyOf(Node.class, NodeKey.class).getNodeId()).setDestTp(
				dst.firstKeyOf(TerminationPoint.class, TerminationPointKey.class).getTpId()).build());
		lb.addAugmentation(Link1.class, lab.build());
		lb.addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Link1.class, slab.build());

		trans.putOperationalData(linkForLsp(id), lb.build());
	}

	private InstanceIdentifier<TerminationPoint> tpIdentifier(final NodeId node, final TpId tp) {
		return InstanceIdentifier.builder(this.target).child(Node.class, new NodeKey(node)).child(TerminationPoint.class, new TerminationPointKey(tp)).toInstance();
	}

	private InstanceIdentifier<Node> nodeIdentifier(final NodeId node) {
		return InstanceIdentifier.builder(this.target).child(Node.class, new NodeKey(node)).toInstance();
	}

	private void remove(final DataModificationTransaction trans, final InstanceIdentifier<ReportedLsp> i, final ReportedLsp value) {
		final InstanceIdentifier<Link> li = linkForLsp(linkIdForLsp(i, value));

		final Link l = (Link) trans.readOperationalData(li);
		if (l != null) {
			LOG.debug("Removing link {} (was {})", li, l);
			trans.removeOperationalData(li);

			LOG.debug("Searching for orphan links/nodes");
			final Topology t = (Topology) trans.readOperationalData(InstanceIdentifier.builder(this.target).toInstance());

			NodeId srcNode = l.getSource().getSourceNode();
			NodeId dstNode = l.getDestination().getDestNode();
			TpId srcTp = l.getSource().getSourceTp();
			TpId dstTp = l.getDestination().getDestTp();

			boolean orphSrcNode = true, orphDstNode = true, orphDstTp = true, orphSrcTp = true;
			for (final Link lw : t.getLink()) {
				LOG.trace("Checking link {}", lw);

				final NodeId sn = lw.getSource().getSourceNode();
				final NodeId dn = lw.getDestination().getDestNode();
				final TpId st = lw.getSource().getSourceTp();
				final TpId dt = lw.getDestination().getDestTp();

				// Source node checks
				if (srcNode.equals(sn)) {
					if (orphSrcNode) {
						LOG.debug("Node {} held by source of link {}", srcNode, lw);
						orphSrcNode = false;
					}
					if (orphSrcTp && srcTp.equals(st)) {
						LOG.debug("TP {} held by source of link {}", srcTp, lw);
						orphSrcTp = false;
					}
				}
				if (srcNode.equals(dn)) {
					if (orphSrcNode) {
						LOG.debug("Node {} held by destination of link {}", srcNode, lw);
						orphSrcNode = false;
					}
					if (orphSrcTp && srcTp.equals(dt)) {
						LOG.debug("TP {} held by destination of link {}", srcTp, lw);
						orphSrcTp = false;
					}
				}

				// Destination node checks
				if (dstNode.equals(sn)) {
					if (orphDstNode) {
						LOG.debug("Node {} held by source of link {}", dstNode, lw);
						orphDstNode = false;
					}
					if (orphDstTp && dstTp.equals(st)) {
						LOG.debug("TP {} held by source of link {}", dstTp, lw);
						orphDstTp = false;
					}
				}
				if (dstNode.equals(dn)) {
					if (orphDstNode) {
						LOG.debug("Node {} held by destination of link {}", dstNode, lw);
						orphDstNode = false;
					}
					if (orphDstTp && dstTp.equals(dt)) {
						LOG.debug("TP {} held by destination of link {}", dstTp, lw);
						orphDstTp = false;
					}
				}
			}

			if (orphSrcNode && !orphSrcTp) {
				LOG.warn("Orphan source node {} but not TP {}, retaining the node", srcNode, srcTp);
				orphSrcNode = false;
			}
			if (orphDstNode && !orphDstTp) {
				LOG.warn("Orphan destination node {} but not TP {}, retaining the node", dstNode, dstTp);
				orphDstNode = false;
			}

			if (orphSrcNode) {
				LOG.debug("Removing orphan node {}", srcNode);
				trans.removeOperationalData(nodeIdentifier(srcNode));
			} else if (orphSrcTp) {
				LOG.debug("Removing orphan TP {} on node {}", srcTp, srcNode);
				trans.removeOperationalData(tpIdentifier(srcNode, srcTp));
			}
			if (orphDstNode) {
				LOG.debug("Removing orphan node {}", dstNode);
				trans.removeOperationalData(nodeIdentifier(dstNode));
			} else if (orphDstTp) {
				LOG.debug("Removing orphan TP {} on node {}", dstTp, dstNode);
				trans.removeOperationalData(tpIdentifier(dstNode, dstTp));
			}
		}
	}

	@Override
	public void onDataChanged(final DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
		final DataModificationTransaction trans = this.dataProvider.beginTransaction();

		final Set<InstanceIdentifier<ReportedLsp>> lsps = new HashSet<>();
		final Set<InstanceIdentifier<Node>> nodes = new HashSet<>();

		// Categorize reported identifiers
		for (final InstanceIdentifier<?> i : change.getRemovedOperationalData()) {
			categorizeIdentifier(i, lsps, nodes);
		}
		for (final InstanceIdentifier<?> i : change.getUpdatedOperationalData().keySet()) {
			categorizeIdentifier(i, lsps, nodes);
		}
		for (final InstanceIdentifier<?> i : change.getCreatedOperationalData().keySet()) {
			categorizeIdentifier(i, lsps, nodes);
		}

		// Get the subtrees
		final Map<InstanceIdentifier<?>, DataObject> o = change.getOriginalOperationalData();
		final Map<InstanceIdentifier<?>, DataObject> n = change.getUpdatedOperationalData();

		// Now walk all nodes, check for removals/additions and cascade them to LSPs
		for (final InstanceIdentifier<Node> i : nodes) {
			enumerateLsps(i, (Node) o.get(i), lsps);
			enumerateLsps(i, (Node) n.get(i), lsps);
		}

		// We now have list of all affected LSPs. Walk them create/remove them
		for (final InstanceIdentifier<ReportedLsp> i : lsps) {
			final ReportedLsp oldValue = (ReportedLsp) o.get(i);
			final ReportedLsp newValue = (ReportedLsp) n.get(i);

			LOG.debug("Updating lsp {} value {} -> {}", i, oldValue, newValue);
			if (oldValue != null) {
				remove(trans, i, oldValue);
			}
			if (newValue != null) {
				create(trans, i, newValue);
			}
		}

		Futures.addCallback(JdkFutureAdapters.listenInPoolThread(trans.commit()), new FutureCallback<RpcResult<TransactionStatus>>() {
			@Override
			public void onSuccess(final RpcResult<TransactionStatus> result) {
				LOG.trace("Topology change committed successfully");
			}

			@Override
			public void onFailure(final Throwable t) {
				LOG.error("Failed to propagate a topology change, target topology became inconsistent", t);
			}
		});
	}

	public static InstanceIdentifier<Link> linkIdentifier(final InstanceIdentifier<Topology> topology, final NodeId node,
			final String name) {
		return InstanceIdentifier.builder(topology).child(Link.class, new LinkKey(new LinkId(node.getValue() + "/lsp/" + name))).toInstance();
	}
}
