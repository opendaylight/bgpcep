/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.ClusterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.ClusterIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.ClusterIdentifier;

public final class ClusterIdAttributeParser implements AttributeParser, AttributeSerializer {

    public static final int TYPE = 10;

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder) {
        final List<ClusterIdentifier> list = Lists.newArrayList();
        while (buffer.isReadable()) {
            list.add(new ClusterIdentifier(Ipv4Util.addressForByteBuf(buffer)));
        }
        builder.setClusterId(new ClusterIdBuilder().setCluster(list).build());
    }

    @Override
    public void serializeAttribute(final Attributes pathAttributes, final ByteBuf byteAggregator) {
        final ClusterId cid = pathAttributes.getClusterId();
        if (cid == null) {
            return;
        }
        final List<ClusterIdentifier> cluster = cid.getCluster();
        if (cluster == null || cluster.isEmpty()) {
            return;
        }
        final ByteBuf clusterIdBuffer = Unpooled.buffer();
        for (final ClusterIdentifier clusterIdentifier : cid.getCluster()) {
            clusterIdBuffer.writeBytes(Ipv4Util.bytesForAddress(clusterIdentifier));
        }
        AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL, TYPE, clusterIdBuffer, byteAggregator);
    }
}
