/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.pcc.mock.protocol;

import static java.util.Objects.requireNonNull;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.pcep.PCEPPeerProposal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Tlvs3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

public class PCCServerPeerProposal implements PCEPPeerProposal {
    private boolean isAfterReconnection;
    private final BigInteger dbVersion;

    public PCCServerPeerProposal(@Nonnull final BigInteger dbVersion) {
        this.dbVersion = dbVersion;
    }

    @Override
    public void setPeerSpecificProposal(@Nonnull final InetSocketAddress address,
            @Nonnull final TlvsBuilder openBuilder) {
        requireNonNull(address);
        final LspDbVersionBuilder lspDbVersionBuilder = new LspDbVersionBuilder();
        if (this.isAfterReconnection) {
            lspDbVersionBuilder.setLspDbVersionValue(this.dbVersion);
        } else {
            this.isAfterReconnection = true;
        }
        openBuilder.addAugmentation(Tlvs3.class, new Tlvs3Builder()
                .setLspDbVersion(lspDbVersionBuilder.build()).build());
    }
}