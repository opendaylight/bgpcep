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
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationObjectRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.AbstractPCEPExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Pcinitiate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcrep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.add.lsp.input.arguments.ero.subobject.subobject.type.SrEroType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.sr.pce.capability.tlv.SrPceCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.setup.type.tlv.PathSetupType;

public class SegmentRoutingActivator extends AbstractPCEPExtensionProviderActivator {

    @Override
    protected List<AutoCloseable> startImpl(PCEPExtensionProviderContext context) {
        final List<AutoCloseable> regs = Lists.newArrayList();

        /* Tlvs */
        regs.add(context.registerTlvParser(SrPceCapabilityTlvParser.TYPE, new SrPceCapabilityTlvParser()));
        regs.add(context.registerTlvParser(SrPathSetupTypeTlvParser.TYPE, new SrPathSetupTypeTlvParser()));

        regs.add(context.registerTlvSerializer(PathSetupType.class, new SrPathSetupTypeTlvParser()));
        regs.add(context.registerTlvSerializer(SrPceCapability.class, new SrPceCapabilityTlvParser()));

        /* Subobjects */
        regs.add(context.registerEROSubobjectParser(SrEroSubobjectParser.TYPE, new SrEroSubobjectParser()));
        regs.add(context.registerEROSubobjectSerializer(SrEroType.class, new SrEroSubobjectParser()));

        /* Objects */
        final TlvRegistry tlvReg = context.getTlvHandlerRegistry();
        final VendorInformationTlvRegistry viTlvRegistry = context.getVendorInformationTlvRegistry();
        regs.add(context.registerObjectParser(PcepOpenObjectWithSpcTlvParser.CLASS,
                PcepOpenObjectWithSpcTlvParser.TYPE, new PcepOpenObjectWithSpcTlvParser(tlvReg, viTlvRegistry)));

        regs.add(context.registerObjectSerializer(Open.class, new PcepOpenObjectWithSpcTlvParser(tlvReg, viTlvRegistry)));

        /* Messages */
        final ObjectRegistry objRegistry = context.getObjectHandlerRegistry();
        final VendorInformationObjectRegistry objReg = context.getVendorInformationObjectRegistry();
        regs.add(context.registerMessageParser(SrPcRepMessageParser.TYPE, new SrPcRepMessageParser(objRegistry, objReg)));
        regs.add(context.registerMessageParser(SrPcInitiateMessageParser.TYPE, new SrPcInitiateMessageParser(
                objRegistry)));
        regs.add(context.registerMessageParser(SrPcRptMessageParser.TYPE, new SrPcRptMessageParser(objRegistry)));
        regs.add(context.registerMessageParser(SrPcUpdMessageParser.TYPE, new SrPcUpdMessageParser(objRegistry)));

        regs.add(context.registerMessageSerializer(Pcrep.class, new SrPcRepMessageParser(objRegistry, objReg)));
        regs.add(context.registerMessageSerializer(Pcinitiate.class, new SrPcInitiateMessageParser(objRegistry)));
        regs.add(context.registerMessageSerializer(Pcrpt.class, new SrPcRptMessageParser(objRegistry)));
        regs.add(context.registerMessageSerializer(Pcupd.class, new SrPcUpdMessageParser(objRegistry)));

        return regs;
    }
}
