/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.pcep.parser.tlv.P2MPTeLspCapabilityParser;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderActivator;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.p2mp.pce.capability.tlv.P2mpPceCapability;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Component;

@Singleton
@MetaInfServices
@Component(immediate = true)
public final class P2MPExtensionActivator implements PCEPExtensionProviderActivator {
    @Inject
    public P2MPExtensionActivator() {
        // Exposed for DI
    }

    @Override
    public List<Registration> start(final PCEPExtensionProviderContext context) {
        final var p2mpCapabilityParser = new P2MPTeLspCapabilityParser();

        return List.of(
            context.registerTlvParser(P2MPTeLspCapabilityParser.TYPE, p2mpCapabilityParser),
            context.registerTlvSerializer(P2mpPceCapability.class, p2mpCapabilityParser));
    }
}