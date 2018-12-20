/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.p2mp.te.lsp;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.pojo.AbstractPCEPExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.p2mp.te.lsp.rev181109.p2mp.pce.capability.tlv.P2mpPceCapability;
import org.opendaylight.yangtools.concepts.Registration;

public final class Activator extends AbstractPCEPExtensionProviderActivator {
    @Override
    protected List<Registration> startImpl(final PCEPExtensionProviderContext context) {
        final List<Registration> regs = new ArrayList<>(2);

        final P2MPTeLspCapabilityParser p2mpCapabilityParser = new P2MPTeLspCapabilityParser();
        regs.add(context.registerTlvParser(P2MPTeLspCapabilityParser.TYPE, p2mpCapabilityParser));
        regs.add(context.registerTlvSerializer(P2mpPceCapability.class, p2mpCapabilityParser));
        return regs;
    }
}