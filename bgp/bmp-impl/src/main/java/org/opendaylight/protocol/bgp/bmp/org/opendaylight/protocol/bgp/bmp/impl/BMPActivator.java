package org.opendaylight.protocol.bgp.bmp.org.opendaylight.protocol.bgp.bmp.impl;/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.protocol.bgp.bmp.org.opendaylight.protocol.bgp.bmp.impl.message.InitiationHandler;
import org.opendaylight.protocol.bgp.bmp.org.opendaylight.protocol.bgp.bmp.impl.message.PeerDownHandler;
import org.opendaylight.protocol.bgp.bmp.org.opendaylight.protocol.bgp.bmp.impl.message.PeerUpHandler;
import org.opendaylight.protocol.bgp.bmp.org.opendaylight.protocol.bgp.bmp.impl.message.RouteMonitoringMessageHandler;
import org.opendaylight.protocol.bgp.bmp.org.opendaylight.protocol.bgp.bmp.impl.message.StatisticsReportHandler;
import org.opendaylight.protocol.bgp.bmp.org.opendaylight.protocol.bgp.bmp.impl.message.TerminationHandler;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bmp.spi.registry.AbstractBMPExtensionProviderActivator;
import org.opendaylight.protocol.bmp.spi.registry.BmpExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.InitiationMessage;

/**
 * Created by cgasparini on 15.5.2015.
 */
public final class BMPActivator extends AbstractBMPExtensionProviderActivator {
    enum BMP_Message_Type {
        RouteMonitor(0), StatisticRepor(1), PeerDownNotification(2), PeerUpNotification(3),
        Initiation(4), Termination(5);
        public final int value;

        BMP_Message_Type(final int value) {
            this.value = value;
        }
    }

    @Override
    protected List<AutoCloseable> startImpl(final BmpExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>();
        this.registerBGPParsers(regs, context);
        return regs;
    }

    private void registerBGPParsers(final List<AutoCloseable> regs, final BmpExtensionProviderContext context) {
        final MessageRegistry messageRegistry = null;//TODO

        final InitiationHandler initiationHandler = new InitiationHandler();
        regs.add(context.registerBmpMessageParser(BMP_Message_Type.Initiation.ordinal(), initiationHandler));
        regs.add(context.registerBmpMessageSerializer(InitiationMessage.class, initiationHandler));

        final TerminationHandler terminationHandler = new TerminationHandler();
        regs.add(context.registerBmpMessageParser(BMP_Message_Type.Termination.ordinal(), terminationHandler));
        regs.add(context.registerBmpMessageSerializer(InitiationMessage.class, terminationHandler));

        final PeerUpHandler peerUpHandler = new PeerUpHandler(messageRegistry);
        regs.add(context.registerBmpMessageParser(BMP_Message_Type.PeerUpNotification.ordinal(), peerUpHandler));
        regs.add(context.registerBmpMessageSerializer(InitiationMessage.class, peerUpHandler));

        final PeerDownHandler peerDownHandler = new PeerDownHandler(messageRegistry);
        regs.add(context.registerBmpMessageParser(BMP_Message_Type.PeerDownNotification.ordinal(), peerDownHandler));
        regs.add(context.registerBmpMessageSerializer(InitiationMessage.class, peerDownHandler));

        final StatisticsReportHandler statisticsReportHandler = new StatisticsReportHandler(messageRegistry);
        regs.add(context.registerBmpMessageParser(BMP_Message_Type.StatisticRepor.ordinal(), statisticsReportHandler));
        regs.add(context.registerBmpMessageSerializer(InitiationMessage.class, statisticsReportHandler));

        final RouteMonitoringMessageHandler routeMonitoringMessageHandler = new RouteMonitoringMessageHandler(messageRegistry);
        regs.add(context.registerBmpMessageParser(BMP_Message_Type.RouteMonitor.ordinal(), routeMonitoringMessageHandler));
        regs.add(context.registerBmpMessageSerializer(InitiationMessage.class, routeMonitoringMessageHandler));

    }
}
