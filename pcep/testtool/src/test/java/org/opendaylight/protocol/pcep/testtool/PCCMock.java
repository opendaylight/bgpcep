/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.testtool;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.net.HostAndPort;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.PCEPTimerProposal;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCDispatcherImpl;
import org.opendaylight.protocol.pcep.spi.pojo.DefaultPCEPExtensionConsumerContext;
import org.opendaylight.protocol.util.InetSocketAddressUtil;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint8;

public final class PCCMock {
    private PCCMock() {
        // Hidden on purpose
    }

    public static void main(final String[] args) throws InterruptedException, ExecutionException {
        checkArgument(args.length > 0, "Host and port of server must be provided.");
        final var snf = new DefaultPCEPSessionNegotiatorFactory(SimpleSessionListener::new,
            new PCEPTimerProposal(Uint8.valueOf(30), Uint8.valueOf(120)), List.of(), Uint16.ZERO, null);
        final var serverHostAndPort = HostAndPort.fromString(args[0]);
        final var serverAddr = new InetSocketAddress(serverHostAndPort.getHost(),
            serverHostAndPort.getPortOrDefault(12345));
        final var clientAddr = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(0);

        try (var pccDispatcher = new PCCDispatcherImpl(
                new DefaultPCEPExtensionConsumerContext().getMessageHandlerRegistry())) {
            pccDispatcher.createClient(serverAddr, -1, snf, KeyMapping.of(), clientAddr).get();
        }
    }
}
