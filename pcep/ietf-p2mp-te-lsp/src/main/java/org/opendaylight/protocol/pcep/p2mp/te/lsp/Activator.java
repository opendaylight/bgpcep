/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.p2mp.te.lsp;

import java.util.List;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderActivator;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.p2mp.te.lsp.rev181109.p2mp.pce.capability.tlv.P2mpPceCapability;
import org.opendaylight.yangtools.concepts.Registration;

@MetaInfServices
public final class Activator implements PCEPExtensionProviderActivator {
    @Override
    public List<Registration> start(final PCEPExtensionProviderContext context) {
        final P2MPTeLspCapabilityParser p2mpCapabilityParser = new P2MPTeLspCapabilityParser();

        return List.of(
            context.registerTlvParser(P2MPTeLspCapabilityParser.TYPE, p2mpCapabilityParser),
            context.registerTlvSerializer(P2mpPceCapability.class, p2mpCapabilityParser));
    }
}