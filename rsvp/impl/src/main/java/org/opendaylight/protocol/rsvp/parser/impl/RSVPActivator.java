/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.impl;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.label.GeneralizedLabelParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.label.Type1LabelParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.label.WavebandSwitchingLabelParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.ero.EROAsNumberSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.ero.EROIpv4PrefixSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.ero.EROIpv6PrefixSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.ero.EROLabelSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.ero.EROPathKey128SubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.ero.EROPathKey32SubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.ero.EROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.ero.SEROBasicProtectionSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.ero.SERODynamicProtectionSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.rro.RROIpv4PrefixSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.rro.RROIpv6PrefixSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.rro.RROLabelSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.rro.RROPathKey128SubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.rro.RROPathKey32SubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.rro.RROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.rro.SRROBasicProtectionSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.rro.SRRODynamicProtectionSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.xro.XROAsNumberSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.xro.XROIpv4PrefixSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.xro.XROIpv6PrefixSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.xro.XROPathKey128SubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.xro.XROPathKey32SubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.xro.XROSRLGSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.xro.XROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.te.AdminStatusObjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.te.AssociationObjectParserIPV4;
import org.opendaylight.protocol.rsvp.parser.impl.te.AssociationObjectParserIPV6;
import org.opendaylight.protocol.rsvp.parser.impl.te.AttributesObjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.te.BandwidthObjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.te.DetourObjectIpv4Parser;
import org.opendaylight.protocol.rsvp.parser.impl.te.DetourObjectIpv6Parser;
import org.opendaylight.protocol.rsvp.parser.impl.te.DynamicProtectionObjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.te.ExcludeRouteObjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.te.ExplicitRouteObjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.te.FastRerouteObjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.te.FlowSpecObjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.te.InformationalFastRerouteObjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.te.MetricObjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.te.PrimaryPathRouteObjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.te.ProtectionObjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.te.RecordRouteObjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.te.ReoptimizationBandwidthObjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.te.RequiredAttributesObjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.te.SecondaryExplicitRouteObjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.te.SecondaryRecordRouteObjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.te.SenderTspecObjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.te.SessionAttributeLspObjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.te.SessionAttributeLspRaObjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.AbstractRSVPExtensionProviderActivator;
import org.opendaylight.protocol.rsvp.parser.spi.LabelRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPExtensionProviderContext;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.PathKeyCase;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.secondary.explicit.route.object.secondary.explicit.route.object.subobject.container.subobject.type.BasicProtectionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.secondary.explicit.route.object.secondary.explicit.route.object.subobject.container.subobject.type.DynamicControlProtectionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.secondary.record.route.object.SecondaryRecordRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.session.attribute.object.session.attribute.object.BasicSessionAttributeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.session.attribute.object.session.attribute.object.SessionAttributeObjectWithResourcesAffinities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.tspec.object.TspecObject;

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
        final DetourObjectIpv4Parser detourIpv4 = new DetourObjectIpv4Parser();
        context.registerRsvpObjectParser(DetourObjectIpv4Parser.CLASS_NUM, DetourObjectIpv4Parser.CTYPE, detourIpv4);
        context.registerRsvpObjectSerializer(Ipv4DetourObject.class, detourIpv4);

        final DetourObjectIpv6Parser detourIpv6 = new DetourObjectIpv6Parser();
        context.registerRsvpObjectParser(DetourObjectIpv6Parser.CLASS_NUM, DetourObjectIpv6Parser.CTYPE, detourIpv6);
        context.registerRsvpObjectSerializer(Ipv6DetourObject.class, detourIpv6);

        final FastRerouteObjectParser fastC1 = new FastRerouteObjectParser();
        context.registerRsvpObjectParser(FastRerouteObjectParser.CLASS_NUM, FastRerouteObjectParser.CTYPE, fastC1);
        context.registerRsvpObjectSerializer(BasicFastRerouteObject.class, fastC1);

        final InformationalFastRerouteObjectParser fastC7 = new InformationalFastRerouteObjectParser();
        context.registerRsvpObjectParser(InformationalFastRerouteObjectParser.CLASS_NUM, InformationalFastRerouteObjectParser.CTYPE, fastC7);
        context.registerRsvpObjectSerializer(LegacyFastRerouteObject.class, fastC7);

        final SenderTspecObjectParser tSpec = new SenderTspecObjectParser();
        context.registerRsvpObjectParser(SenderTspecObjectParser.CLASS_NUM, SenderTspecObjectParser.CTYPE, tSpec);
        context.registerRsvpObjectSerializer(TspecObject.class, tSpec);

        final FlowSpecObjectParser fSpec = new FlowSpecObjectParser();
        context.registerRsvpObjectParser(FlowSpecObjectParser.CLASS_NUM, FlowSpecObjectParser.CTYPE, fSpec);
        context.registerRsvpObjectSerializer(FlowSpecObject.class, fSpec);

        final SessionAttributeLspObjectParser sAttributeC7 = new SessionAttributeLspObjectParser();
        context.registerRsvpObjectParser(SessionAttributeLspObjectParser.CLASS_NUM, SessionAttributeLspObjectParser.CTYPE, sAttributeC7);
        context.registerRsvpObjectSerializer(BasicSessionAttributeObject.class, sAttributeC7);

        final SessionAttributeLspRaObjectParser sAttributeC1 = new SessionAttributeLspRaObjectParser();
        context.registerRsvpObjectParser(SessionAttributeLspRaObjectParser.CLASS_NUM, SessionAttributeLspRaObjectParser.CTYPE, sAttributeC1);
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

        final AssociationObjectParserIPV4 ipv4AsoParser = new AssociationObjectParserIPV4();
        context.registerRsvpObjectParser(AssociationObjectParserIPV4.CLASS_NUM, AssociationObjectParserIPV4.CTYPE_IPV4, ipv4AsoParser);
        final AssociationObjectParserIPV6 ipv6AsoParser = new AssociationObjectParserIPV6();
        context.registerRsvpObjectParser(AssociationObjectParserIPV6.CLASS_NUM, AssociationObjectParserIPV6.CTYPE_IPV6, ipv6AsoParser);
        context.registerRsvpObjectSerializer(AssociationObject.class, ipv4AsoParser);

        final AdminStatusObjectParser admParser = new AdminStatusObjectParser();
        context.registerRsvpObjectParser(AdminStatusObjectParser.CLASS_NUM, AdminStatusObjectParser.CTYPE, admParser);
        context.registerRsvpObjectSerializer(AdminStatusObject.class, admParser);

        final BandwidthObjectParser bandT1Parser = new BandwidthObjectParser();
        context.registerRsvpObjectParser(BandwidthObjectParser.CLASS_NUM, BandwidthObjectParser.CTYPE, bandT1Parser);
        context.registerRsvpObjectSerializer(BasicBandwidthObject.class, bandT1Parser);

        final ReoptimizationBandwidthObjectParser bandT2Parser = new ReoptimizationBandwidthObjectParser();
        context.registerRsvpObjectParser(ReoptimizationBandwidthObjectParser.CLASS_NUM, ReoptimizationBandwidthObjectParser.CTYPE, bandT2Parser);
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

        final ProtectionObjectParser protectionType1Parser = new ProtectionObjectParser();
        context.registerRsvpObjectParser(ProtectionObjectParser.CLASS_NUM, ProtectionObjectParser.CTYPE, protectionType1Parser);
        context.registerRsvpObjectSerializer(BasicProtectionObject.class, protectionType1Parser);

        final DynamicProtectionObjectParser protectionType2Parser = new DynamicProtectionObjectParser();
        context.registerRsvpObjectParser(DynamicProtectionObjectParser.CLASS_NUM, DynamicProtectionObjectParser.CTYPE, protectionType2Parser);
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

        final RROPathKey32SubobjectParser pathKey32Parser = new RROPathKey32SubobjectParser();
        final RROPathKey128SubobjectParser pathKey128Parser = new RROPathKey128SubobjectParser();
        regs.add(context.registerRROSubobjectParser(RROPathKey32SubobjectParser.TYPE, pathKey32Parser));
        regs.add(context.registerRROSubobjectParser(RROPathKey128SubobjectParser.TYPE, pathKey128Parser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                .yang.rsvp.rev150820.record.route.subobjects.subobject.type.PathKeyCase.class, pathKey32Parser));

        final RROLabelSubobjectParser labelParser = new RROLabelSubobjectParser(labelReg);
        regs.add(context.registerRROSubobjectParser(RROLabelSubobjectParser.TYPE, labelParser));
        regs.add(context.registerRROSubobjectSerializer(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.LabelCase.class,
            labelParser));

        final SRROBasicProtectionSubobjectParser srroBasicParser = new SRROBasicProtectionSubobjectParser();
        final SRRODynamicProtectionSubobjectParser srroDynamicParser = new SRRODynamicProtectionSubobjectParser();
        regs.add(context.registerRROSubobjectParser(SRROBasicProtectionSubobjectParser.TYPE, srroBasicParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.secondary.record.route.object.secondary.record.route.object.subobject.container.subobject.type.BasicProtectionCase.class, srroBasicParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.secondary.record.route.object.secondary.record.route.object.subobject.container.subobject.type.DynamicControlProtectionCase.class, srroDynamicParser));
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
