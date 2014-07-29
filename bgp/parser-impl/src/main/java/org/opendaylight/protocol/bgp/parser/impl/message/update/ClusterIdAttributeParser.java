/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.ClusterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.ClusterIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.yang.binding.DataObject;

public final class ClusterIdAttributeParser implements AttributeParser, AttributeSerializer {

    public static final int TYPE = 10;

    @Override
    public void parseAttribute(final ByteBuf buffer, final PathAttributesBuilder builder) {
        final List<ClusterIdentifier> list = Lists.newArrayList();
        while (buffer.isReadable()) {
            list.add(new ClusterIdentifier(Ipv4Util.addressForByteBuf(buffer)));
        }
        builder.setClusterId(new ClusterIdBuilder().setCluster(list).build());
    }

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof PathAttributes, "Attribute parameter is not a PathAttribute object.");
        final ClusterId cid = ((PathAttributes) attribute).getClusterId();
        if (cid == null) {
            return;
        }
        final ByteBuf clusterIdBuffer = Unpooled.buffer();
        for (final ClusterIdentifier clusterIdentifier : cid.getCluster()) {
            clusterIdBuffer.writeBytes(Ipv4Util.bytesForAddress(clusterIdentifier));
        }
        AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL, TYPE, clusterIdBuffer, byteAggregator);
    }
}
