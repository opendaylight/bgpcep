/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.segment.routing;

import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderActivator;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.add.lsp.input.arguments.ero.subobject.subobject.type.SrEroType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.add.lsp.input.arguments.rro.subobject.subobject.type.SrRroType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.sr.pce.capability.tlv.SrPceCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.path.setup.type.tlv.PathSetupType;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Singleton
@MetaInfServices
@Component(immediate = true)
@Designate(ocd = SegmentRoutingActivator.Configuration.class)
public class SegmentRoutingActivator implements PCEPExtensionProviderActivator {
    @Deprecated
    @ObjectClassDefinition(description = "Configuration parameters for SegmentRoutingActivator")
    public @interface Configuration {
        @AttributeDefinition(description = """
            If true (default) IANA Types for SR-ERO type (=36) and SR-RRO type (=36) are used, else historical types
            (5 & 6) are used for parsing/serialization.
            """)
        boolean ianaSrSubobjectsType() default true;
    }

    @Deprecated
    private final boolean ianaSrSubobjectsType;

    @Inject
    public SegmentRoutingActivator() {
        this(true);
    }

    @Activate
    public SegmentRoutingActivator(final Configuration config) {
        this(config.ianaSrSubobjectsType());
    }

    @Deprecated
    public SegmentRoutingActivator(final boolean ianaSrSubobjectsType) {
        this.ianaSrSubobjectsType = ianaSrSubobjectsType;
    }

    @Override
    public List<Registration> start(final PCEPExtensionProviderContext context) {
        final List<Registration> regs = new ArrayList<>();

        /* Tlvs */
        regs.add(context.registerTlvParser(SrPceCapabilityTlvParser.TYPE, new SrPceCapabilityTlvParser()));
        regs.add(context.registerTlvParser(SrPathSetupTypeTlvParser.TYPE, new SrPathSetupTypeTlvParser()));

        regs.add(context.registerTlvSerializer(PathSetupType.class, new SrPathSetupTypeTlvParser()));
        regs.add(context.registerTlvSerializer(SrPceCapability.class, new SrPceCapabilityTlvParser()));

        /* Subobjects */
        final SrEroSubobjectParser srEroSubobjectParser = new SrEroSubobjectParser(ianaSrSubobjectsType);
        regs.add(context.registerEROSubobjectParser(srEroSubobjectParser.getCodePoint(), srEroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev200720.network.topology.topology.node.path.computation.client.reported.lsp.path.ero
            .subobject.subobject.type.SrEroType.class,srEroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev200720.pcinitiate.pcinitiate.message.requests.ero.subobject.subobject.type
            .SrEroType.class, srEroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev200720.pcrep.pcrep.message.replies.result.success._case.success.paths.ero.subobject
            .subobject.type.SrEroType.class, srEroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev200720.pcrpt.pcrpt.message.reports.path.ero.subobject.subobject.type
            .SrEroType.class, srEroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev200720.pcupd.pcupd.message.updates.path.ero.subobject.subobject.type
            .SrEroType.class, srEroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev200720.update.lsp.input.arguments.ero.subobject.subobject.type.SrEroType.class,
            srEroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(SrEroType.class, srEroSubobjectParser));

        final SrRroSubobjectParser srRroSubobjectParser = new SrRroSubobjectParser(ianaSrSubobjectsType);
        regs.add(context.registerRROSubobjectParser(srRroSubobjectParser.getCodePoint(), srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev200720.network.topology.topology.node.path.computation.client.reported.lsp.path.rro
            .subobject.subobject.type.SrRroType.class, srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev200720.pcinitiate.pcinitiate.message.requests.rro.subobject.subobject.type
            .SrRroType.class, srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev200720.pcrep.pcrep.message.replies.result.failure._case.rro.subobject.subobject
            .type.SrRroType.class, srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev200720.pcreq.pcreq.message.requests.segment.computation.p2p.reported.route.rro
            .subobject.subobject.type.SrRroType.class, srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev200720.pcrpt.pcrpt.message.reports.path.rro.subobject.subobject.type
            .SrRroType.class, srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev200720.pcupd.pcupd.message.updates.path.rro.subobject.subobject.type
            .SrRroType.class, srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev200720.update.lsp.input.arguments.rro.subobject.subobject.type.SrRroType.class,
            srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev200720.pcrep.pcrep.message.replies.result.success._case.success.paths.rro.subobject
            .subobject.type.SrRroType.class, srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev200720.pcreq.pcreq.message.requests.segment.computation.p2p.rro.subobject.subobject
            .type.SrRroType.class, srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(SrRroType.class, srRroSubobjectParser));

        /* Objects */
        final TlvRegistry tlvReg = context.getTlvHandlerRegistry();
        final VendorInformationTlvRegistry viTlvRegistry = context.getVendorInformationTlvRegistry();
        regs.add(context.registerObjectParser(new PcepOpenObjectWithSpcTlvParser(tlvReg, viTlvRegistry)));
        regs.add(context.registerObjectSerializer(Open.class,
            new PcepOpenObjectWithSpcTlvParser(tlvReg, viTlvRegistry)));

        return regs;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("ianaSubobjects", ianaSrSubobjectsType).toString();
    }
}
