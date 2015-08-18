/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package parser.impl;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.rsvp.parser.spi.AbstractRSVPExtensionProviderActivator;
import org.opendaylight.protocol.rsvp.parser.spi.LabelRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobject.subobject.type.PathKeyCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.admin.status.object.AdminStatusObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.association.object.AssociationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.bandwidth.object.bandwidth.object.BasicBandwidthObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.bandwidth.object.bandwidth.object.ReoptimizationBandwidthObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.AsNumberCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.SrlgCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.UnnumberedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.detour.object.detour.object.Ipv4DetourObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.detour.object.detour.object.Ipv6DetourObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.exclude.route.object.ExcludeRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.object.ExplicitRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.BasicProtectionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.DynamicControlProtectionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.fast.reroute.object.fast.reroute.object.BasicFastRerouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.fast.reroute.object.fast.reroute.object.LegacyFastRerouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.flow.spec.object.FlowSpecObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.GeneralizedLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.Type1LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.WavebandSwitchingLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.lsp.attributes.object.LspAttributesObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.lsp.required.attributes.object.LspRequiredAttributesObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.metric.object.MetricObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.primary.path.route.object.PrimaryPathRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.protection.object.protection.object.BasicProtectionObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.protection.object.protection.object.DynamicControlProtectionObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.object.RecordRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.secondary.explicit.route.object.SecondaryExplicitRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.secondary.record.route.object.SecondaryRecordRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.session.attribute.object.session.attribute.object.BasicSessionAttributeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.session.attribute.object.session.attribute.object.SessionAttributeObjectWithResourcesAffinities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.tspec.object.TspecObject;
import parser.impl.TE.AdminStatusObjectParser;
import parser.impl.TE.AssociationObjectParserType1;
import parser.impl.TE.AssociationObjectParserType2;
import parser.impl.TE.AttributesObjectParser;
import parser.impl.TE.BandwidthObjectType1Parser;
import parser.impl.TE.BandwidthObjectType2Parser;
import parser.impl.TE.DetourObjectType7Parser;
import parser.impl.TE.DetourObjectType8Parser;
import parser.impl.TE.ExcludeRouteObjectParser;
import parser.impl.TE.ExplicitRouteObjectParser;
import parser.impl.TE.FastRerouteObjectType1Parser;
import parser.impl.TE.FastRerouteObjectType7Parser;
import parser.impl.TE.FlowSpecObjectParser;
import parser.impl.TE.MetricObjectParser;
import parser.impl.TE.PrimaryPathRouteObjectParser;
import parser.impl.TE.ProtectionObjectType1Parser;
import parser.impl.TE.ProtectionObjectType2Parser;
import parser.impl.TE.RecordRouteObjectParser;
import parser.impl.TE.RequiredAttributesObjectParser;
import parser.impl.TE.SecondaryExplicitRouteObjectParser;
import parser.impl.TE.SecondaryRecordRouteObjectParser;
import parser.impl.TE.SenderTspecObjectParser;
import parser.impl.TE.SessionAttributeObjectType1Parser;
import parser.impl.TE.SessionAttributeObjectType7Parser;
import parser.impl.subobject.ERO.EROAsNumberSubobjectParser;
import parser.impl.subobject.ERO.EROIpv4PrefixSubobjectParser;
import parser.impl.subobject.ERO.EROIpv6PrefixSubobjectParser;
import parser.impl.subobject.ERO.EROLabelSubobjectParser;
import parser.impl.subobject.ERO.EROPathKey128SubobjectParser;
import parser.impl.subobject.ERO.EROPathKey32SubobjectParser;
import parser.impl.subobject.ERO.EROUnnumberedInterfaceSubobjectParser;
import parser.impl.subobject.ERO.SEROBasicProtectionSubobjectParser;
import parser.impl.subobject.ERO.SERODynamicProtectionSubobjectParser;
import parser.impl.subobject.Label.GeneralizedLabelParser;
import parser.impl.subobject.Label.Type1LabelParser;
import parser.impl.subobject.Label.WavebandSwitchingLabelParser;
import parser.impl.subobject.RRO.RROIpv4PrefixSubobjectParser;
import parser.impl.subobject.RRO.RROIpv6PrefixSubobjectParser;
import parser.impl.subobject.RRO.RROLabelSubobjectParser;
import parser.impl.subobject.RRO.RROPathKey128SubobjectParser;
import parser.impl.subobject.RRO.RROPathKey32SubobjectParser;
import parser.impl.subobject.RRO.RROUnnumberedInterfaceSubobjectParser;
import parser.impl.subobject.RRO.SRROBasicProtectionSubobjectParser;
import parser.impl.subobject.RRO.SRRODynamicProtectionSubobjectParser;
import parser.impl.subobject.XRO.XROAsNumberSubobjectParser;
import parser.impl.subobject.XRO.XROIpv4PrefixSubobjectParser;
import parser.impl.subobject.XRO.XROIpv6PrefixSubobjectParser;
import parser.impl.subobject.XRO.XROPathKey128SubobjectParser;
import parser.impl.subobject.XRO.XROPathKey32SubobjectParser;
import parser.impl.subobject.XRO.XROSRLGSubobjectParser;
import parser.impl.subobject.XRO.XROUnnumberedInterfaceSubobjectParser;

public class RSVPActivator extends AbstractRSVPExtensionProviderActivator {
    @Override
    protected List<AutoCloseable> startImpl(final RSVPExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>();

        registerLabelParsers(regs, context);
        final LabelRegistry labelReg = context.getLabelHandlerRegistry();
        registerRROParsers(regs, context, labelReg);
        registerXROParsers(regs, context);
        registerEROParsers(regs, context, labelReg);
        registerRSVPTEParsers(context);

        return regs;
    }

    private void registerRSVPTEParsers(final RSVPExtensionProviderContext context) {
        final DetourObjectType7Parser detourC7 = new DetourObjectType7Parser();
        context.registerRsvpObjectParser(DetourObjectType7Parser.CLASS_NUM, DetourObjectType7Parser.CTYPE, detourC7);
        context.registerRsvpObjectSerializer(Ipv4DetourObject.class, detourC7);

        final DetourObjectType8Parser detourC8 = new DetourObjectType8Parser();
        context.registerRsvpObjectParser(DetourObjectType8Parser.CLASS_NUM, DetourObjectType8Parser.CTYPE, detourC8);
        context.registerRsvpObjectSerializer(Ipv6DetourObject.class, detourC8);

        final FastRerouteObjectType1Parser fastC1 = new FastRerouteObjectType1Parser();
        context.registerRsvpObjectParser(FastRerouteObjectType1Parser.CLASS_NUM, FastRerouteObjectType1Parser.CTYPE, fastC1);
        context.registerRsvpObjectSerializer(BasicFastRerouteObject.class, fastC1);

        final FastRerouteObjectType7Parser fastC7 = new FastRerouteObjectType7Parser();
        context.registerRsvpObjectParser(FastRerouteObjectType7Parser.CLASS_NUM, FastRerouteObjectType7Parser.CTYPE, fastC7);
        context.registerRsvpObjectSerializer(LegacyFastRerouteObject.class, fastC7);

        final SenderTspecObjectParser tSpec = new SenderTspecObjectParser();
        context.registerRsvpObjectParser(SenderTspecObjectParser.CLASS_NUM, SenderTspecObjectParser.CTYPE, tSpec);
        context.registerRsvpObjectSerializer(TspecObject.class, tSpec);

        final FlowSpecObjectParser fSpec = new FlowSpecObjectParser();
        context.registerRsvpObjectParser(FlowSpecObjectParser.CLASS_NUM, FlowSpecObjectParser.CTYPE, fSpec);
        context.registerRsvpObjectSerializer(FlowSpecObject.class, fSpec);

        final SessionAttributeObjectType7Parser sAttributeC7 = new SessionAttributeObjectType7Parser();
        context.registerRsvpObjectParser(SessionAttributeObjectType7Parser.CLASS_NUM, SessionAttributeObjectType7Parser.CTYPE, sAttributeC7);
        context.registerRsvpObjectSerializer(BasicSessionAttributeObject.class, sAttributeC7);

        final SessionAttributeObjectType1Parser sAttributeC1 = new SessionAttributeObjectType1Parser();
        context.registerRsvpObjectParser(SessionAttributeObjectType1Parser.CLASS_NUM, SessionAttributeObjectType1Parser.CTYPE, sAttributeC1);
        context.registerRsvpObjectSerializer(SessionAttributeObjectWithResourcesAffinities.class, sAttributeC1);

        final RecordRouteObjectParser rroParser = new RecordRouteObjectParser(context.getRROSubobjectHandlerRegistry());
        context.registerRsvpObjectParser(RecordRouteObjectParser.CLASS_NUM, RecordRouteObjectParser.CTYPE, rroParser);
        context.registerRsvpObjectSerializer(RecordRouteObject.class, rroParser);

        final ExcludeRouteObjectParser xroParser = new ExcludeRouteObjectParser(context.getXROSubobjectHandlerRegistry());
        context.registerRsvpObjectParser(ExcludeRouteObjectParser.CLASS_NUM, ExcludeRouteObjectParser.CTYPE, xroParser);
        context.registerRsvpObjectSerializer(ExcludeRouteObject.class, xroParser);

        final ExplicitRouteObjectParser eroParser = new ExplicitRouteObjectParser(context.getEROSubobjectHandlerRegistry());
        context.registerRsvpObjectParser(ExplicitRouteObjectParser.CLASS_NUM, ExplicitRouteObjectParser.CTYPE, eroParser);
        context.registerRsvpObjectSerializer(ExplicitRouteObject.class, eroParser);

        final AssociationObjectParserType1 ipv4AsoParser = new AssociationObjectParserType1();
        context.registerRsvpObjectParser(AssociationObjectParserType1.CLASS_NUM, AssociationObjectParserType1.CTYPE_IPV4, ipv4AsoParser);
        final AssociationObjectParserType2 ipv6AsoParser = new AssociationObjectParserType2();
        context.registerRsvpObjectParser(AssociationObjectParserType2.CLASS_NUM, AssociationObjectParserType2.CTYPE_IPV6, ipv6AsoParser);
        context.registerRsvpObjectSerializer(AssociationObject.class, ipv4AsoParser);

        final AdminStatusObjectParser admParser = new AdminStatusObjectParser();
        context.registerRsvpObjectParser(AdminStatusObjectParser.CLASS_NUM, AdminStatusObjectParser.CTYPE, admParser);
        context.registerRsvpObjectSerializer(AdminStatusObject.class, admParser);

        final BandwidthObjectType1Parser bandT1Parser = new BandwidthObjectType1Parser();
        context.registerRsvpObjectParser(BandwidthObjectType1Parser.CLASS_NUM, BandwidthObjectType1Parser.CTYPE, bandT1Parser);
        context.registerRsvpObjectSerializer(BasicBandwidthObject.class, bandT1Parser);

        final BandwidthObjectType2Parser bandT2Parser = new BandwidthObjectType2Parser();
        context.registerRsvpObjectParser(BandwidthObjectType1Parser.CLASS_NUM, BandwidthObjectType2Parser.CTYPE, bandT2Parser);
        context.registerRsvpObjectSerializer(ReoptimizationBandwidthObject.class, bandT2Parser);

        final MetricObjectParser metricParser = new MetricObjectParser();
        context.registerRsvpObjectParser(MetricObjectParser.CLASS_NUM, MetricObjectParser.CTYPE, metricParser);
        context.registerRsvpObjectSerializer(MetricObject.class, metricParser);

        final PrimaryPathRouteObjectParser primatyParser = new PrimaryPathRouteObjectParser(context.getEROSubobjectHandlerRegistry());
        context.registerRsvpObjectParser(PrimaryPathRouteObjectParser.CLASS_NUM, PrimaryPathRouteObjectParser.CTYPE, primatyParser);
        context.registerRsvpObjectSerializer(PrimaryPathRouteObject.class, primatyParser);

        final AttributesObjectParser lspAttributeParser = new AttributesObjectParser();
        context.registerRsvpObjectParser(AttributesObjectParser.CLASS_NUM, AttributesObjectParser.CTYPE, lspAttributeParser);
        context.registerRsvpObjectSerializer(LspAttributesObject.class, lspAttributeParser);

        final RequiredAttributesObjectParser lspRequiredAttParser = new RequiredAttributesObjectParser();
        context.registerRsvpObjectParser(RequiredAttributesObjectParser.CLASS_NUM, RequiredAttributesObjectParser.CTYPE,
            lspRequiredAttParser);
        context.registerRsvpObjectSerializer(LspRequiredAttributesObject.class, lspRequiredAttParser);

        final ProtectionObjectType1Parser protectionType1Parser = new ProtectionObjectType1Parser();
        context.registerRsvpObjectParser(ProtectionObjectType1Parser.CLASS_NUM, ProtectionObjectType1Parser.CTYPE, protectionType1Parser);
        context.registerRsvpObjectSerializer(BasicProtectionObject.class, protectionType1Parser);

        final ProtectionObjectType2Parser protectionType2Parser = new ProtectionObjectType2Parser();
        context.registerRsvpObjectParser(ProtectionObjectType2Parser.CLASS_NUM, ProtectionObjectType2Parser.CTYPE, protectionType2Parser);
        context.registerRsvpObjectSerializer(DynamicControlProtectionObject.class, protectionType2Parser);

        final SecondaryExplicitRouteObjectParser serTypeParser = new SecondaryExplicitRouteObjectParser(context.getEROSubobjectHandlerRegistry());
        context.registerRsvpObjectParser(SecondaryExplicitRouteObjectParser.CLASS_NUM, SecondaryExplicitRouteObjectParser.CTYPE, serTypeParser);
        context.registerRsvpObjectSerializer(SecondaryExplicitRouteObject.class, serTypeParser);

        final SecondaryRecordRouteObjectParser srroTypeParser = new SecondaryRecordRouteObjectParser(context.getRROSubobjectHandlerRegistry());
        context.registerRsvpObjectParser(SecondaryRecordRouteObjectParser.CLASS_NUM, SecondaryRecordRouteObjectParser.CTYPE, srroTypeParser);
        context.registerRsvpObjectSerializer(SecondaryRecordRouteObject.class, srroTypeParser);
    }

    private void registerLabelParsers(final List<AutoCloseable> regs, final RSVPExtensionProviderContext context) {
        final Type1LabelParser type1Parser = new Type1LabelParser();
        regs.add(context.registerLabelParser(Type1LabelParser.CTYPE, type1Parser));
        regs.add(context.registerLabelSerializer(Type1LabelCase.class, type1Parser));

        final GeneralizedLabelParser generalizedParser = new GeneralizedLabelParser();
        regs.add(context.registerLabelParser(GeneralizedLabelParser.CTYPE, generalizedParser));
        regs.add(context.registerLabelSerializer(GeneralizedLabelCase.class, generalizedParser));

        final WavebandSwitchingLabelParser wavebandParser = new WavebandSwitchingLabelParser();
        regs.add(context.registerLabelParser(WavebandSwitchingLabelParser.CTYPE, wavebandParser));
        regs.add(context.registerLabelSerializer(WavebandSwitchingLabelCase.class, wavebandParser));
    }

    private void registerRROParsers(final List<AutoCloseable> regs, final RSVPExtensionProviderContext context, final LabelRegistry labelReg) {
        final RROIpv4PrefixSubobjectParser ipv4prefixParser = new RROIpv4PrefixSubobjectParser();
        regs.add(context.registerRROSubobjectParser(RROIpv4PrefixSubobjectParser.TYPE, ipv4prefixParser));
        regs.add(context.registerRROSubobjectSerializer(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.IpPrefixCase.class,
            ipv4prefixParser));
        regs.add(context.registerRROSubobjectParser(RROIpv6PrefixSubobjectParser.TYPE, new RROIpv6PrefixSubobjectParser()));

        final RROUnnumberedInterfaceSubobjectParser unnumberedParser = new RROUnnumberedInterfaceSubobjectParser();
        regs.add(context.registerRROSubobjectParser(RROUnnumberedInterfaceSubobjectParser.TYPE, unnumberedParser));
        regs.add(context.registerRROSubobjectSerializer(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.UnnumberedCase.class,
            unnumberedParser));

        final RROPathKey32SubobjectParser pathKeyParser = new RROPathKey32SubobjectParser();
        regs.add(context.registerRROSubobjectParser(RROPathKey32SubobjectParser.TYPE, pathKeyParser));
        regs.add(context.registerRROSubobjectParser(RROPathKey128SubobjectParser.TYPE, new RROPathKey128SubobjectParser()));
        regs.add(context.registerRROSubobjectSerializer(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.subobject.subobject.type.PathKeyCase.class,
            pathKeyParser));

        final RROLabelSubobjectParser labelParser = new RROLabelSubobjectParser(labelReg);
        regs.add(context.registerRROSubobjectParser(RROLabelSubobjectParser.TYPE, labelParser));
        regs.add(context.registerRROSubobjectSerializer(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.LabelCase.class,
            labelParser));

        final SRROBasicProtectionSubobjectParser ssroBasicParser = new SRROBasicProtectionSubobjectParser();
        final SRRODynamicProtectionSubobjectParser ssroDynamicParser = new SRRODynamicProtectionSubobjectParser();
        regs.add(context.registerRROSubobjectParser(SRROBasicProtectionSubobjectParser.TYPE, ssroBasicParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.
            BasicProtectionCase.class, ssroBasicParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.
            DynamicControlProtectionCase.class, ssroDynamicParser));
    }

    private void registerXROParsers(final List<AutoCloseable> regs, final RSVPExtensionProviderContext context) {
        final XROIpv4PrefixSubobjectParser ipv4prefixParser = new XROIpv4PrefixSubobjectParser();
        regs.add(context.registerXROSubobjectParser(XROIpv4PrefixSubobjectParser.TYPE, ipv4prefixParser));
        regs.add(context.registerXROSubobjectSerializer(IpPrefixCase.class, ipv4prefixParser));
        regs.add(context.registerXROSubobjectParser(XROIpv6PrefixSubobjectParser.TYPE, new XROIpv6PrefixSubobjectParser()));

        final XROAsNumberSubobjectParser asNumberParser = new XROAsNumberSubobjectParser();
        regs.add(context.registerXROSubobjectParser(XROAsNumberSubobjectParser.TYPE, asNumberParser));
        regs.add(context.registerXROSubobjectSerializer(AsNumberCase.class, asNumberParser));

        final XROSRLGSubobjectParser srlgParser = new XROSRLGSubobjectParser();
        regs.add(context.registerXROSubobjectParser(XROSRLGSubobjectParser.TYPE, srlgParser));
        regs.add(context.registerXROSubobjectSerializer(SrlgCase.class, srlgParser));

        final XROUnnumberedInterfaceSubobjectParser unnumberedParser = new XROUnnumberedInterfaceSubobjectParser();
        regs.add(context.registerXROSubobjectParser(XROUnnumberedInterfaceSubobjectParser.TYPE, unnumberedParser));
        regs.add(context.registerXROSubobjectSerializer(UnnumberedCase.class, unnumberedParser));

        final XROPathKey32SubobjectParser pathKeyParser = new XROPathKey32SubobjectParser();
        regs.add(context.registerXROSubobjectParser(XROPathKey32SubobjectParser.TYPE, pathKeyParser));
        regs.add(context.registerXROSubobjectParser(XROPathKey128SubobjectParser.TYPE, new XROPathKey128SubobjectParser()));
        regs.add(context.registerXROSubobjectSerializer(PathKeyCase.class, pathKeyParser));
    }

    private void registerEROParsers(final List<AutoCloseable> regs, final RSVPExtensionProviderContext context, final LabelRegistry labelReg) {
        final EROIpv4PrefixSubobjectParser ipv4prefixParser = new EROIpv4PrefixSubobjectParser();
        regs.add(context.registerEROSubobjectParser(EROIpv4PrefixSubobjectParser.TYPE, ipv4prefixParser));
        regs.add(context.registerEROSubobjectSerializer(IpPrefixCase.class, ipv4prefixParser));
        regs.add(context.registerEROSubobjectParser(EROIpv6PrefixSubobjectParser.TYPE, new EROIpv6PrefixSubobjectParser()));

        final EROAsNumberSubobjectParser asNumberParser = new EROAsNumberSubobjectParser();
        regs.add(context.registerEROSubobjectParser(EROAsNumberSubobjectParser.TYPE, asNumberParser));
        regs.add(context.registerEROSubobjectSerializer(AsNumberCase.class, asNumberParser));

        final EROUnnumberedInterfaceSubobjectParser unnumberedParser = new EROUnnumberedInterfaceSubobjectParser();
        regs.add(context.registerEROSubobjectParser(EROUnnumberedInterfaceSubobjectParser.TYPE, unnumberedParser));
        regs.add(context.registerEROSubobjectSerializer(UnnumberedCase.class, unnumberedParser));

        final EROPathKey32SubobjectParser pathKeyParser = new EROPathKey32SubobjectParser();
        regs.add(context.registerEROSubobjectParser(EROPathKey32SubobjectParser.TYPE, pathKeyParser));
        regs.add(context.registerEROSubobjectParser(EROPathKey128SubobjectParser.TYPE, new EROPathKey128SubobjectParser()));
        regs.add(context.registerEROSubobjectSerializer(PathKeyCase.class, pathKeyParser));

        final EROLabelSubobjectParser labelParser = new EROLabelSubobjectParser(labelReg);
        regs.add(context.registerEROSubobjectParser(EROLabelSubobjectParser.TYPE, labelParser));
        regs.add(context.registerEROSubobjectSerializer(LabelCase.class, labelParser));

        final SERODynamicProtectionSubobjectParser seroDynamicParser = new SERODynamicProtectionSubobjectParser();
        final SEROBasicProtectionSubobjectParser seroBasicParser = new SEROBasicProtectionSubobjectParser();
        regs.add(context.registerEROSubobjectParser(SERODynamicProtectionSubobjectParser.TYPE, seroBasicParser));
        regs.add(context.registerEROSubobjectSerializer(DynamicControlProtectionCase.class, seroDynamicParser));
        regs.add(context.registerEROSubobjectSerializer(BasicProtectionCase.class, seroBasicParser));
    }
}
