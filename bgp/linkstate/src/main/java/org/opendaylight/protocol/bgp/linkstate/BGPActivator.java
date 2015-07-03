/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.linkstate.attribute.LinkstateAttributeParser;
import org.opendaylight.protocol.bgp.linkstate.attribute.objects.BGPAdminStatusObjectParser;
import org.opendaylight.protocol.bgp.linkstate.attribute.objects.BGPAssociationObjectParserType1;
import org.opendaylight.protocol.bgp.linkstate.attribute.objects.BGPAssociationObjectParserType2;
import org.opendaylight.protocol.bgp.linkstate.attribute.objects.BGPBandwidthObjectType1Parser;
import org.opendaylight.protocol.bgp.linkstate.attribute.objects.BGPBandwidthObjectType2Parser;
import org.opendaylight.protocol.bgp.linkstate.attribute.objects.BGPDetourObjectType7Parser;
import org.opendaylight.protocol.bgp.linkstate.attribute.objects.BGPDetourObjectType8Parser;
import org.opendaylight.protocol.bgp.linkstate.attribute.objects.BGPExcludeRouteObjectParser;
import org.opendaylight.protocol.bgp.linkstate.attribute.objects.BGPExplicitRouteObjectParser;
import org.opendaylight.protocol.bgp.linkstate.attribute.objects.BGPFastRerouteObjectType1Parser;
import org.opendaylight.protocol.bgp.linkstate.attribute.objects.BGPFastRerouteObjectType7Parser;
import org.opendaylight.protocol.bgp.linkstate.attribute.objects.BGPFlowSpecObjectParser;
import org.opendaylight.protocol.bgp.linkstate.attribute.objects.BGPLspAttributesObjectParser;
import org.opendaylight.protocol.bgp.linkstate.attribute.objects.BGPLspRequiredAttributesObjectParser;
import org.opendaylight.protocol.bgp.linkstate.attribute.objects.BGPMetricObjectParser;
import org.opendaylight.protocol.bgp.linkstate.attribute.objects.BGPPrimaryPathRouteObject;
import org.opendaylight.protocol.bgp.linkstate.attribute.objects.BGPProtectionObjectType1Parser;
import org.opendaylight.protocol.bgp.linkstate.attribute.objects.BGPProtectionObjectType2Parser;
import org.opendaylight.protocol.bgp.linkstate.attribute.objects.BGPRecordRouteObjectParser;
import org.opendaylight.protocol.bgp.linkstate.attribute.objects.BGPSecondaryExplicitRouteObjectParser;
import org.opendaylight.protocol.bgp.linkstate.attribute.objects.BGPSecondaryRecordRouteObjectParser;
import org.opendaylight.protocol.bgp.linkstate.attribute.objects.BGPSenderTspecObjectParser;
import org.opendaylight.protocol.bgp.linkstate.attribute.objects.BGPSessionAttributeObjectType1Parser;
import org.opendaylight.protocol.bgp.linkstate.attribute.objects.BGPSessionAttributeObjectType7Parser;
import org.opendaylight.protocol.bgp.linkstate.nlri.LinkstateNlriParser;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.routes.LinkstateRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.admin.status.object.AdminStatusObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.association.object.AssociationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.bandwidth.object.BandwidthObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.detour.object.DetourObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.exclude.route.object.ExcludeRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.explicit.route.object.ExplicitRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.flow.spec.object.FlowSpecObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.lsp.attributes.object.LspAttributesObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.lsp.required.attributes.object.LspRequiredAttributesObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.metric.object.MetricObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.primary.path.route.object.PrimaryPathRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.protection.object.ProtectionObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.record.route.object.RecordRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.secondary.explicit.route.object.SecondaryExplicitRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.secondary.record.route.object.SecondaryRecordRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.sender.tspec.object.SenderTspecObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.session.attribute.object.SessionAttributeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;

/**
 * Activator for registering linkstate extensions to BGP parser.
 */
public final class BGPActivator extends AbstractBGPExtensionProviderActivator {

    private static final int LINKSTATE_AFI = 16388;

    private static final int LINKSTATE_SAFI = 71;

    private final boolean ianaLinkstateAttributeType;

    public BGPActivator() {
        super();
        this.ianaLinkstateAttributeType = true;
    }

    public BGPActivator(final boolean ianaLinkstateAttributeType) {
        super();
        this.ianaLinkstateAttributeType = ianaLinkstateAttributeType;
    }

    @Override
    protected List<AutoCloseable> startImpl(final BGPExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>();

        regs.add(context.registerAddressFamily(LinkstateAddressFamily.class, LINKSTATE_AFI));
        regs.add(context.registerSubsequentAddressFamily(LinkstateSubsequentAddressFamily.class, LINKSTATE_SAFI));

        regs.add(context.registerNlriParser(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class,
            new LinkstateNlriParser(false)));
        regs.add(context.registerNlriParser(LinkstateAddressFamily.class, MplsLabeledVpnSubsequentAddressFamily.class,
            new LinkstateNlriParser(true)));
        regs.add(context.registerNlriSerializer(LinkstateRoutes.class, new LinkstateNlriParser(false)));

        regs.add(context.registerAttributeSerializer(Attributes1.class, new LinkstateAttributeParser(this.ianaLinkstateAttributeType, context.getTeLspRegistry())));
        final LinkstateAttributeParser linkstateAttributeParser = new LinkstateAttributeParser(this.ianaLinkstateAttributeType, context.getTeLspRegistry());
        regs.add(context.registerAttributeParser(linkstateAttributeParser.getType(), linkstateAttributeParser));

        this.registerTeLspObject(context);
        return regs;
    }

    private void registerTeLspObject(final BGPExtensionProviderContext context) {
        final BGPDetourObjectType7Parser detourC7 = new BGPDetourObjectType7Parser();
        context.registerTeLspObjectParser(BGPDetourObjectType7Parser.CLASS_NUM, BGPDetourObjectType7Parser.CTYPE, detourC7);
        context.registerTeLspObjectSerializer(DetourObject.class, BGPDetourObjectType7Parser.CTYPE, detourC7);

        final BGPDetourObjectType8Parser detourC8 = new BGPDetourObjectType8Parser();
        context.registerTeLspObjectParser(BGPDetourObjectType8Parser.CLASS_NUM, BGPDetourObjectType8Parser.CTYPE, detourC8);
        context.registerTeLspObjectSerializer(DetourObject.class, BGPDetourObjectType8Parser.CTYPE, detourC8);

        final BGPFastRerouteObjectType1Parser fastC1 = new BGPFastRerouteObjectType1Parser();
        context.registerTeLspObjectParser(BGPFastRerouteObjectType1Parser.CLASS_NUM, BGPFastRerouteObjectType1Parser.CTYPE, fastC1);
        context.registerTeLspObjectSerializer(DetourObject.class, BGPFastRerouteObjectType1Parser.CTYPE, fastC1);

        final BGPFastRerouteObjectType7Parser fastC7 = new BGPFastRerouteObjectType7Parser();
        context.registerTeLspObjectParser(BGPFastRerouteObjectType7Parser.CLASS_NUM, BGPFastRerouteObjectType7Parser.CTYPE, fastC7);
        context.registerTeLspObjectSerializer(DetourObject.class, BGPFastRerouteObjectType7Parser.CTYPE, fastC7);

        final BGPSenderTspecObjectParser tSpec = new BGPSenderTspecObjectParser();
        context.registerTeLspObjectParser(BGPSenderTspecObjectParser.CLASS_NUM, BGPSenderTspecObjectParser.CTYPE, tSpec);
        context.registerTeLspObjectSerializer(SenderTspecObject.class, BGPSenderTspecObjectParser.CTYPE, tSpec);

        final BGPFlowSpecObjectParser fSpec = new BGPFlowSpecObjectParser();
        context.registerTeLspObjectParser(BGPFlowSpecObjectParser.CLASS_NUM, BGPFlowSpecObjectParser.CTYPE, fSpec);
        context.registerTeLspObjectSerializer(FlowSpecObject.class, BGPFlowSpecObjectParser.CTYPE, fSpec);

        final BGPSessionAttributeObjectType7Parser sAttributeC7 = new BGPSessionAttributeObjectType7Parser();
        context.registerTeLspObjectParser(BGPSessionAttributeObjectType7Parser.CLASS_NUM, BGPSessionAttributeObjectType7Parser.CTYPE, sAttributeC7);
        context.registerTeLspObjectSerializer(SessionAttributeObject.class, BGPSessionAttributeObjectType7Parser.CTYPE, sAttributeC7);

        final BGPSessionAttributeObjectType1Parser sAttributeC1 = new BGPSessionAttributeObjectType1Parser();
        context.registerTeLspObjectParser(BGPSessionAttributeObjectType1Parser.CLASS_NUM, BGPSessionAttributeObjectType1Parser.CTYPE, sAttributeC1);
        context.registerTeLspObjectSerializer(SessionAttributeObject.class, BGPSessionAttributeObjectType1Parser.CTYPE, sAttributeC1);

        final BGPRecordRouteObjectParser rroParser = new BGPRecordRouteObjectParser();
        context.registerTeLspObjectParser(BGPRecordRouteObjectParser.CLASS_NUM, BGPRecordRouteObjectParser.CTYPE, rroParser);
        context.registerTeLspObjectSerializer(RecordRouteObject.class, BGPRecordRouteObjectParser.CTYPE, rroParser);

        final BGPExcludeRouteObjectParser xroParser = new BGPExcludeRouteObjectParser();
        context.registerTeLspObjectParser(BGPExcludeRouteObjectParser.CLASS_NUM, BGPExcludeRouteObjectParser.CTYPE, xroParser);
        context.registerTeLspObjectSerializer(ExcludeRouteObject.class, BGPExcludeRouteObjectParser.CTYPE, xroParser);

        final BGPExplicitRouteObjectParser eroParser = new BGPExplicitRouteObjectParser();
        context.registerTeLspObjectParser(BGPExplicitRouteObjectParser.CLASS_NUM, BGPExplicitRouteObjectParser.CTYPE, eroParser);
        context.registerTeLspObjectSerializer(ExplicitRouteObject.class, BGPExplicitRouteObjectParser.CTYPE, eroParser);

        final BGPAssociationObjectParserType1 ipv4AsoParser = new BGPAssociationObjectParserType1();
        context.registerTeLspObjectParser(BGPAssociationObjectParserType1.CLASS_NUM, BGPAssociationObjectParserType1.CTYPE, ipv4AsoParser);
        context.registerTeLspObjectSerializer(AssociationObject.class, BGPAssociationObjectParserType1.CTYPE, ipv4AsoParser);

        final BGPAssociationObjectParserType2 ipv6AsoParser = new BGPAssociationObjectParserType2();
        context.registerTeLspObjectParser(BGPAssociationObjectParserType2.CLASS_NUM, BGPAssociationObjectParserType2.CTYPE, ipv6AsoParser);
        context.registerTeLspObjectSerializer(AssociationObject.class, BGPAssociationObjectParserType2.CTYPE, ipv6AsoParser);

        final BGPAdminStatusObjectParser admParser = new BGPAdminStatusObjectParser();
        context.registerTeLspObjectParser(BGPAdminStatusObjectParser.CLASS_NUM, BGPAdminStatusObjectParser.CTYPE, admParser);
        context.registerTeLspObjectSerializer(AdminStatusObject.class, BGPAdminStatusObjectParser.CTYPE, admParser);

        final BGPBandwidthObjectType1Parser bandT1Parser = new BGPBandwidthObjectType1Parser();
        context.registerTeLspObjectParser(BGPBandwidthObjectType1Parser.CLASS_NUM, BGPBandwidthObjectType1Parser.CTYPE, bandT1Parser);
        context.registerTeLspObjectSerializer(BandwidthObject.class, BGPBandwidthObjectType1Parser.CTYPE, bandT1Parser);

        final BGPBandwidthObjectType1Parser bandT2Parser = new BGPBandwidthObjectType1Parser();
        context.registerTeLspObjectParser(BGPBandwidthObjectType1Parser.CLASS_NUM, BGPBandwidthObjectType2Parser.CTYPE, bandT2Parser);
        context.registerTeLspObjectSerializer(BandwidthObject.class, BGPBandwidthObjectType2Parser.CTYPE, bandT2Parser);

        final BGPMetricObjectParser metricParser = new BGPMetricObjectParser();
        context.registerTeLspObjectParser(BGPMetricObjectParser.CLASS_NUM, BGPMetricObjectParser.CTYPE, metricParser);
        context.registerTeLspObjectSerializer(MetricObject.class, BGPMetricObjectParser.CTYPE, metricParser);

        final BGPPrimaryPathRouteObject primatyParser = new BGPPrimaryPathRouteObject();
        context.registerTeLspObjectParser(BGPPrimaryPathRouteObject.CLASS_NUM, BGPPrimaryPathRouteObject.CTYPE, primatyParser);
        context.registerTeLspObjectSerializer(PrimaryPathRouteObject.class, BGPPrimaryPathRouteObject.CTYPE, primatyParser);

        final BGPLspAttributesObjectParser lspAttributeParser = new BGPLspAttributesObjectParser();
        context.registerTeLspObjectParser(BGPLspAttributesObjectParser.CLASS_NUM, BGPLspAttributesObjectParser.CTYPE, lspAttributeParser);
        context.registerTeLspObjectSerializer(LspAttributesObject.class, BGPLspAttributesObjectParser.CTYPE, lspAttributeParser);

        final BGPLspRequiredAttributesObjectParser lspRequiredAttParser = new BGPLspRequiredAttributesObjectParser();
        context.registerTeLspObjectParser(BGPLspRequiredAttributesObjectParser.CLASS_NUM, BGPLspRequiredAttributesObjectParser.CTYPE,
            lspRequiredAttParser);
        context.registerTeLspObjectSerializer(LspRequiredAttributesObject.class, BGPLspRequiredAttributesObjectParser.CTYPE,
            lspRequiredAttParser);

        final BGPProtectionObjectType1Parser protectionType1Parser = new BGPProtectionObjectType1Parser();
        context.registerTeLspObjectParser(BGPProtectionObjectType1Parser.CLASS_NUM, BGPProtectionObjectType1Parser.CTYPE, protectionType1Parser);
        context.registerTeLspObjectSerializer(ProtectionObject.class, BGPProtectionObjectType1Parser.CTYPE, protectionType1Parser);

        final BGPProtectionObjectType2Parser protectionType2Parser = new BGPProtectionObjectType2Parser();
        context.registerTeLspObjectParser(BGPProtectionObjectType2Parser.CLASS_NUM, BGPProtectionObjectType2Parser.CTYPE, protectionType2Parser);
        context.registerTeLspObjectSerializer(ProtectionObject.class, BGPProtectionObjectType2Parser.CTYPE, protectionType2Parser);

        final BGPSecondaryExplicitRouteObjectParser serTypeParser = new BGPSecondaryExplicitRouteObjectParser();
        context.registerTeLspObjectParser(BGPSecondaryExplicitRouteObjectParser.CLASS_NUM, BGPSecondaryExplicitRouteObjectParser.CTYPE, serTypeParser);
        context.registerTeLspObjectSerializer(SecondaryExplicitRouteObject.class, BGPSecondaryExplicitRouteObjectParser.CTYPE, serTypeParser);

        final BGPSecondaryRecordRouteObjectParser srroTypeParser = new BGPSecondaryRecordRouteObjectParser();
        context.registerTeLspObjectParser(BGPSecondaryRecordRouteObjectParser.CLASS_NUM, BGPSecondaryRecordRouteObjectParser.CTYPE, srroTypeParser);
        context.registerTeLspObjectSerializer(SecondaryRecordRouteObject.class, BGPSecondaryRecordRouteObjectParser.CTYPE, srroTypeParser);
    }
}
