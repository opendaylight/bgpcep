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
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.AbstractPCEPExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.add.lsp.input.arguments.rro.subobject.subobject.type.SrRroType;
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

        /* Ero-type */
        regs.add(context.registerEROSubobjectParser(RepliesSuccessMessageSrEroSubobjectParser.TYPE, new RepliesSuccessMessageSrEroSubobjectParser()));
        regs.add(context.registerEROSubobjectParser(PcinitiateMessageSrEroSubobjectParser.TYPE, new PcinitiateMessageSrEroSubobjectParser()));
        regs.add(context.registerEROSubobjectParser(PcrptMessageSrEroSubobjectParser.TYPE, new PcrptMessageSrEroSubobjectParser()));
        regs.add(context.registerEROSubobjectParser(PcupdMessageSrEroSubobjectParser.TYPE, new PcupdMessageSrEroSubobjectParser()));
        regs.add(context.registerEROSubobjectParser(AddLspSrEroSubobjectParser.TYPE, new AddLspSrEroSubobjectParser()));
        regs.add(context.registerEROSubobjectParser(UpdateLspSrEroSubobjectParser.TYPE, new UpdateLspSrEroSubobjectParser()));
        regs.add(context.registerEROSubobjectParser(ReportedLspSrEroSubobjectParser.TYPE, new ReportedLspSrEroSubobjectParser()));

        /* Rro-type */
        regs.add(context.registerRROSubobjectParser(RequestMessageReportedRouteSrRroSubobjectParser.TYPE, new RequestMessageReportedRouteSrRroSubobjectParser()));
        regs.add(context.registerRROSubobjectParser(RequestMessageRROSrRroSubobjectParser.TYPE, new RequestMessageRROSrRroSubobjectParser()));
        regs.add(context.registerRROSubobjectParser(RepliesFailureMessageSrRroSubobjectParser.TYPE, new RepliesFailureMessageSrRroSubobjectParser()));
        regs.add(context.registerRROSubobjectParser(RepliesSuccessMessageSrRroSubobjectParser.TYPE, new RepliesSuccessMessageSrRroSubobjectParser()));
        regs.add(context.registerRROSubobjectParser(PcinitiateMessageSrRroSubobjectParser.TYPE, new PcinitiateMessageSrRroSubobjectParser()));
        regs.add(context.registerRROSubobjectParser(PcrptMessageSrRroSubobjectParser.TYPE, new PcrptMessageSrRroSubobjectParser()));
        regs.add(context.registerRROSubobjectParser(PcupdMessageSrRroSubobjectParser.TYPE, new PcupdMessageSrRroSubobjectParser()));
        regs.add(context.registerRROSubobjectParser(AddLspSrRroSubobjectParser.TYPE, new AddLspSrRroSubobjectParser()));
        regs.add(context.registerRROSubobjectParser(UpdateLspSrRroSubobjectParser.TYPE, new UpdateLspSrRroSubobjectParser()));
        regs.add(context.registerRROSubobjectParser(ReportedLspSrRroSubobjectParser.TYPE, new ReportedLspSrRroSubobjectParser()));

        /* serializers for Ero-type and Rro-type */
        regs.add(context.registerRROSubobjectSerializer(SrRroType.class, new SrRroSubobjectParser()));

        /* Objects */
        final TlvRegistry tlvReg = context.getTlvHandlerRegistry();
        final VendorInformationTlvRegistry viTlvRegistry = context.getVendorInformationTlvRegistry();
        regs.add(context.registerObjectParser(PcepOpenObjectWithSpcTlvParser.CLASS,
                PcepOpenObjectWithSpcTlvParser.TYPE, new PcepOpenObjectWithSpcTlvParser(tlvReg, viTlvRegistry)));
        regs.add(context.registerObjectSerializer(Open.class, new PcepOpenObjectWithSpcTlvParser(tlvReg, viTlvRegistry)));

        return regs;
    }
}
