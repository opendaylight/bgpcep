/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.CRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.IsisPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonode;

public final class IsisPseudoNodeCaseSerializer implements RouterIdTlvSerializer {

    @Override
    public void serializeRouterId(final CRouterIdentifier routerId, final ByteBuf buffer) {
        final IsisPseudonode isis = ((IsisPseudonodeCase) routerId).getIsisPseudonode();
        buffer.writeBytes(isis.getIsIsRouterIdentifier().getIsoSystemId().getValue());
        buffer.writeByte(((isis.getPsn() != null) ? isis.getPsn() : 0));
    }
}
