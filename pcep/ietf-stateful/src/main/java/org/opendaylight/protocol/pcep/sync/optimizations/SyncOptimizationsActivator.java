/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.sync.optimizations;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderActivator;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.speaker.entity.id.tlv.SpeakerEntityId;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Component;

@Singleton
@MetaInfServices
@Component(immediate = true)
public class SyncOptimizationsActivator implements PCEPExtensionProviderActivator {
    @Inject
    public SyncOptimizationsActivator() {
        // Exposed for DI
    }

    @Override
    public List<Registration> start(final PCEPExtensionProviderContext context) {
        final List<Registration> regs = new ArrayList<>();

        regs.add(context.registerTlvParser(LspDbVersionTlvParser.TYPE, new LspDbVersionTlvParser()));
        regs.add(context.registerTlvSerializer(LspDbVersion.class, new LspDbVersionTlvParser()));

        regs.add(context.registerTlvParser(SpeakerEntityIdTlvParser.TYPE, new SpeakerEntityIdTlvParser()));
        regs.add(context.registerTlvSerializer(SpeakerEntityId.class, new SpeakerEntityIdTlvParser()));

        return regs;
    }
}
