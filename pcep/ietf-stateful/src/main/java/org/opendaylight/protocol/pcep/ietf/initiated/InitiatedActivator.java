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
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.Pcinitiate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev210825.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev210825.srp.object.Srp;
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
        final TlvRegistry tlvReg = context.getTlvHandlerRegistry();
        final VendorInformationTlvRegistry viTlvReg = context.getVendorInformationTlvRegistry();

        return List.of(
            context.registerMessageParser(InitiatedPCInitiateMessageParser.TYPE,
                new InitiatedPCInitiateMessageParser(context.getObjectHandlerRegistry())),
            context.registerMessageSerializer(Pcinitiate.class,
                new InitiatedPCInitiateMessageParser(context.getObjectHandlerRegistry())),

            context.registerObjectParser(new InitiatedLspObjectParser(tlvReg, viTlvReg)),
            context.registerObjectSerializer(Lsp.class, new InitiatedLspObjectParser(tlvReg, viTlvReg)),
            context.registerObjectParser(new InitiatedSrpObjectParser(tlvReg, viTlvReg)),
            context.registerObjectSerializer(Srp.class, new InitiatedSrpObjectParser(tlvReg, viTlvReg)),

            context.registerTlvParser(InitiatedStatefulCapabilityTlvParser.TYPE,
                new InitiatedStatefulCapabilityTlvParser()),
            context.registerTlvSerializer(Stateful.class, new InitiatedStatefulCapabilityTlvParser()));
    }
}
