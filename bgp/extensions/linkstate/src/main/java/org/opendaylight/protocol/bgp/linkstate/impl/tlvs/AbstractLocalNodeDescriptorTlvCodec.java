/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate.impl.tlvs;

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.protocol.bgp.linkstate.spi.LinkstateTlvParser;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.link._case.LocalNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.link._case.LocalNodeDescriptorsBuilder;
import org.opendaylight.yangtools.yang.common.QName;


public abstract class AbstractLocalNodeDescriptorTlvCodec<T extends NodeIdentifier> extends AbstractNodeDescriptorTlvCodec implements
    LinkstateTlvParser<LocalNodeDescriptors>, LinkstateTlvParser.LinkstateTlvSerializer<T> {

    private static final int LOCAL_NODE_DESCRIPTORS_TYPE = 256;

    @Override
    public final LocalNodeDescriptors parseTlvBody(final ByteBuf value) {
        final Map<QName, Object> parsedSubTlvs = new HashMap<>();
        final LocalNodeDescriptorsBuilder builder = new LocalNodeDescriptorsBuilder(parseNodeDescriptor(value, parsedSubTlvs));
        builder.setBgpRouterId((Ipv4Address) parsedSubTlvs.get(BgpRouterIdTlvParser.BGP_ROUTER_ID_QNAME));
        builder.setMemberAsn((AsNumber) parsedSubTlvs.get(MemAsNumTlvParser.MEMBER_AS_NUMBER_QNAME));
        return builder.build();
    }

    @Override
    public final int getType() {
        return LOCAL_NODE_DESCRIPTORS_TYPE;
    }

}
