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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev140506.add.lsp.input.arguments.ero.subobject.subobject.type.SrEroType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev140506.add.lsp.input.arguments.rro.subobject.subobject.type.SrRroType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev140506.sr.pce.capability.tlv.SrPceCapability;
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
        regs.add(context.registerRROSubobjectParser(SrRroSubobjectParser.TYPE, new SrRroSubobjectParser()));
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
