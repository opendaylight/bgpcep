/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.testtool;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.impl.BasePCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCDispatcherImpl;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;
import org.opendaylight.protocol.util.InetSocketAddressUtil;

public class PCCMock {

    public static void main(final String[] args) throws InterruptedException, ExecutionException {
        final List<PCEPCapability> caps = new ArrayList<>();
        final PCEPSessionProposalFactory proposal = new BasePCEPSessionProposalFactory((short) 120, (short) 30, caps);
        final PCEPSessionNegotiatorFactory snf = new DefaultPCEPSessionNegotiatorFactory(proposal, 0);
        final InetSocketAddress clientAddr = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress();

        try (final PCCDispatcherImpl pccDispatcher = new PCCDispatcherImpl(ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance().getMessageHandlerRegistry())) {
            pccDispatcher.createClient(clientAddr, -1, new PCEPSessionListenerFactory() {
                @Override
                public PCEPSessionListener getSessionListener() {
                    return new SimpleSessionListener();
                }
            }, snf, null, clientAddr).get();
        }
    }
}
