/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.integration.bgp;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nullable;

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
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.impl.BGPPeer;
import org.opendaylight.protocol.bgp.rib.impl.RIBActivator;
import org.opendaylight.protocol.bgp.rib.impl.RIBImpl;
import org.opendaylight.protocol.bgp.rib.mock.BGPMock;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.util.HexDumpBGPFileParser;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.LocRib;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

public class ParserToSalTest {

	private static final Logger LOG = LoggerFactory.getLogger(ParserToSalTest.class);

	private final String hex_messages = "/bgp_hex.txt";

	private BGPMock mock;

	@Mock
	DataModificationTransaction mockedTransaction;

	@Mock
	DataProviderService providerService;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		final List<byte[]> bgpMessages = HexDumpBGPFileParser.parseMessages(ParserToSalTest.class.getResourceAsStream(this.hex_messages));
		this.mock = new BGPMock(new EventBus("test"), ServiceLoaderBGPExtensionProviderContext.createConsumerContext().getMessageRegistry(), Lists.newArrayList(fixMessages(bgpMessages)));

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

		Mockito.doAnswer(new Answer<Object>() {
			@Override
			public Object answer(final InvocationOnMock invocation) throws Throwable {
				final Object[] args = invocation.getArguments();
				LOG.debug("Get key {}", args[0]);
				return data.get(args[0]);
			}

		}).when(this.mockedTransaction).readOperationalData(Matchers.any(InstanceIdentifier.class));
	}

	@Test
	public void test() {
		final RIBExtensionProviderContext ext = new SimpleRIBExtensionProviderContext();
		new RIBActivator().startRIBExtensionProvider(ext);
		new org.opendaylight.protocol.bgp.linkstate.RIBActivator().startRIBExtensionProvider(ext);
		final RIBImpl rib = new RIBImpl(InstanceIdentifier.builder(LocRib.class).toInstance(), ext, this.providerService);
		final BGPPeer peer = new BGPPeer(rib, "peer-" + this.mock.toString());

		this.mock.registerUpdateListener(peer, new ReconnectStrategyFactory() {
			@Override
			public ReconnectStrategy createReconnectStrategy() {
				return null;
			}
		}, null);
		Mockito.verify(this.mockedTransaction, Mockito.times(24)).commit();
		Mockito.verify(this.mockedTransaction, Mockito.times(67)).putOperationalData(Matchers.any(InstanceIdentifier.class),
				Matchers.any(DataObject.class));
	}

	private Collection<byte[]> fixMessages(final Collection<byte[]> bgpMessages) {
		return Collections2.transform(bgpMessages, new Function<byte[], byte[]>() {

			@Nullable
			@Override
			public byte[] apply(@Nullable final byte[] input) {
				final byte[] ret = new byte[input.length + 1];
				// ff
				ret[0] = -1;
				for (int i = 0; i < input.length; i++) {
					ret[i + 1] = input[i];
				}
				return ret;
			}
		});
	}
}
