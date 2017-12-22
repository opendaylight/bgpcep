/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.parser;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionConsumerContext;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.protocol.bmp.parser.message.InitiationHandler;
import org.opendaylight.protocol.bmp.parser.message.PeerDownHandler;
import org.opendaylight.protocol.bmp.parser.message.PeerUpHandler;
import org.opendaylight.protocol.bmp.parser.message.RouteMirroringMessageHandler;
import org.opendaylight.protocol.bmp.parser.message.RouteMonitoringMessageHandler;
import org.opendaylight.protocol.bmp.parser.message.StatisticsReportHandler;
import org.opendaylight.protocol.bmp.parser.message.TerminationHandler;
import org.opendaylight.protocol.bmp.parser.tlv.DescriptionTlvHandler;
import org.opendaylight.protocol.bmp.parser.tlv.MirrorInformationTlvHandler;
import org.opendaylight.protocol.bmp.parser.tlv.NameTlvHandler;
import org.opendaylight.protocol.bmp.parser.tlv.ReasonTlvHandler;
import org.opendaylight.protocol.bmp.parser.tlv.StatType000TlvHandler;
import org.opendaylight.protocol.bmp.parser.tlv.StatType001TlvHandler;
import org.opendaylight.protocol.bmp.parser.tlv.StatType002TlvHandler;
import org.opendaylight.protocol.bmp.parser.tlv.StatType003TlvHandler;
import org.opendaylight.protocol.bmp.parser.tlv.StatType004TlvHandler;
import org.opendaylight.protocol.bmp.parser.tlv.StatType005TlvHandler;
import org.opendaylight.protocol.bmp.parser.tlv.StatType006TlvHandler;
import org.opendaylight.protocol.bmp.parser.tlv.StatType007TlvHandler;
import org.opendaylight.protocol.bmp.parser.tlv.StatType008TlvHandler;
import org.opendaylight.protocol.bmp.parser.tlv.StatType009TlvHandler;
import org.opendaylight.protocol.bmp.parser.tlv.StatType010TlvHandler;
import org.opendaylight.protocol.bmp.parser.tlv.StatType011TlvHandler;
import org.opendaylight.protocol.bmp.parser.tlv.StatType012TlvHandler;
import org.opendaylight.protocol.bmp.parser.tlv.StatType013TlvHandler;
import org.opendaylight.protocol.bmp.parser.tlv.StringTlvHandler;
import org.opendaylight.protocol.bmp.spi.registry.AbstractBmpExtensionProviderActivator;
import org.opendaylight.protocol.bmp.spi.registry.BmpExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.InitiationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.PeerDownNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.PeerUpNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.RouteMirroringMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.RouteMonitoringMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.StatsReportsMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.TerminationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.description.tlv.DescriptionTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.mirror.information.tlv.MirrorInformationTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.name.tlv.NameTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.reason.tlv.ReasonTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.AdjRibsInRoutesTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.DuplicatePrefixAdvertisementsTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.DuplicateUpdatesTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.DuplicateWithdrawsTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.InvalidatedAsConfedLoopTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.InvalidatedAsPathLoopTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.InvalidatedClusterListLoopTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.InvalidatedOriginatorIdTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.LocRibRoutesTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.PerAfiSafiAdjRibInTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.PerAfiSafiLocRibTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.PrefixesTreatedAsWithdrawTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.RejectedPrefixesTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.UpdatesTreatedAsWithdrawTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.string.tlv.StringTlv;



/**
 * Created by cgasparini on 15.5.2015.
 */
public final class BmpActivator extends AbstractBmpExtensionProviderActivator {

    private final MessageRegistry messageRegistry;
    private final AddressFamilyRegistry afiRegistry;
    private final SubsequentAddressFamilyRegistry safiRegistry;

    public BmpActivator(final BGPExtensionConsumerContext context) {
        this.messageRegistry = context.getMessageRegistry();
        this.afiRegistry = context.getAddressFamilyRegistry();
        this.safiRegistry = context.getSubsequentAddressFamilyRegistry();
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

        final PeerUpHandler peerUpHandler = new PeerUpHandler(this.messageRegistry, context.getBmpPeerUpTlvRegistry());
        regs.add(context.registerBmpMessageParser(peerUpHandler.getBmpMessageType(), peerUpHandler));
        regs.add(context.registerBmpMessageSerializer(PeerUpNotification.class, peerUpHandler));

        final PeerDownHandler peerDownHandler = new PeerDownHandler(this.messageRegistry);
        regs.add(context.registerBmpMessageParser(peerDownHandler.getBmpMessageType(), peerDownHandler));
        regs.add(context.registerBmpMessageSerializer(PeerDownNotification.class, peerDownHandler));

        final StatisticsReportHandler statisticsReportHandler = new StatisticsReportHandler(this.messageRegistry,
                context.getBmpStatisticsTlvRegistry());
        regs.add(context.registerBmpMessageParser(statisticsReportHandler.getBmpMessageType(),
                statisticsReportHandler));
        regs.add(context.registerBmpMessageSerializer(StatsReportsMessage.class, statisticsReportHandler));

        final RouteMonitoringMessageHandler routeMonitoringMessageHandler =
                new RouteMonitoringMessageHandler(this.messageRegistry);
        regs.add(context.registerBmpMessageParser(routeMonitoringMessageHandler.getBmpMessageType(),
                routeMonitoringMessageHandler));
        regs.add(context.registerBmpMessageSerializer(RouteMonitoringMessage.class, routeMonitoringMessageHandler));

        final RouteMirroringMessageHandler routeMirroringMessageHandler =
                new RouteMirroringMessageHandler(this.messageRegistry, context.getBmpRouteMirroringTlvRegistry());
        regs.add(context.registerBmpMessageParser(routeMirroringMessageHandler.getBmpMessageType(),
                routeMirroringMessageHandler));
        regs.add(context.registerBmpMessageSerializer(RouteMirroringMessage.class, routeMirroringMessageHandler));

    }

    private static void registerBmpTlvHandlers(final List<AutoCloseable> regs,
            final BmpExtensionProviderContext context) {
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
        regs.add(context.registerBmpPeerUpTlvParser(StringTlvHandler.TYPE, stringTlvHandler));
        regs.add(context.registerBmpPeerUpTlvSerializer(StringTlv.class, stringTlvHandler));

        final ReasonTlvHandler reasonTlvHandler = new ReasonTlvHandler();
        regs.add(context.registerBmpTerminationTlvParser(ReasonTlvHandler.TYPE, reasonTlvHandler));
        regs.add(context.registerBmpTerminationTlvSerializer(ReasonTlv.class, reasonTlvHandler));

        final MirrorInformationTlvHandler informationTlvHandler = new MirrorInformationTlvHandler();
        regs.add(context.registerBmpRouteMirroringTlvParser(MirrorInformationTlvHandler.TYPE, informationTlvHandler));
        regs.add(context.registerBmpRouteMirroringTlvSerializer(MirrorInformationTlv.class, informationTlvHandler));
    }

    private void registerBmpStatTlvHandlers(final List<AutoCloseable> regs,
            final BmpExtensionProviderContext context) {
        final StatType000TlvHandler statType000TlvHandler = new StatType000TlvHandler();
        regs.add(context.registerBmpStatisticsTlvParser(StatType000TlvHandler.TYPE, statType000TlvHandler));
        regs.add(context.registerBmpStatisticsTlvSerializer(RejectedPrefixesTlv.class, statType000TlvHandler));

        final StatType001TlvHandler statType001TlvHandler = new StatType001TlvHandler();
        regs.add(context.registerBmpStatisticsTlvParser(StatType001TlvHandler.TYPE, statType001TlvHandler));
        regs.add(context.registerBmpStatisticsTlvSerializer(DuplicatePrefixAdvertisementsTlv.class,
                statType001TlvHandler));

        final StatType002TlvHandler statType002TlvHandler = new StatType002TlvHandler();
        regs.add(context.registerBmpStatisticsTlvParser(StatType002TlvHandler.TYPE, statType002TlvHandler));
        regs.add(context.registerBmpStatisticsTlvSerializer(DuplicateWithdrawsTlv.class, statType002TlvHandler));

        final StatType003TlvHandler statType003TlvHandler = new StatType003TlvHandler();
        regs.add(context.registerBmpStatisticsTlvParser(StatType003TlvHandler.TYPE, statType003TlvHandler));
        regs.add(context.registerBmpStatisticsTlvSerializer(InvalidatedClusterListLoopTlv.class,
                statType003TlvHandler));

        final StatType004TlvHandler statType004TlvHandler = new StatType004TlvHandler();
        regs.add(context.registerBmpStatisticsTlvParser(StatType004TlvHandler.TYPE, statType004TlvHandler));
        regs.add(context.registerBmpStatisticsTlvSerializer(InvalidatedAsPathLoopTlv.class, statType004TlvHandler));

        final StatType005TlvHandler statType005TlvHandler = new StatType005TlvHandler();
        regs.add(context.registerBmpStatisticsTlvParser(StatType005TlvHandler.TYPE, statType005TlvHandler));
        regs.add(context.registerBmpStatisticsTlvSerializer(InvalidatedOriginatorIdTlv.class, statType005TlvHandler));

        final StatType006TlvHandler statType006TlvHandler = new StatType006TlvHandler();
        regs.add(context.registerBmpStatisticsTlvParser(StatType006TlvHandler.TYPE, statType006TlvHandler));
        regs.add(context.registerBmpStatisticsTlvSerializer(InvalidatedAsConfedLoopTlv.class, statType006TlvHandler));

        final StatType007TlvHandler statType007TlvHandler = new StatType007TlvHandler();
        regs.add(context.registerBmpStatisticsTlvParser(StatType007TlvHandler.TYPE, statType007TlvHandler));
        regs.add(context.registerBmpStatisticsTlvSerializer(AdjRibsInRoutesTlv.class, statType007TlvHandler));

        final StatType008TlvHandler statType008TlvHandler = new StatType008TlvHandler();
        regs.add(context.registerBmpStatisticsTlvParser(StatType008TlvHandler.TYPE, statType008TlvHandler));
        regs.add(context.registerBmpStatisticsTlvSerializer(LocRibRoutesTlv.class, statType008TlvHandler));

        final StatType009TlvHandler statType009TlvHandler =
                new StatType009TlvHandler(this.afiRegistry, this.safiRegistry);
        regs.add(context.registerBmpStatisticsTlvParser(StatType009TlvHandler.TYPE, statType009TlvHandler));
        regs.add(context.registerBmpStatisticsTlvSerializer(PerAfiSafiAdjRibInTlv.class, statType009TlvHandler));

        final StatType010TlvHandler statType010TlvHandler =
                new StatType010TlvHandler(this.afiRegistry, this.safiRegistry);
        regs.add(context.registerBmpStatisticsTlvParser(StatType010TlvHandler.TYPE, statType010TlvHandler));
        regs.add(context.registerBmpStatisticsTlvSerializer(PerAfiSafiLocRibTlv.class, statType010TlvHandler));

        final StatType011TlvHandler statType011TlvHandler = new StatType011TlvHandler();
        regs.add(context.registerBmpStatisticsTlvParser(StatType011TlvHandler.TYPE, statType011TlvHandler));
        regs.add(context.registerBmpStatisticsTlvSerializer(UpdatesTreatedAsWithdrawTlv.class, statType011TlvHandler));

        final StatType012TlvHandler statType012TlvHandler = new StatType012TlvHandler();
        regs.add(context.registerBmpStatisticsTlvParser(StatType012TlvHandler.TYPE, statType012TlvHandler));
        regs.add(context.registerBmpStatisticsTlvSerializer(PrefixesTreatedAsWithdrawTlv.class, statType012TlvHandler));

        final StatType013TlvHandler statType013TlvHandler = new StatType013TlvHandler();
        regs.add(context.registerBmpStatisticsTlvParser(StatType013TlvHandler.TYPE, statType013TlvHandler));
        regs.add(context.registerBmpStatisticsTlvSerializer(DuplicateUpdatesTlv.class, statType013TlvHandler));

    }
}
