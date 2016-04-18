/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import com.google.common.primitives.UnsignedInteger;
import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.CRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.OspfPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.ospf.pseudonode._case.OspfPseudonode;

public final class OspfPseudoNodeCaseSerializer implements RouterIdTlvSerializer {

    @Override
    public void serializeRouterId(final CRouterIdentifier routerId, final ByteBuf buffer) {
        final OspfPseudonode node = ((OspfPseudonodeCase) routerId).getOspfPseudonode();
        buffer.writeInt(UnsignedInteger.valueOf(node.getOspfRouterId()).intValue());
        buffer.writeInt(UnsignedInteger.valueOf(node.getLanInterface().getValue()).intValue());
    }
}
