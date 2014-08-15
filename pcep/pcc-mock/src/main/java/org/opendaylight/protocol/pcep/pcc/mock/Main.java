/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPHandlerFactory;
import org.opendaylight.protocol.pcep.impl.PCEPSessionImpl;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static final int DEFAULT_PORT = 4189;

    public static void main(String[] args) throws InterruptedException, ExecutionException, UnknownHostException {

        if (args.length < 2) {
            LOG.info("Insufficient number of arguments {}.", args.length);
            return;
        }

        InetAddress address = null;
        int pccCount = 1;
        int lsps = 1;
        boolean pcError = false;

        int argIdx = 0;
        while (argIdx < args.length) {
            if (args[argIdx].equals("--address")) {
                address = InetAddress.getByName(args[argIdx + 1]);
                argIdx++;
            } else if (args[argIdx].equals("--pcc")) {
                pccCount = Integer.valueOf(args[argIdx + 1]);
                argIdx++;
            } else if (args[argIdx].equals("--lsp")) {
                lsps = Integer.valueOf(args[argIdx + 1]);
                argIdx++;
            } else if (args[argIdx].equals("--pcerr")) {
                pcError = true;
                argIdx++;
            } else {
                LOG.warn("WARNING: Unrecognized argument: {}", args[argIdx]);
            }
            argIdx++;
        }
        Preconditions.checkState(address != null, "Missing mandatory address parameter.");
        createPCCs(lsps, pcError, pccCount, address);
    }

    public static void createPCCs(final int lspsPerPcc, final boolean pcerr, final int pccCount,
            final InetAddress address) throws InterruptedException, ExecutionException {
        final SessionNegotiatorFactory<Message, PCEPSessionImpl, PCEPSessionListener> snf = new DefaultPCEPSessionNegotiatorFactory(
                new OpenBuilder().setKeepalive((short) 30).setDeadTimer((short) 120).setSessionId((short) 0).build(), 0);

        final PCCMock<Message, PCEPSessionImpl, PCEPSessionListener> pcc = new PCCMock<>(snf, new PCEPHandlerFactory(
                ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance().getMessageHandlerRegistry()),
                new DefaultPromise<PCEPSessionImpl>(GlobalEventExecutor.INSTANCE));

        InetAddress inetAddress = address;
        int i = 0;
        while (i < pccCount) {
            final int pccNumber = i + 1;
            final InetAddress pccAddress = inetAddress;
            pcc.createClient(new InetSocketAddress(pccAddress, DEFAULT_PORT),
                    new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 2000),
                    new SessionListenerFactory<PCEPSessionListener>() {

                        @Override
                        public PCEPSessionListener getSessionListener() {
                            return new SimpleSessionListener(lspsPerPcc, pcerr, pccNumber, pccAddress);
                        }
                    }).get();
            i++;
            inetAddress = InetAddresses.increment(inetAddress);
        }
    }

}
