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
import org.opendaylight.protocol.pcep.lsp.setup.type01.PathSetupTypeTlvParser;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.pojo.AbstractPCEPExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.path.setup.type.tlv.PathSetupType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.sr.pce.capability.tlv.SrPceCapability;

public class SegmentRoutingActivator extends AbstractPCEPExtensionProviderActivator {

    @Override
    protected List<AutoCloseable> startImpl(PCEPExtensionProviderContext context) {
        final List<AutoCloseable> regs = Lists.newArrayList();

        regs.add(context.registerTlvParser(SrPceCapabilityTlvParser.TYPE, new SrPceCapabilityTlvParser()));
        regs.add(context.registerTlvSerializer(SrPceCapability.class, new SrPceCapabilityTlvParser()));

        regs.add(context.registerTlvParser(PathSetupTypeTlvParser.TYPE, new PathSetupTypeTlvParser()));
        regs.add(context.registerTlvSerializer(PathSetupType.class, new PathSetupTypeTlvParser()));
        return regs;
    }
}
