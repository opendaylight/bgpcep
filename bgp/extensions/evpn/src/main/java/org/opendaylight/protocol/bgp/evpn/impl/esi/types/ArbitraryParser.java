/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.esi.types;

import static com.google.common.base.Preconditions.checkArgument;
import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.EsiModelUtil.extractArbitrary;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.EsiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.esi.Esi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.esi.esi.ArbitraryCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.esi.esi.ArbitraryCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.esi.esi.arbitrary._case.ArbitraryBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class ArbitraryParser extends AbstractEsiType {
    static final int ARBITRARY_LENGTH = 9;

    @Override
    public ByteBuf serializeBody(final Esi esiCase, final ByteBuf body) {
        checkArgument(esiCase instanceof ArbitraryCase, "Unknown esi instance. Passed %s. Needed ArbitraryCase.",
            esiCase);
        return body.writeBytes(((ArbitraryCase) esiCase).getArbitrary().getArbitrary());
    }

    @Override
    protected EsiType getType() {
        return EsiType.Arbitrary;
    }

    @Override
    public Esi serializeEsi(final ContainerNode esi) {
        return new ArbitraryCaseBuilder().setArbitrary(extractArbitrary(esi)).build();
    }

    @Override
    public Esi parseEsi(final ByteBuf body) {
        return new ArbitraryCaseBuilder().setArbitrary(new ArbitraryBuilder()
                .setArbitrary(ByteArray.readBytes(body, ARBITRARY_LENGTH)).build()).build();
    }
}
