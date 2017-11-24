/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.segment.routing;

import com.google.common.collect.Lists;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.AbstractPCEPExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.add.lsp.input.arguments.ero.subobject.subobject.type.SrEroType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.add.lsp.input.arguments.rro.subobject.subobject.type.SrRroType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.sr.pce.capability.tlv.SrPceCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.setup.type.tlv.PathSetupType;

public class SegmentRoutingActivator extends AbstractPCEPExtensionProviderActivator {

    private final boolean ianaSrSubobjectsType;

    public SegmentRoutingActivator() {
        this.ianaSrSubobjectsType = false;
    }

    public SegmentRoutingActivator(final boolean ianaSrSubobjectsType) {
        this.ianaSrSubobjectsType = ianaSrSubobjectsType;
    }

    @Override
    protected List<AutoCloseable> startImpl(final PCEPExtensionProviderContext context) {
        final List<AutoCloseable> regs = Lists.newArrayList();

        /* Tlvs */
        regs.add(context.registerTlvParser(SrPceCapabilityTlvParser.TYPE, new SrPceCapabilityTlvParser()));
        regs.add(context.registerTlvParser(SrPathSetupTypeTlvParser.TYPE, new SrPathSetupTypeTlvParser()));

        regs.add(context.registerTlvSerializer(PathSetupType.class, new SrPathSetupTypeTlvParser()));
        regs.add(context.registerTlvSerializer(SrPceCapability.class, new SrPceCapabilityTlvParser()));

        /* Subobjects */
        final SrEroSubobjectParser srEroSubobjectParser = new SrEroSubobjectParser(this.ianaSrSubobjectsType);
        regs.add(context.registerEROSubobjectParser(srEroSubobjectParser.getCodePoint(), srEroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.network.topology.topology.node.path.computation.client.reported.lsp.path.ero.subobject.subobject.type.SrEroType.class,
                srEroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.pcinitiate.pcinitiate.message.requests.ero.subobject.subobject.type.SrEroType.class,
                srEroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.pcrep.pcrep.message.replies.result.success._case.success.paths.ero.subobject.subobject.type.SrEroType.class,
                srEroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.pcrpt.pcrpt.message.reports.path.ero.subobject.subobject.type.SrEroType.class,
                srEroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.pcupd.pcupd.message.updates.path.ero.subobject.subobject.type.SrEroType.class,
                srEroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.update.lsp.input.arguments.ero.subobject.subobject.type.SrEroType.class,
                srEroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(SrEroType.class, srEroSubobjectParser));

        final SrRroSubobjectParser srRroSubobjectParser = new SrRroSubobjectParser(this.ianaSrSubobjectsType);
        regs.add(context.registerRROSubobjectParser(srRroSubobjectParser.getCodePoint(), srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.network.topology.topology.node.path.computation.client.reported.lsp.path.rro.subobject.subobject.type.SrRroType.class,
                srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.pcinitiate.pcinitiate.message.requests.rro.subobject.subobject.type.SrRroType.class,
                srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.pcrep.pcrep.message.replies.result.failure._case.rro.subobject.subobject.type.SrRroType.class,
                srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.pcreq.pcreq.message.requests.segment.computation.p2p.reported.route.rro.subobject.subobject.type.SrRroType.class,
                srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.pcrpt.pcrpt.message.reports.path.rro.subobject.subobject.type.SrRroType.class,
                srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.pcupd.pcupd.message.updates.path.rro.subobject.subobject.type.SrRroType.class,
                srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.update.lsp.input.arguments.rro.subobject.subobject.type.SrRroType.class, srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.pcrep.pcrep.message.replies.result.success._case.success.paths.rro.subobject.subobject.type.SrRroType.class,
                srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.pcreq.pcreq.message.requests.segment.computation.p2p.rro.subobject.subobject.type.SrRroType.class,
                srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(SrRroType.class, srRroSubobjectParser));

        /* Objects */
        final TlvRegistry tlvReg = context.getTlvHandlerRegistry();
        final VendorInformationTlvRegistry viTlvRegistry = context.getVendorInformationTlvRegistry();
        regs.add(context.registerObjectParser(PcepOpenObjectWithSpcTlvParser.CLASS,
                PcepOpenObjectWithSpcTlvParser.TYPE, new PcepOpenObjectWithSpcTlvParser(tlvReg, viTlvRegistry)));
        regs.add(context.registerObjectSerializer(Open.class, new PcepOpenObjectWithSpcTlvParser(tlvReg, viTlvRegistry)));

        return regs;
    }
}
