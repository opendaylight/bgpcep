/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.tunnel.provider;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.bgpcep.pcep.topology.spi.AbstractTopologyProgrammingExecutor;
import org.opendaylight.bgpcep.programming.spi.InstructionExecutor;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.SuccessfulRpcResult;
import org.opendaylight.bgpcep.programming.topology.TopologyProgrammingUtil;
import org.opendaylight.bgpcep.programming.tunnel.TunnelProgrammingUtil;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.BandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.classtype.object.ClassTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.ipv6._case.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObjBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.submit.instruction.output.result.FailureCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.submit.instruction.output.result.failure._case.Failure;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.add.lsp.args.ArgumentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.p2p.rev130819.tunnel.p2p.path.cfg.attributes.ExplicitHops;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepCreateP2pTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepCreateP2pTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepCreateP2pTunnelOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepDestroyTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepDestroyTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepDestroyTunnelOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepUpdateTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepUpdateTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepUpdateTunnelOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.TopologyTunnelPcepProgrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.AdministrativeStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.ExplicitHops1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.Link1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.SupportingNode1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.programming.rev130930.TpReference;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.igp.termination.point.attributes.TerminationPointType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.igp.termination.point.attributes.termination.point.type.Ip;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public final class TunnelProgramming implements TopologyTunnelPcepProgrammingService, AutoCloseable {
	private static final Logger LOG = LoggerFactory.getLogger(TunnelProgramming.class);
	private final NetworkTopologyPcepService topologyService;
	private final DataProviderService dataProvider;
	private final InstructionScheduler scheduler;

	public TunnelProgramming(final InstructionScheduler scheduler, final DataProviderService dataProvider,
			final NetworkTopologyPcepService topologyService) {
		this.scheduler = Preconditions.checkNotNull(scheduler);
		this.dataProvider = Preconditions.checkNotNull(dataProvider);
		this.topologyService = Preconditions.checkNotNull(topologyService);
	}

	private static final class TpReader {
		private final DataModificationTransaction t;
		private final InstanceIdentifier<Node> nii;
		private final InstanceIdentifier<TerminationPoint> tii;

		TpReader(final DataModificationTransaction t, final InstanceIdentifier<Topology> topo, final TpReference ref) {
			this.t = Preconditions.checkNotNull(t);

			this.nii = InstanceIdentifier.builder(topo).child(Node.class, new NodeKey(ref.getNode())).toInstance();
			this.tii = InstanceIdentifier.builder(this.nii).child(TerminationPoint.class, new TerminationPointKey(ref.getTp())).toInstance();
		}

		private Node getNode() {
			return (Node) this.t.readOperationalData(this.nii);
		}

		private TerminationPoint getTp() {
			return (TerminationPoint) this.t.readOperationalData(this.tii);
		}
	}

	private AddressFamily buildAddressFamily(final TerminationPoint sp, final TerminationPoint dp) {
		// We need the IGP augmentation -- it has IP addresses
		final TerminationPoint1 sp1 = Preconditions.checkNotNull(sp.getAugmentation(TerminationPoint1.class));
		final TerminationPoint1 dp1 = Preconditions.checkNotNull(dp.getAugmentation(TerminationPoint1.class));

		// Get the types
		final TerminationPointType spt = sp1.getIgpTerminationPointAttributes().getTerminationPointType();
		final TerminationPointType dpt = dp1.getIgpTerminationPointAttributes().getTerminationPointType();

		// The types have to match
		Preconditions.checkArgument(spt.getImplementedInterface().equals(dpt.getImplementedInterface()));

		// And they have to actually be Ip
		final Ip sips = (Ip) spt;
		final Ip dips = (Ip) dpt;

		/*
		 * Now a bit of magic. We need to find 'like' addresses, e.g. both
		 * IPv4 or both IPv6. We are in IPv6-enabled world now, so let's
		 * prefer that.
		 */
		AddressFamily ret = findIpv6(sips.getIpAddress(), dips.getIpAddress());
		if (ret == null) {
			ret = findIpv4(sips.getIpAddress(), dips.getIpAddress());
		}

		// We need to have a ret now
		Preconditions.checkArgument(ret != null, "Failed to find like Endpoint addresses");

		return ret;
	}

	private AddressFamily findIpv4(final List<IpAddress> srcs, final List<IpAddress> dsts) {
		for (final IpAddress sc : srcs) {
			if (sc.getIpv4Address() != null) {
				for (final IpAddress dc : dsts) {
					if (dc.getIpv4Address() != null) {
						return new Ipv4CaseBuilder().setIpv4(
								new Ipv4Builder().setSourceIpv4Address(sc.getIpv4Address()).setDestinationIpv4Address(dc.getIpv4Address()).build()).build();
					}
				}
			}
		}

		return null;
	}

	private AddressFamily findIpv6(final List<IpAddress> srcs, final List<IpAddress> dsts) {
		for (final IpAddress sc : srcs) {
			if (sc.getIpv6Address() != null) {
				for (final IpAddress dc : dsts) {
					if (dc.getIpv6Address() != null) {
						return new Ipv6CaseBuilder().setIpv6(
								new Ipv6Builder().setSourceIpv6Address(sc.getIpv6Address()).setDestinationIpv6Address(dc.getIpv6Address()).build()).build();
					}
				}
			}
		}

		return null;
	}

	private NodeId supportingNode(final Node node) {
		for (final SupportingNode n : node.getSupportingNode()) {
			final SupportingNode1 n1 = n.getAugmentation(SupportingNode1.class);
			if (n1 != null && n1.getPathComputationClient().isControlling()) {
				return n.getKey().getNodeRef();
			}
		}

		return null;
	}

	private Ero buildEro(final List<ExplicitHops> explicitHops) {
		final EroBuilder b = new EroBuilder();

		if (!explicitHops.isEmpty()) {
			final List<Subobjects> subobjs = new ArrayList<>(explicitHops.size());
			for (final ExplicitHops h : explicitHops) {

				final ExplicitHops1 h1 = h.getAugmentation(ExplicitHops1.class);
				if (h1 != null) {
					final SubobjectsBuilder sb = new SubobjectsBuilder();
					sb.fieldsFrom(h1);
					sb.setLoose(h.isLoose());
					subobjs.add(sb.build());
				} else {
					LOG.debug("Ignoring unhandled explicit hop {}", h);
				}
			}
			b.setSubobjects(subobjs);
		}
		return b.build();
	}

	@Override
	public ListenableFuture<RpcResult<PcepCreateP2pTunnelOutput>> pcepCreateP2pTunnel(final PcepCreateP2pTunnelInput input) {
		final InstructionExecutor e = new AbstractTopologyProgrammingExecutor() {
			@Override
			protected ListenableFuture<OperationResult> executeImpl() {
				final InstanceIdentifier<Topology> tii = TopologyProgrammingUtil.topologyForInput(input);

				final DataModificationTransaction t = TunnelProgramming.this.dataProvider.beginTransaction();

				final TpReader dr = new TpReader(t, tii, input.getDestination());
				final TpReader sr = new TpReader(t, tii, input.getSource());

				final Node sn = Preconditions.checkNotNull(sr.getNode());
				final TerminationPoint sp = Preconditions.checkNotNull(sr.getTp());
				final TerminationPoint dp = Preconditions.checkNotNull(dr.getTp());

				final AddLspInputBuilder ab = new AddLspInputBuilder();
				ab.setNode(Preconditions.checkNotNull(supportingNode(sn)));
				ab.setName(Preconditions.checkNotNull(input.getSymbolicPathName()));

				// The link has to be non-existent
				final InstanceIdentifier<Link> lii = NodeChangedListener.linkIdentifier(tii, ab.getNode(), ab.getName());
				Preconditions.checkState(t.readOperationalData(lii) == null);

				final ArgumentsBuilder args = new ArgumentsBuilder();
				args.setAdministrative(input.getAdministrativeStatus() == AdministrativeStatus.Active);
				args.setBandwidth(new BandwidthBuilder().setBandwidth(input.getBandwidth()).build());
				args.setClassType(new ClassTypeBuilder().setClassType(input.getClassType()).build());
				args.setEndpointsObj(new EndpointsObjBuilder().setAddressFamily(buildAddressFamily(sp, dp)).build());
				args.setEro(buildEro(input.getExplicitHops()));
				args.setLspa(new LspaBuilder(input).build());

				ab.setArguments(args.build());

				return Futures.transform(
						(ListenableFuture<RpcResult<AddLspOutput>>) TunnelProgramming.this.topologyService.addLsp(ab.build()),
						new Function<RpcResult<AddLspOutput>, OperationResult>() {
							@Override
							public OperationResult apply(final RpcResult<AddLspOutput> input) {
								return input.getResult();
							}
						});
			}
		};

		final Failure f = this.scheduler.submitInstruction(input, e);
		final PcepCreateP2pTunnelOutputBuilder b = new PcepCreateP2pTunnelOutputBuilder();
		if (f != null) {
			b.setResult(new FailureCaseBuilder().setFailure(f).build());
		}

		final RpcResult<PcepCreateP2pTunnelOutput> res = SuccessfulRpcResult.create(b.build());
		return Futures.immediateFuture(res);
	}

	private Node sourceNode(final DataModificationTransaction t, final InstanceIdentifier<Topology> topology, final Link link) {
		final InstanceIdentifier<Node> nii = InstanceIdentifier.builder(topology).child(Node.class,
				new NodeKey(link.getSource().getSourceNode())).toInstance();
		return (Node) t.readOperationalData(nii);
	}

	@Override
	public ListenableFuture<RpcResult<PcepDestroyTunnelOutput>> pcepDestroyTunnel(final PcepDestroyTunnelInput input) {
		final InstructionExecutor e = new AbstractTopologyProgrammingExecutor() {
			@Override
			protected ListenableFuture<OperationResult> executeImpl() {
				final InstanceIdentifier<Topology> tii = TopologyProgrammingUtil.topologyForInput(input);
				final InstanceIdentifier<Link> lii = TunnelProgrammingUtil.linkIdentifier(tii, input);

				final DataModificationTransaction t = TunnelProgramming.this.dataProvider.beginTransaction();

				// The link has to exist
				final Link link = (Link) t.readOperationalData(lii);
				Preconditions.checkState(link != null);

				// The source node has to exist
				final Node node = sourceNode(t, tii, link);
				Preconditions.checkState(node != null);

				final RemoveLspInputBuilder ab = new RemoveLspInputBuilder();
				ab.setName(link.getAugmentation(Link1.class).getSymbolicPathName());
				ab.setNode(node.getSupportingNode().get(0).getKey().getNodeRef());

				return Futures.transform(
						(ListenableFuture<RpcResult<RemoveLspOutput>>) TunnelProgramming.this.topologyService.removeLsp(ab.build()),
						new Function<RpcResult<RemoveLspOutput>, OperationResult>() {
							@Override
							public OperationResult apply(final RpcResult<RemoveLspOutput> input) {
								return input.getResult();
							}
						});
			}
		};

		final Failure f = this.scheduler.submitInstruction(input, e);
		final PcepDestroyTunnelOutputBuilder b = new PcepDestroyTunnelOutputBuilder();
		if (f != null) {
			b.setResult(new FailureCaseBuilder().setFailure(f).build());
		}

		final RpcResult<PcepDestroyTunnelOutput> res = SuccessfulRpcResult.create(b.build());
		return Futures.immediateFuture(res);
	}

	@Override
	public ListenableFuture<RpcResult<PcepUpdateTunnelOutput>> pcepUpdateTunnel(final PcepUpdateTunnelInput input) {
		final InstructionExecutor e = new AbstractTopologyProgrammingExecutor() {
			@Override
			protected ListenableFuture<OperationResult> executeImpl() {
				final InstanceIdentifier<Topology> tii = TopologyProgrammingUtil.topologyForInput(input);
				final InstanceIdentifier<Link> lii = TunnelProgrammingUtil.linkIdentifier(tii, input);

				final DataModificationTransaction t = TunnelProgramming.this.dataProvider.beginTransaction();

				// The link has to exist
				final Link link = (Link) t.readOperationalData(lii);
				Preconditions.checkState(link != null);

				// The source node has to exist
				final Node node = sourceNode(t, tii, link);
				Preconditions.checkState(node != null);

				final UpdateLspInputBuilder ab = new UpdateLspInputBuilder();
				ab.setName(link.getAugmentation(Link1.class).getSymbolicPathName());
				ab.setNode(Preconditions.checkNotNull(supportingNode(node)));

				final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.update.lsp.args.ArgumentsBuilder args = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.update.lsp.args.ArgumentsBuilder();

				args.setAdministrative(input.getAdministrativeStatus() == AdministrativeStatus.Active);
				args.setBandwidth(new BandwidthBuilder().setBandwidth(input.getBandwidth()).build());
				args.setClassType(new ClassTypeBuilder().setClassType(input.getClassType()).build());
				args.setEro(buildEro(input.getExplicitHops()));
				args.setLspa(new LspaBuilder(input).build());

				ab.setArguments(args.build());

				return Futures.transform(
						(ListenableFuture<RpcResult<UpdateLspOutput>>) TunnelProgramming.this.topologyService.updateLsp(ab.build()),
						new Function<RpcResult<UpdateLspOutput>, OperationResult>() {
							@Override
							public OperationResult apply(final RpcResult<UpdateLspOutput> input) {
								return input.getResult();
							}
						});
			}
		};

		final Failure f = this.scheduler.submitInstruction(input, e);
		final PcepUpdateTunnelOutputBuilder b = new PcepUpdateTunnelOutputBuilder();
		if (f != null) {
			b.setResult(new FailureCaseBuilder().setFailure(f).build());
		}

		final RpcResult<PcepUpdateTunnelOutput> res = SuccessfulRpcResult.create(b.build());
		return Futures.immediateFuture(res);
	}

	@Override
	public void close() {
		// FIXME: BUG-186: remove all scheduled instructions, etc.
	}
}
