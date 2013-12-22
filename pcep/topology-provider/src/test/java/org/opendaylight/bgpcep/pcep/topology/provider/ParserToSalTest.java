/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Promise;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiator;
import org.opendaylight.protocol.pcep.impl.PCEPSessionImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PcrptBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.PcrptMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class ParserToSalTest {

	private static final Logger LOG = LoggerFactory.getLogger(ParserToSalTest.class);

	private List<Notification> receivedMsgs;

	private PCEPSessionImpl session;

	@Mock
	private Channel clientListener;

	@Mock
	private ChannelPipeline pipeline;

	@Mock
	DataProviderService providerService;

	@Mock
	DataModificationTransaction mockedTransaction;

	private final Open localPrefs = new OpenBuilder().setDeadTimer((short) 30).setKeepalive((short) 10).setTlvs(
			new TlvsBuilder().setStateful(new StatefulBuilder().build()).build()).build();

	private Pcrpt rptmsg;

	private ServerSessionManager manager;

	@Before
	public void setUp() throws IOException {
		MockitoAnnotations.initMocks(this);

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(final InvocationOnMock invocation) {
				final Object[] args = invocation.getArguments();
				ParserToSalTest.this.receivedMsgs.add((Notification) args[0]);
				return mock(ChannelFuture.class);
			}
		}).when(this.clientListener).writeAndFlush(any(Notification.class));
		doReturn("TestingChannel").when(this.clientListener).toString();
		doReturn(this.pipeline).when(this.clientListener).pipeline();
		doReturn(this.pipeline).when(this.pipeline).replace(any(ChannelHandler.class), any(String.class), any(ChannelHandler.class));
		doReturn(true).when(this.clientListener).isActive();
		final SocketAddress ra = new InetSocketAddress("127.0.0.1", 4189);
		doReturn(ra).when(this.clientListener).remoteAddress();
		final SocketAddress la = new InetSocketAddress("127.0.0.1", 30000);
		doReturn(la).when(this.clientListener).localAddress();

		doReturn(mock(ChannelFuture.class)).when(this.clientListener).close();

		Mockito.doReturn(this.mockedTransaction).when(this.providerService).beginTransaction();
		Mockito.doReturn(new Future<RpcResult<TransactionStatus>>() {
			int i = 0;

			@Override
			public boolean cancel(final boolean mayInterruptIfRunning) {
				LOG.debug("Cancel.");
				return false;
			}

			@Override
			public boolean isCancelled() {
				LOG.debug("Is cancelled.");
				return false;
			}

			@Override
			public boolean isDone() {
				this.i++;
				LOG.debug("Done. {}", this.i);
				return true;
			}

			@Override
			public RpcResult<TransactionStatus> get() throws InterruptedException, ExecutionException {
				return null;
			}

			@Override
			public RpcResult<TransactionStatus> get(final long timeout, final TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
				return null;
			}
		}).when(this.mockedTransaction).commit();

		final HashMap<Object, Object> data = new HashMap<>();

		Mockito.doAnswer(new Answer<Object>() {
			@Override
			public Object answer(final InvocationOnMock invocation) throws Throwable {
				final Object[] args = invocation.getArguments();
				LOG.debug("Get key {}", args[0]);
				return data.get(args[0]);
			}

		}).when(this.mockedTransaction).readOperationalData(Matchers.any(InstanceIdentifier.class));

		Mockito.doAnswer(new Answer<Object>() {
			@Override
			public Object answer(final InvocationOnMock invocation) throws Throwable {
				final Object[] args = invocation.getArguments();
				LOG.debug("Get key {}", args[0]);
				return data.get(args[0]);
			}

		}).when(this.providerService).readOperationalData(Matchers.any(InstanceIdentifier.class));

		Mockito.doAnswer(new Answer<Object>() {

			@Override
			public Object answer(final InvocationOnMock invocation) throws Throwable {
				data.remove(invocation.getArguments()[0]);
				return null;
			}

		}).when(this.mockedTransaction).removeOperationalData(Matchers.any(InstanceIdentifier.class));

		Mockito.doAnswer(new Answer<String>() {
			@Override
			public String answer(final InvocationOnMock invocation) throws Throwable {
				final Object[] args = invocation.getArguments();
				LOG.debug("Put key {} value {}", args[0]);
				LOG.debug("Put value {}", args[1]);
				data.put(args[0], args[1]);
				return null;
			}

		}).when(this.mockedTransaction).putOperationalData(Matchers.any(InstanceIdentifier.class), Matchers.any(DataObject.class));

		this.manager = new ServerSessionManager(this.providerService, InstanceIdentifier.builder(NetworkTopology.class).child(
				Topology.class, new TopologyKey(new TopologyId("testtopo"))).toInstance());
		final DefaultPCEPSessionNegotiator neg = new DefaultPCEPSessionNegotiator(new HashedWheelTimer(), mock(Promise.class), this.clientListener, this.manager.getSessionListener(), (short) 1, 5, this.localPrefs);
		this.session = neg.createSession(new HashedWheelTimer(), this.clientListener, this.localPrefs, this.localPrefs);

		final List<Reports> reports = Lists.newArrayList(new ReportsBuilder().setLsp(
				new LspBuilder().setPlspId(new PlspId(5L)).setSync(false).setRemove(false).setTlvs(
						new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.TlvsBuilder().setSymbolicPathName(
								new SymbolicPathNameBuilder().setPathName(new SymbolicPathName(new byte[] { 22, 34 })).build()).build()).build()).build());
		this.rptmsg = new PcrptBuilder().setPcrptMessage(new PcrptMessageBuilder().setReports(reports).build()).build();
	}

	@After
	public void tearDown() throws InterruptedException, ExecutionException {
		this.manager.close();
	}

	@Test
	public void testUnknownLsp() {
		this.session.sessionUp();
		this.session.handleMessage(this.rptmsg);
		Mockito.verify(this.mockedTransaction, Mockito.times(4)).putOperationalData(Matchers.any(InstanceIdentifier.class),
				Matchers.any(DataObject.class));
		Mockito.verify(this.mockedTransaction, Mockito.times(2)).commit();
	}
}
