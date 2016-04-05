/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.esi.types;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.EsiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.Esi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.esi.ArbitraryCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.esi.ArbitraryCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.esi.arbitrary._case.Arbitrary;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.esi.arbitrary._case.ArbitraryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.Evpn;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class ArbitraryParser extends AbstractEsiType {
    public static final NodeIdentifier ARBITRARY_CASE_NID = new NodeIdentifier(ArbitraryCase.QNAME);
    static final NodeIdentifier ARBITRARY_NID = new NodeIdentifier(Arbitrary.QNAME);
    static final NodeIdentifier ARB_NID = NodeIdentifier.create(QName.create(Evpn.QNAME, "arbitrary").intern());
    private static final int ARBITRARY_LENGTH = 9;

    @Override
    public void serializeEsi(final Esi esiCase, final ByteBuf buffer) {
        Preconditions.checkArgument(esiCase instanceof ArbitraryCase, "Unknown esi instance. Passed %s. Needed ArbitraryCase.", esiCase.getClass());
        final ByteBuf body = Unpooled.buffer(ESI_TYPE_LENGTH);
        body.writeByte(EsiType.Arbitrary.getIntValue());
        body.writeBytes(((ArbitraryCase) esiCase).getArbitrary().getArbitrary());
        buffer.writeBytes(body);
    }

    @Override
    public Esi serializeEsi(final ContainerNode esi) {
        return new ArbitraryCaseBuilder().setArbitrary(extractArbitrary(esi)).build();
    }

    private Arbitrary extractArbitrary(final ContainerNode esi) {
        if (esi.getChild(ARB_NID).isPresent()) {
            return new ArbitraryBuilder().setArbitrary((byte[]) esi.getChild(ARB_NID).get().getValue()).build();
        }
        return null;
    }

    @Override
    public Esi parseEsi(final ByteBuf buffer) {
        return new ArbitraryCaseBuilder().setArbitrary(new ArbitraryBuilder().setArbitrary(buffer.readSlice(ARBITRARY_LENGTH).array()).build()).build();
    }
}
