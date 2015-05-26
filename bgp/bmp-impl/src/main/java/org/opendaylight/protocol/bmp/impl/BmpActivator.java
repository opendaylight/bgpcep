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
import org.opendaylight.protocol.bmp.impl.tlv.DescriptionTlvHandler;
import org.opendaylight.protocol.bmp.impl.tlv.NameTlvHandler;
import org.opendaylight.protocol.bmp.impl.tlv.ReasonTlvHandler;
import org.opendaylight.protocol.bmp.impl.tlv.StatType0TlvHandler;
import org.opendaylight.protocol.bmp.impl.tlv.StatType1TlvHandler;
import org.opendaylight.protocol.bmp.impl.tlv.StatType2TlvHandler;
import org.opendaylight.protocol.bmp.impl.tlv.StatType3TlvHandler;
import org.opendaylight.protocol.bmp.impl.tlv.StatType4TlvHandler;
import org.opendaylight.protocol.bmp.impl.tlv.StatType5TlvHandler;
import org.opendaylight.protocol.bmp.impl.tlv.StatType6TlvHandler;
import org.opendaylight.protocol.bmp.impl.tlv.StatType7TlvHandler;
import org.opendaylight.protocol.bmp.impl.tlv.StatType8TlvHandler;
import org.opendaylight.protocol.bmp.impl.tlv.StringTlvHandler;
import org.opendaylight.protocol.bmp.spi.registry.AbstractBmpExtensionProviderActivator;
import org.opendaylight.protocol.bmp.spi.registry.BmpExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.InitiationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerDownNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerUpNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.RouteMonitoringMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.StatsReportsMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.TerminationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.description.tlv.DescriptionTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.name.tlv.NameTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.reason.tlv.ReasonTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.stat.tlvs.AdjRibsInRoutesTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.stat.tlvs.DuplicatePrefixAdvertisementsTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.stat.tlvs.DuplicateWithdrawsTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.stat.tlvs.InvalidatedAsConfedLoopTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.stat.tlvs.InvalidatedAsPathLoopTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.stat.tlvs.InvalidatedClusterListLoopTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.stat.tlvs.InvalidatedOriginatorIdTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.stat.tlvs.LocRibRoutesTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.stat.tlvs.RejectedPrefixesTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.string.tlv.StringTlv;

/**
 * Created by cgasparini on 15.5.2015.
 */
public final class BmpActivator extends AbstractBmpExtensionProviderActivator {

    private final MessageRegistry messageRegistry;

    public BmpActivator(final MessageRegistry messageRegistry) {
        this.messageRegistry = messageRegistry;
    }

    @Override
    protected List<AutoCloseable> startImpl(final BmpExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>();
        registerBmpTlvHandlers(regs, context);
        registerBmpStatTlvHandlers(regs, context);
        registerBmpMessageHandlers(regs, context);
        return regs;
    }

    private void registerBmpMessageHandlers(final List<AutoCloseable> regs, final BmpExtensionProviderContext context) {
        final InitiationHandler initiationHandler = new InitiationHandler(context.getBmpInitiationTlvRegistry());
        regs.add(context.registerBmpMessageParser(initiationHandler.getBmpMessageType(), initiationHandler));
        regs.add(context.registerBmpMessageSerializer(InitiationMessage.class, initiationHandler));

        final TerminationHandler terminationHandler = new TerminationHandler(context.getBmpTerminationTlvRegistry());
        regs.add(context.registerBmpMessageParser(terminationHandler.getBmpMessageType(), terminationHandler));
        regs.add(context.registerBmpMessageSerializer(TerminationMessage.class, terminationHandler));

        final PeerUpHandler peerUpHandler = new PeerUpHandler(this.messageRegistry);
        regs.add(context.registerBmpMessageParser(peerUpHandler.getBmpMessageType(), peerUpHandler));
        regs.add(context.registerBmpMessageSerializer(PeerUpNotification.class, peerUpHandler));

        final PeerDownHandler peerDownHandler = new PeerDownHandler(this.messageRegistry);
        regs.add(context.registerBmpMessageParser(peerDownHandler.getBmpMessageType(), peerDownHandler));
        regs.add(context.registerBmpMessageSerializer(PeerDownNotification.class, peerDownHandler));

        final StatisticsReportHandler statisticsReportHandler = new StatisticsReportHandler(this.messageRegistry,
                context.getBmpStatisticsTlvRegistry());
        regs.add(context.registerBmpMessageParser(statisticsReportHandler.getBmpMessageType(), statisticsReportHandler));
        regs.add(context.registerBmpMessageSerializer(StatsReportsMessage.class, statisticsReportHandler));

        final RouteMonitoringMessageHandler routeMonitoringMessageHandler = new RouteMonitoringMessageHandler(this.messageRegistry);
        regs.add(context.registerBmpMessageParser(routeMonitoringMessageHandler.getBmpMessageType(), routeMonitoringMessageHandler));
        regs.add(context.registerBmpMessageSerializer(RouteMonitoringMessage.class, routeMonitoringMessageHandler));

    }

    private void registerBmpTlvHandlers(final List<AutoCloseable> regs, final BmpExtensionProviderContext context) {
        final DescriptionTlvHandler descriptionTlvHandler = new DescriptionTlvHandler();
        regs.add(context.registerBmpInitiationTlvParser(DescriptionTlvHandler.TYPE, descriptionTlvHandler));
        regs.add(context.registerBmpInitiationTlvSerializer(DescriptionTlv.class, descriptionTlvHandler));

        final NameTlvHandler nameTlvHandler = new NameTlvHandler();
        regs.add(context.registerBmpInitiationTlvParser(NameTlvHandler.TYPE, nameTlvHandler));
        regs.add(context.registerBmpInitiationTlvSerializer(NameTlv.class, nameTlvHandler));

        final StringTlvHandler stringTlvHandler = new StringTlvHandler();
        regs.add(context.registerBmpInitiationTlvParser(StringTlvHandler.TYPE, stringTlvHandler));
        regs.add(context.registerBmpInitiationTlvSerializer(StringTlv.class, stringTlvHandler));
        regs.add(context.registerBmpTerminationTlvParser(StringTlvHandler.TYPE, stringTlvHandler));
        regs.add(context.registerBmpTerminationTlvSerializer(StringTlv.class, stringTlvHandler));

        final ReasonTlvHandler reasonTlvHandler = new ReasonTlvHandler();
        regs.add(context.registerBmpTerminationTlvParser(ReasonTlvHandler.TYPE, reasonTlvHandler));
        regs.add(context.registerBmpTerminationTlvSerializer(ReasonTlv.class, reasonTlvHandler));
    }

    private void registerBmpStatTlvHandlers(final List<AutoCloseable> regs, final BmpExtensionProviderContext context) {
        final StatType0TlvHandler statType0TlvHandler = new StatType0TlvHandler();
        regs.add(context.registerBmpStatisticsTlvParser(StatType0TlvHandler.TYPE, statType0TlvHandler));
        regs.add(context.registerBmpStatisticsTlvSerializer(RejectedPrefixesTlv.class, statType0TlvHandler));

        final StatType1TlvHandler statType1TlvHandler = new StatType1TlvHandler();
        regs.add(context.registerBmpStatisticsTlvParser(StatType1TlvHandler.TYPE, statType1TlvHandler));
        regs.add(context.registerBmpStatisticsTlvSerializer(DuplicatePrefixAdvertisementsTlv.class, statType1TlvHandler));

        final StatType2TlvHandler statType2TlvHandler = new StatType2TlvHandler();
        regs.add(context.registerBmpStatisticsTlvParser(StatType2TlvHandler.TYPE, statType2TlvHandler));
        regs.add(context.registerBmpStatisticsTlvSerializer(DuplicateWithdrawsTlv.class, statType2TlvHandler));

        final StatType3TlvHandler statType3TlvHandler = new StatType3TlvHandler();
        regs.add(context.registerBmpStatisticsTlvParser(StatType3TlvHandler.TYPE, statType3TlvHandler));
        regs.add(context.registerBmpStatisticsTlvSerializer(InvalidatedClusterListLoopTlv.class, statType3TlvHandler));

        final StatType4TlvHandler statType4TlvHandler = new StatType4TlvHandler();
        regs.add(context.registerBmpStatisticsTlvParser(StatType4TlvHandler.TYPE, statType4TlvHandler));
        regs.add(context.registerBmpStatisticsTlvSerializer(InvalidatedAsPathLoopTlv.class, statType4TlvHandler));

        final StatType5TlvHandler statType5TlvHandler = new StatType5TlvHandler();
        regs.add(context.registerBmpStatisticsTlvParser(StatType5TlvHandler.TYPE, statType5TlvHandler));
        regs.add(context.registerBmpStatisticsTlvSerializer(InvalidatedOriginatorIdTlv.class, statType5TlvHandler));

        final StatType6TlvHandler statType6TlvHandler = new StatType6TlvHandler();
        regs.add(context.registerBmpStatisticsTlvParser(StatType6TlvHandler.TYPE, statType6TlvHandler));
        regs.add(context.registerBmpStatisticsTlvSerializer(InvalidatedAsConfedLoopTlv.class, statType6TlvHandler));

        final StatType7TlvHandler statType7TlvHandler = new StatType7TlvHandler();
        regs.add(context.registerBmpStatisticsTlvParser(StatType7TlvHandler.TYPE, statType7TlvHandler));
        regs.add(context.registerBmpStatisticsTlvSerializer(AdjRibsInRoutesTlv.class, statType7TlvHandler));

        final StatType8TlvHandler statType8TlvHandler = new StatType8TlvHandler();
        regs.add(context.registerBmpStatisticsTlvParser(StatType8TlvHandler.TYPE, statType8TlvHandler));
        regs.add(context.registerBmpStatisticsTlvSerializer(LocRibRoutesTlv.class, statType8TlvHandler));
    }
}
