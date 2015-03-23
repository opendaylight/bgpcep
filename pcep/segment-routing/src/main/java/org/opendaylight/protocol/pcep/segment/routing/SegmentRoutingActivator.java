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
import org.opendaylight.protocol.pcep.segment.routing.parsers.AddLspSrEroSubobjectParser;
import org.opendaylight.protocol.pcep.segment.routing.parsers.AddLspSrRroSubobjectParser;
import org.opendaylight.protocol.pcep.segment.routing.parsers.PcinitiateMessageSrEroSubobjectParser;
import org.opendaylight.protocol.pcep.segment.routing.parsers.PcinitiateMessageSrRroSubobjectParser;
import org.opendaylight.protocol.pcep.segment.routing.parsers.PcrptMessageSrEroSubobjectParser;
import org.opendaylight.protocol.pcep.segment.routing.parsers.PcrptMessageSrRroSubobjectParser;
import org.opendaylight.protocol.pcep.segment.routing.parsers.PcupdMessageSrEroSubobjectParser;
import org.opendaylight.protocol.pcep.segment.routing.parsers.PcupdMessageSrRroSubobjectParser;
import org.opendaylight.protocol.pcep.segment.routing.parsers.RepliesFailureMessageSrRroSubobjectParser;
import org.opendaylight.protocol.pcep.segment.routing.parsers.RepliesSuccessMessageSrEroSubobjectParser;
import org.opendaylight.protocol.pcep.segment.routing.parsers.RepliesSuccessMessageSrRroSubobjectParser;
import org.opendaylight.protocol.pcep.segment.routing.parsers.ReportedLspSrEroSubobjectParser;
import org.opendaylight.protocol.pcep.segment.routing.parsers.ReportedLspSrRroSubobjectParser;
import org.opendaylight.protocol.pcep.segment.routing.parsers.RequestMessageRROSrRroSubobjectParser;
import org.opendaylight.protocol.pcep.segment.routing.parsers.RequestMessageReportedRouteSrRroSubobjectParser;
import org.opendaylight.protocol.pcep.segment.routing.parsers.UpdateLspSrEroSubobjectParser;
import org.opendaylight.protocol.pcep.segment.routing.parsers.UpdateLspSrRroSubobjectParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.RROSubobjectUtil;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.AbstractPCEPExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.sr.pce.capability.tlv.SrPceCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.setup.type.tlv.PathSetupType;

public class SegmentRoutingActivator extends AbstractPCEPExtensionProviderActivator {

    @Override
    protected List<AutoCloseable> startImpl(final PCEPExtensionProviderContext context) {
        final List<AutoCloseable> regs = Lists.newArrayList();

        /* Tlvs */
        regs.add(context.registerTlvParser(SrPceCapabilityTlvParser.TYPE, new SrPceCapabilityTlvParser()));
        regs.add(context.registerTlvParser(SrPathSetupTypeTlvParser.TYPE, new SrPathSetupTypeTlvParser()));

        regs.add(context.registerTlvSerializer(PathSetupType.class, new SrPathSetupTypeTlvParser()));
        regs.add(context.registerTlvSerializer(SrPceCapability.class, new SrPceCapabilityTlvParser()));

        /* Subobjects */

        /* parser for Ero-type and Rro-type */
        regs.add(context.registerEROSubobjectParser(EROSubobjectUtil.ERO_TYPE, new AddLspSrEroSubobjectParser()));
        regs.add(context.registerRROSubobjectParser(RROSubobjectUtil.RRO_TYPE, new AddLspSrRroSubobjectParser()));

        /* serializers for Ero-type and Rro-type */
        regs.add(context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.pcrep.pcrep.message.replies.result.success._case.success.paths.ero.subobject.subobject.type.SrEroType.class, new RepliesSuccessMessageSrEroSubobjectParser()));
        regs.add(context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.pcinitiate.pcinitiate.message.requests.ero.subobject.subobject.type.SrEroType.class, new PcinitiateMessageSrEroSubobjectParser()));
        regs.add(context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.pcrpt.pcrpt.message.reports.path.ero.subobject.subobject.type.SrEroType.class, new PcrptMessageSrEroSubobjectParser()));
        regs.add(context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.pcupd.pcupd.message.updates.path.ero.subobject.subobject.type.SrEroType.class, new PcupdMessageSrEroSubobjectParser()));
        regs.add(context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.add.lsp.input.arguments.ero.subobject.subobject.type.SrEroType.class, new AddLspSrEroSubobjectParser()));
        regs.add(context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.update.lsp.input.arguments.ero.subobject.subobject.type.SrEroType.class, new UpdateLspSrEroSubobjectParser()));
        regs.add(context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.network.topology.topology.node.path.computation.client.reported.lsp.path.ero.subobject.subobject.type.SrEroType.class, new ReportedLspSrEroSubobjectParser()));

        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.pcreq.pcreq.message.requests.segment.computation.p2p.reported.route.rro.subobject.subobject.type.SrRroType.class, new RequestMessageReportedRouteSrRroSubobjectParser()));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.pcinitiate.pcinitiate.message.requests.rro.subobject.subobject.type.SrRroType.class, new RequestMessageRROSrRroSubobjectParser()));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.pcrep.pcrep.message.replies.result.failure._case.rro.subobject.subobject.type.SrRroType.class, new RepliesFailureMessageSrRroSubobjectParser()));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.pcrep.pcrep.message.replies.result.success._case.success.paths.rro.subobject.subobject.type.SrRroType.class, new RepliesSuccessMessageSrRroSubobjectParser()));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.pcreq.pcreq.message.requests.segment.computation.p2p.rro.subobject.subobject.type.SrRroType.class, new PcinitiateMessageSrRroSubobjectParser()));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.pcrpt.pcrpt.message.reports.path.rro.subobject.subobject.type.SrRroType.class, new PcrptMessageSrRroSubobjectParser()));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.pcupd.pcupd.message.updates.path.rro.subobject.subobject.type.SrRroType.class, new PcupdMessageSrRroSubobjectParser()));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.add.lsp.input.arguments.rro.subobject.subobject.type.SrRroType.class, new AddLspSrRroSubobjectParser()));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.update.lsp.input.arguments.rro.subobject.subobject.type.SrRroType.class, new UpdateLspSrRroSubobjectParser()));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.network.topology.topology.node.path.computation.client.reported.lsp.path.rro.subobject.subobject.type.SrRroType.class, new ReportedLspSrRroSubobjectParser()));

        /* Objects */
        final TlvRegistry tlvReg = context.getTlvHandlerRegistry();
        final VendorInformationTlvRegistry viTlvRegistry = context.getVendorInformationTlvRegistry();
        regs.add(context.registerObjectParser(PcepOpenObjectWithSpcTlvParser.CLASS,
                PcepOpenObjectWithSpcTlvParser.TYPE, new PcepOpenObjectWithSpcTlvParser(tlvReg, viTlvRegistry)));
        regs.add(context.registerObjectSerializer(Open.class, new PcepOpenObjectWithSpcTlvParser(tlvReg, viTlvRegistry)));

        return regs;
    }
}
