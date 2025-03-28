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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250328.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250328.path.setup.type.tlv.PathSetupType;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Component;

@Singleton
@MetaInfServices
@Component(immediate = true)
public class SegmentRoutingActivator implements PCEPExtensionProviderActivator {

    @Inject
    public SegmentRoutingActivator() {
        // Hidden on purpose
    }

    @Override
    public List<Registration> start(final PCEPExtensionProviderContext context) {
        final List<Registration> regs = new ArrayList<>();

        // TLVs
        regs.add(context.registerTlvParser(SrPceCapabilityTlvParser.TYPE, new SrPceCapabilityTlvParser()));
        regs.add(context.registerTlvParser(SrPathSetupTypeTlvParser.TYPE, new SrPathSetupTypeTlvParser()));

        regs.add(context.registerTlvSerializer(PathSetupType.class, new SrPathSetupTypeTlvParser()));
        regs.add(context.registerTlvSerializer(SrPceCapability.class, new SrPceCapabilityTlvParser()));

        // SR-ERO SubTLVs
        final SrEroSubobjectParser srEroSubobjectParser = new SrEroSubobjectParser();
        regs.add(context.registerEROSubobjectParser(SrEroSubobjectParser.TYPE, srEroSubobjectParser));
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

        // SR-RRO SubTLVs
        final SrRroSubobjectParser srRroSubobjectParser = new SrRroSubobjectParser();
        regs.add(context.registerRROSubobjectParser(SrRroSubobjectParser.TYPE, srRroSubobjectParser));
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
        return MoreObjects.toStringHelper(this).toString();
    }
}
