/*
 * Copyright (c) 2024 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.nlri;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.linkstate.spi.AbstractNlriTypeCodec;
import org.opendaylight.protocol.bgp.linkstate.spi.pojo.SimpleNlriTypeRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.Srv6SidCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.Srv6SidCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.srv6.sid._case.Srv6NodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.srv6.sid._case.Srv6NodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.srv6.sid._case.Srv6SidDescriptors;

public final class Srv6SidNlriParser extends AbstractNlriTypeCodec {

    @Override
    protected ObjectType parseObjectType(final ByteBuf buffer) {
        final NodeIdentifier srv6Node = SimpleNlriTypeRegistry.getInstance().parseTlv(buffer);
        final Srv6SidDescriptors srv6SidDescriptors = SimpleNlriTypeRegistry.getInstance().parseTlv(buffer);
        return new Srv6SidCaseBuilder()
                .setSrv6NodeDescriptors(new Srv6NodeDescriptorsBuilder(srv6Node).build())
                .setSrv6SidDescriptors(srv6SidDescriptors)
                .build();
    }

    @Override
    protected void serializeObjectType(final ObjectType objectType, final ByteBuf buffer) {
        final Srv6SidCase srv6 = (Srv6SidCase) objectType;
        SimpleNlriTypeRegistry.getInstance().serializeTlv(Srv6NodeDescriptors.QNAME,
            srv6.getSrv6NodeDescriptors(), buffer);
        SimpleNlriTypeRegistry.getInstance().serializeTlv(Srv6SidDescriptors.QNAME,
                srv6.getSrv6SidDescriptors(), buffer);
    }

    @Override
    public int getNlriType() {
        return NlriType.Srv6Sid.getIntValue();
    }
}
