/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.segment.routing02;

import com.google.common.collect.Lists;
import java.util.List;
import org.opendaylight.protocol.pcep.lsp.setup.type01.CInitiated00SrpObjectWithPstTlvParser;
import org.opendaylight.protocol.pcep.lsp.setup.type01.PathSetupTypeTlvParser;
import org.opendaylight.protocol.pcep.lsp.setup.type01.PcepRpObjectWithPstTlvParser;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.AbstractPCEPExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Pcinitiate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.path.setup.type.tlv.PathSetupType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcrep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.add.lsp.input.arguments.ero.subobject.subobject.type.SrEroType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.sr.pce.capability.tlv.SrPceCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;

public class SegmentRoutingActivator extends AbstractPCEPExtensionProviderActivator {

    @Override
    protected List<AutoCloseable> startImpl(PCEPExtensionProviderContext context) {
        final List<AutoCloseable> regs = Lists.newArrayList();

        /* Tlvs */
        regs.add(context.registerTlvParser(SrPceCapabilityTlvParser.TYPE, new SrPceCapabilityTlvParser()));
        regs.add(context.registerTlvParser(PathSetupTypeTlvParser.TYPE, new PathSetupTypeTlvParser()));

        regs.add(context.registerTlvSerializer(PathSetupType.class, new PathSetupTypeTlvParser()));
        regs.add(context.registerTlvSerializer(SrPceCapability.class, new SrPceCapabilityTlvParser()));

        /* Subobjects */
        regs.add(context.registerEROSubobjectParser(SrEroSubobjectParser.TYPE, new SrEroSubobjectParser()));
        regs.add(context.registerEROSubobjectSerializer(SrEroType.class, new SrEroSubobjectParser()));

        /* Objects */
        final TlvRegistry tlvReg = context.getTlvHandlerRegistry();
        regs.add(context.registerObjectParser(CInitiated00SrpObjectWithPstTlvParser.CLASS, CInitiated00SrpObjectWithPstTlvParser.TYPE, new CInitiated00SrpObjectWithPstTlvParser(tlvReg)));
        regs.add(context.registerObjectParser(PcepRpObjectWithPstTlvParser.CLASS, PcepRpObjectWithPstTlvParser.TYPE, new PcepRpObjectWithPstTlvParser(tlvReg)));
        regs.add(context.registerObjectParser(PcepOpenObjectWithSpcTlvParser.CLASS, PcepOpenObjectWithSpcTlvParser.TYPE, new PcepOpenObjectWithSpcTlvParser(tlvReg)));

        regs.add(context.registerObjectSerializer(Srp.class, new CInitiated00SrpObjectWithPstTlvParser(tlvReg)));
        regs.add(context.registerObjectSerializer(Rp.class, new PcepRpObjectWithPstTlvParser(tlvReg)));
        regs.add(context.registerObjectSerializer(Open.class, new PcepOpenObjectWithSpcTlvParser(tlvReg)));

        /* Messages */
        final ObjectRegistry objRegistry = context.getObjectHandlerRegistry();
        regs.add(context.registerMessageParser(SrPcRepMessageParser.TYPE, new SrPcRepMessageParser(objRegistry)));
        regs.add(context.registerMessageParser(SrPcInitiateMessageParser.TYPE, new SrPcInitiateMessageParser(objRegistry)));
        regs.add(context.registerMessageParser(SrPcRptMessageParser.TYPE, new SrPcRptMessageParser(objRegistry)));
        regs.add(context.registerMessageParser(SrPcUpdMessageParser.TYPE, new SrPcUpdMessageParser(objRegistry)));

        regs.add(context.registerMessageSerializer(Pcrep.class, new SrPcRepMessageParser(objRegistry)));
        regs.add(context.registerMessageSerializer(Pcinitiate.class, new SrPcInitiateMessageParser(objRegistry)));
        regs.add(context.registerMessageSerializer(Pcrpt.class, new SrPcRptMessageParser(objRegistry)));
        regs.add(context.registerMessageSerializer(Pcupd.class, new SrPcUpdMessageParser(objRegistry)));

        return regs;
    }
}
