/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.initiated;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderActivator;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Pcinitiate;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Component;

@Singleton
@MetaInfServices
@Component(immediate = true)
public final class InitiatedActivator implements PCEPExtensionProviderActivator {
    @Inject
    public InitiatedActivator() {
        // Exposed for DI
    }

    @Override
    public List<Registration> start(final PCEPExtensionProviderContext context) {

        return List.of(
            context.registerMessageParser(InitiatedPCInitiateMessageParser.TYPE,
                new InitiatedPCInitiateMessageParser(context.getObjectHandlerRegistry())),
            context.registerMessageSerializer(Pcinitiate.class,
                new InitiatedPCInitiateMessageParser(context.getObjectHandlerRegistry())));
    }
}
