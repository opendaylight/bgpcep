/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.testtool;

import io.netty.util.concurrent.GlobalEventExecutor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import org.opendaylight.protocol.framework.DispatcherImpl;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.protocol.framework.SessionPreferences;
import org.opendaylight.protocol.pcep.PCEPConnection;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPSessionPreferences;
import org.opendaylight.protocol.pcep.PCEPSessionProposal;
import org.opendaylight.protocol.pcep.PCEPSessionProposalChecker;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.PCEPTlv;
import org.opendaylight.protocol.pcep.impl.PCEPDispatcherImpl;
import org.opendaylight.protocol.pcep.impl.PCEPMessageFactory;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;
import org.opendaylight.protocol.pcep.tlv.NodeIdentifierTlv;

import com.google.common.collect.Lists;

public class PCCMock {

	public static void main(final String[] args) throws IOException, InterruptedException {
		final List<PCEPTlv> tlvs = Lists.newArrayList();
		tlvs.add(new NodeIdentifierTlv(new byte[] { (byte) 127, (byte) 2, (byte) 3, (byte) 7 }));
		final PCEPSessionPreferences prop = new PCEPSessionPreferences(new PCEPOpenObject(30, 120, 0, tlvs));
		final DispatcherImpl di = new DispatcherImpl(new PCEPMessageFactory());
		final PCEPDispatcherImpl d = new PCEPDispatcherImpl(di, new PCEPSessionProposalFactory() {

			@Override
			public PCEPSessionProposal getSessionProposal(final InetSocketAddress address, final int sessionId) {
				return new PCEPSessionProposal() {

					@Override
					public PCEPSessionPreferences getProposal() {
						return prop;
					}
				};
			}
		});

		try {

			final PCEPSessionProposalChecker check = new PCEPSessionProposalChecker() {
				@Override
				public Boolean checkSessionCharacteristics(final SessionPreferences openObj) {
					return true;
				}

				@Override
				public PCEPSessionPreferences getNewProposal(final SessionPreferences open) {
					return new PCEPSessionPreferences(new PCEPOpenObject(30, 120, 0, null));
				}
			};

			d.createClient(new PCEPConnection() {
				@Override
				public InetSocketAddress getPeerAddress() {
					return new InetSocketAddress("127.0.0.3", 12345);
				}

				@Override
				public PCEPSessionProposalChecker getProposalChecker() {
					return check;
				}

				@Override
				public PCEPSessionPreferences getProposal() {
					return prop;
				}

				@Override
				public PCEPSessionListener getListener() {
					return new SimpleSessionListener();
				}
			}, new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 2000));
			// Thread.sleep(5000);
			// final List<CompositeRequestObject> cro = new ArrayList<CompositeRequestObject>();
			// cro.add(new CompositeRequestObject(new PCEPRequestParameterObject(false, true, true, true, true, (short)
			// 4, 123, false, false),
			// new PCEPEndPointsObject<IPv4Address>(new IPv4Address(InetAddress.getByName("10.0.0.3")), new
			// IPv4Address(InetAddress.getByName("10.0.0.5")))));
			// for (int i = 0; i < 3; i++) {
			// Thread.sleep(1000);
			// session.sendMessage(new PCEPRequestMessage(cro));
			// }
			// Thread.sleep(5000);
			// Thread.sleep(1000);

		} finally {
			// di.stop();
		}
	}
}
