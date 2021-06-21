/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.parser;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
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
import org.opendaylight.protocol.bmp.spi.registry.BmpExtensionProviderActivator;
import org.opendaylight.protocol.bmp.spi.registry.BmpExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.InitiationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.PeerDownNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.PeerUpNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.RouteMirroringMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.RouteMonitoringMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.StatsReportsMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.TerminationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.description.tlv.DescriptionTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.mirror.information.tlv.MirrorInformationTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.name.tlv.NameTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.reason.tlv.ReasonTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.stat.tlvs.AdjRibsInRoutesTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.stat.tlvs.DuplicatePrefixAdvertisementsTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.stat.tlvs.DuplicateUpdatesTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.stat.tlvs.DuplicateWithdrawsTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.stat.tlvs.InvalidatedAsConfedLoopTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.stat.tlvs.InvalidatedAsPathLoopTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.stat.tlvs.InvalidatedClusterListLoopTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.stat.tlvs.InvalidatedOriginatorIdTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.stat.tlvs.LocRibRoutesTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.stat.tlvs.PerAfiSafiAdjRibInTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.stat.tlvs.PerAfiSafiLocRibTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.stat.tlvs.PrefixesTreatedAsWithdrawTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.stat.tlvs.RejectedPrefixesTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.stat.tlvs.UpdatesTreatedAsWithdrawTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.string.tlv.StringTlv;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, property = "type=org.opendaylight.protocol.bmp.parser.BmpActivator")
@MetaInfServices
@Singleton
public final class BmpActivator implements BmpExtensionProviderActivator {
    private final MessageRegistry messageRegistry;
    private final AddressFamilyRegistry afiRegistry;
    private final SubsequentAddressFamilyRegistry safiRegistry;

    @Inject
    @Activate
    public BmpActivator(final @Reference BGPExtensionConsumerContext context) {
        this.messageRegistry = context.getMessageRegistry();
        this.afiRegistry = context.getAddressFamilyRegistry();
        this.safiRegistry = context.getSubsequentAddressFamilyRegistry();
    }

    @Override
    public List<Registration> start(final BmpExtensionProviderContext context) {
        final DescriptionTlvHandler descriptionTlvHandler = new DescriptionTlvHandler();
        final NameTlvHandler nameTlvHandler = new NameTlvHandler();
        final StringTlvHandler stringTlvHandler = new StringTlvHandler();
        final ReasonTlvHandler reasonTlvHandler = new ReasonTlvHandler();
        final MirrorInformationTlvHandler informationTlvHandler = new MirrorInformationTlvHandler();
        final StatType000TlvHandler statType000TlvHandler = new StatType000TlvHandler();
        final StatType001TlvHandler statType001TlvHandler = new StatType001TlvHandler();
        final StatType002TlvHandler statType002TlvHandler = new StatType002TlvHandler();
        final StatType003TlvHandler statType003TlvHandler = new StatType003TlvHandler();
        final StatType004TlvHandler statType004TlvHandler = new StatType004TlvHandler();
        final StatType005TlvHandler statType005TlvHandler = new StatType005TlvHandler();
        final StatType006TlvHandler statType006TlvHandler = new StatType006TlvHandler();
        final StatType007TlvHandler statType007TlvHandler = new StatType007TlvHandler();
        final StatType008TlvHandler statType008TlvHandler = new StatType008TlvHandler();
        final StatType009TlvHandler statType009TlvHandler =
            new StatType009TlvHandler(this.afiRegistry, this.safiRegistry);
        final StatType010TlvHandler statType010TlvHandler =
            new StatType010TlvHandler(this.afiRegistry, this.safiRegistry);
        final StatType011TlvHandler statType011TlvHandler = new StatType011TlvHandler();
        final StatType012TlvHandler statType012TlvHandler = new StatType012TlvHandler();
        final StatType013TlvHandler statType013TlvHandler = new StatType013TlvHandler();

        final InitiationHandler initiationHandler = new InitiationHandler(context.getBmpInitiationTlvRegistry());
        final TerminationHandler terminationHandler = new TerminationHandler(context.getBmpTerminationTlvRegistry());
        final PeerUpHandler peerUpHandler = new PeerUpHandler(this.messageRegistry, context.getBmpPeerUpTlvRegistry());
        final PeerDownHandler peerDownHandler = new PeerDownHandler(this.messageRegistry);
        final RouteMirroringMessageHandler routeMirroringMessageHandler =
            new RouteMirroringMessageHandler(this.messageRegistry, context.getBmpRouteMirroringTlvRegistry());
        final StatisticsReportHandler statisticsReportHandler = new StatisticsReportHandler(this.messageRegistry,
            context.getBmpStatisticsTlvRegistry());
        final RouteMonitoringMessageHandler routeMonitoringMessageHandler =
            new RouteMonitoringMessageHandler(this.messageRegistry);

        // FIXME: Convert to proper whiteboard pattern
        return List.of(
            context.registerBmpInitiationTlvParser(DescriptionTlvHandler.TYPE, descriptionTlvHandler),
            context.registerBmpInitiationTlvSerializer(DescriptionTlv.class, descriptionTlvHandler),
            context.registerBmpInitiationTlvParser(NameTlvHandler.TYPE, nameTlvHandler),
            context.registerBmpInitiationTlvSerializer(NameTlv.class, nameTlvHandler),

            context.registerBmpInitiationTlvParser(StringTlvHandler.TYPE, stringTlvHandler),
            context.registerBmpInitiationTlvSerializer(StringTlv.class, stringTlvHandler),
            context.registerBmpTerminationTlvParser(StringTlvHandler.TYPE, stringTlvHandler),
            context.registerBmpTerminationTlvSerializer(StringTlv.class, stringTlvHandler),
            context.registerBmpPeerUpTlvParser(StringTlvHandler.TYPE, stringTlvHandler),
            context.registerBmpPeerUpTlvSerializer(StringTlv.class, stringTlvHandler),

            context.registerBmpTerminationTlvParser(ReasonTlvHandler.TYPE, reasonTlvHandler),
            context.registerBmpTerminationTlvSerializer(ReasonTlv.class, reasonTlvHandler),

            context.registerBmpRouteMirroringTlvParser(MirrorInformationTlvHandler.TYPE, informationTlvHandler),
            context.registerBmpRouteMirroringTlvSerializer(MirrorInformationTlv.class, informationTlvHandler),

            context.registerBmpStatisticsTlvParser(StatType000TlvHandler.TYPE, statType000TlvHandler),
            context.registerBmpStatisticsTlvSerializer(RejectedPrefixesTlv.class, statType000TlvHandler),

            context.registerBmpStatisticsTlvParser(StatType001TlvHandler.TYPE, statType001TlvHandler),
            context.registerBmpStatisticsTlvSerializer(DuplicatePrefixAdvertisementsTlv.class, statType001TlvHandler),

            context.registerBmpStatisticsTlvParser(StatType002TlvHandler.TYPE, statType002TlvHandler),
            context.registerBmpStatisticsTlvSerializer(DuplicateWithdrawsTlv.class, statType002TlvHandler),

            context.registerBmpStatisticsTlvParser(StatType003TlvHandler.TYPE, statType003TlvHandler),
            context.registerBmpStatisticsTlvSerializer(InvalidatedClusterListLoopTlv.class, statType003TlvHandler),

            context.registerBmpStatisticsTlvParser(StatType004TlvHandler.TYPE, statType004TlvHandler),
            context.registerBmpStatisticsTlvSerializer(InvalidatedAsPathLoopTlv.class, statType004TlvHandler),

            context.registerBmpStatisticsTlvParser(StatType005TlvHandler.TYPE, statType005TlvHandler),
            context.registerBmpStatisticsTlvSerializer(InvalidatedOriginatorIdTlv.class, statType005TlvHandler),

            context.registerBmpStatisticsTlvParser(StatType006TlvHandler.TYPE, statType006TlvHandler),
            context.registerBmpStatisticsTlvSerializer(InvalidatedAsConfedLoopTlv.class, statType006TlvHandler),

            context.registerBmpStatisticsTlvParser(StatType007TlvHandler.TYPE, statType007TlvHandler),
            context.registerBmpStatisticsTlvSerializer(AdjRibsInRoutesTlv.class, statType007TlvHandler),

            context.registerBmpStatisticsTlvParser(StatType008TlvHandler.TYPE, statType008TlvHandler),
            context.registerBmpStatisticsTlvSerializer(LocRibRoutesTlv.class, statType008TlvHandler),

            context.registerBmpStatisticsTlvParser(StatType009TlvHandler.TYPE, statType009TlvHandler),
            context.registerBmpStatisticsTlvSerializer(PerAfiSafiAdjRibInTlv.class, statType009TlvHandler),

            context.registerBmpStatisticsTlvParser(StatType010TlvHandler.TYPE, statType010TlvHandler),
            context.registerBmpStatisticsTlvSerializer(PerAfiSafiLocRibTlv.class, statType010TlvHandler),

            context.registerBmpStatisticsTlvParser(StatType011TlvHandler.TYPE, statType011TlvHandler),
            context.registerBmpStatisticsTlvSerializer(UpdatesTreatedAsWithdrawTlv.class, statType011TlvHandler),

            context.registerBmpStatisticsTlvParser(StatType012TlvHandler.TYPE, statType012TlvHandler),
            context.registerBmpStatisticsTlvSerializer(PrefixesTreatedAsWithdrawTlv.class, statType012TlvHandler),

            context.registerBmpStatisticsTlvParser(StatType013TlvHandler.TYPE, statType013TlvHandler),
            context.registerBmpStatisticsTlvSerializer(DuplicateUpdatesTlv.class, statType013TlvHandler),


            context.registerBmpMessageParser(initiationHandler.getBmpMessageType(), initiationHandler),
            context.registerBmpMessageSerializer(InitiationMessage.class, initiationHandler),

            context.registerBmpMessageParser(terminationHandler.getBmpMessageType(), terminationHandler),
            context.registerBmpMessageSerializer(TerminationMessage.class, terminationHandler),

            context.registerBmpMessageParser(peerUpHandler.getBmpMessageType(), peerUpHandler),
            context.registerBmpMessageSerializer(PeerUpNotification.class, peerUpHandler),

            context.registerBmpMessageParser(peerDownHandler.getBmpMessageType(), peerDownHandler),
            context.registerBmpMessageSerializer(PeerDownNotification.class, peerDownHandler),

            context.registerBmpMessageParser(statisticsReportHandler.getBmpMessageType(),
                statisticsReportHandler),
            context.registerBmpMessageSerializer(StatsReportsMessage.class, statisticsReportHandler),

            context.registerBmpMessageParser(routeMonitoringMessageHandler.getBmpMessageType(),
                routeMonitoringMessageHandler),
            context.registerBmpMessageSerializer(RouteMonitoringMessage.class, routeMonitoringMessageHandler),

            context.registerBmpMessageParser(routeMirroringMessageHandler.getBmpMessageType(),
                routeMirroringMessageHandler),
            context.registerBmpMessageSerializer(RouteMirroringMessage.class, routeMirroringMessageHandler)
            );
    }
}
