/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bmp.impl.message.InitiationHandler;
import org.opendaylight.protocol.bmp.impl.message.PeerDownHandler;
import org.opendaylight.protocol.bmp.impl.message.PeerUpHandler;
import org.opendaylight.protocol.bmp.impl.message.RouteMonitoringMessageHandler;
import org.opendaylight.protocol.bmp.impl.message.StatisticsReportHandler;
import org.opendaylight.protocol.bmp.impl.message.TerminationHandler;
import org.opendaylight.protocol.bmp.spi.registry.AbstractBMPExtensionProviderActivator;
import org.opendaylight.protocol.bmp.spi.registry.BmpExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.InitiationMessage;

/**
 * Created by cgasparini on 15.5.2015.
 */
public final class BmpActivator extends AbstractBMPExtensionProviderActivator {

    private final MessageRegistry messageRegistry;

    public BmpActivator(final MessageRegistry messageRegistry) {
        this.messageRegistry = messageRegistry;
    }

    @Override
    protected List<AutoCloseable> startImpl(final BmpExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>();
        this.registerBGPParsers(regs, context);
        return regs;
    }

    private void registerBGPParsers(final List<AutoCloseable> regs, final BmpExtensionProviderContext context) {
        final InitiationHandler initiationHandler = new InitiationHandler();
        regs.add(context.registerBmpMessageParser(initiationHandler.getBmpMessageType(), initiationHandler));
        regs.add(context.registerBmpMessageSerializer(InitiationMessage.class, initiationHandler));

        final TerminationHandler terminationHandler = new TerminationHandler();
        regs.add(context.registerBmpMessageParser(terminationHandler.getBmpMessageType(), terminationHandler));
        regs.add(context.registerBmpMessageSerializer(InitiationMessage.class, terminationHandler));

        final PeerUpHandler peerUpHandler = new PeerUpHandler(this.messageRegistry);
        regs.add(context.registerBmpMessageParser(peerUpHandler.getBmpMessageType(), peerUpHandler));
        regs.add(context.registerBmpMessageSerializer(InitiationMessage.class, peerUpHandler));

        final PeerDownHandler peerDownHandler = new PeerDownHandler(this.messageRegistry);
        regs.add(context.registerBmpMessageParser(peerDownHandler.getBmpMessageType(), peerDownHandler));
        regs.add(context.registerBmpMessageSerializer(InitiationMessage.class, peerDownHandler));

        final StatisticsReportHandler statisticsReportHandler = new StatisticsReportHandler(this.messageRegistry);
        regs.add(context.registerBmpMessageParser(statisticsReportHandler.getBmpMessageType(), statisticsReportHandler));
        regs.add(context.registerBmpMessageSerializer(InitiationMessage.class, statisticsReportHandler));

        final RouteMonitoringMessageHandler routeMonitoringMessageHandler = new RouteMonitoringMessageHandler(this.messageRegistry);
        regs.add(context.registerBmpMessageParser(routeMonitoringMessageHandler.getBmpMessageType(), routeMonitoringMessageHandler));
        regs.add(context.registerBmpMessageSerializer(InitiationMessage.class, routeMonitoringMessageHandler));

    }
}
