/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.testtool;

import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.net.InetSocketAddress;
import java.util.List;

import org.opendaylight.protocol.framework.DispatcherImpl;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.protocol.pcep.PCEPTlv;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPDispatcherImpl;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;
import org.opendaylight.protocol.pcep.tlv.NodeIdentifierTlv;

import com.google.common.collect.Lists;

public class PCCMock {

	public static void main(final String[] args) throws Exception {
		final List<PCEPTlv> tlvs = Lists.newArrayList();
		tlvs.add(new NodeIdentifierTlv(new byte[] { (byte) 127, (byte) 2, (byte) 3, (byte) 7 }));

		final DispatcherImpl di = new DispatcherImpl();
		final PCEPDispatcherImpl d = new PCEPDispatcherImpl(di, new DefaultPCEPSessionNegotiatorFactory(new HashedWheelTimer(), new PCEPOpenObject(30, 120, 0, tlvs), 0));

		try {
			d.createClient(new InetSocketAddress("127.0.0.3", 12345), new SimpleSessionListener(),
					new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 2000)).get();

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
