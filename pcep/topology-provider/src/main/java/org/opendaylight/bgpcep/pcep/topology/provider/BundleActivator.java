/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPDispatcherImpl;
import org.opendaylight.protocol.pcep.impl.PCEPSessionProposalFactoryImpl;
import org.opendaylight.protocol.pcep.spi.pojo.PCEPExtensionProviderContextImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public final class BundleActivator extends AbstractBindingAwareProvider {
	private static final Logger LOG = LoggerFactory.getLogger(BundleActivator.class);

	@Override
	public void onSessionInitiated(final ProviderContext session) {
		final DataProviderService dps = Preconditions.checkNotNull(session.getSALService(DataProviderService.class));

		// FIXME: integration with config subsystem should allow this to be injected as a service
		final InetSocketAddress address = new InetSocketAddress("0.0.0.0", 4189);
		final PCEPSessionProposalFactory spf = new PCEPSessionProposalFactoryImpl(30, 10, true, true, true, true, 0);
		final Open prefs = spf.getSessionProposal(address, 0);
		final PCEPDispatcher dispatcher = new PCEPDispatcherImpl(PCEPExtensionProviderContextImpl
				.getSingletonInstance().getMessageHandlerRegistry(), new DefaultPCEPSessionNegotiatorFactory(
						new HashedWheelTimer(), prefs, 5), new NioEventLoopGroup(), new NioEventLoopGroup());

		final InstanceIdentifier<Topology> topology = InstanceIdentifier.builder().node(Topology.class).toInstance();

		final PCEPTopologyProvider exp = new PCEPTopologyProvider(dispatcher, null, dps, topology);
		final ChannelFuture s = exp.startServer(address);
		try {
			s.get();
		} catch (InterruptedException | ExecutionException e) {
			LOG.error("Failed to instantiate server", e);
		}
	}
}
