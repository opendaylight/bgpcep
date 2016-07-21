/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.ClusterIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;

public class ClusterIdAttributeParserTest {
    private static final byte[] CLUSTER_ID_BYTES = {(byte) 0x80, (byte) 0x0A, (byte) 0x08,
        (byte) 0xC0, (byte) 0xA8, (byte) 0x1, (byte) 0x1,
        (byte) 0xC0, (byte) 0xA8, (byte) 0x1, (byte) 0x2};
    private static final ClusterIdAttributeParser PARSER = new ClusterIdAttributeParser();

    @Test
    public void testParserAttribute() throws Exception {
        final List<ClusterIdentifier> list = Lists.newArrayList();
        final Ipv4Address ip1 = new Ipv4Address("192.168.1.1");
        final Ipv4Address ip2 = new Ipv4Address("192.168.1.2");
        list.add(new ClusterIdentifier(ip1));
        list.add(new ClusterIdentifier(ip2));
        final Attributes clusterId = new AttributesBuilder().setClusterId(new ClusterIdBuilder().setCluster
            (list).build()).build();


        final ByteBuf output = Unpooled.buffer();
        PARSER.serializeAttribute(clusterId, output);

        Assert.assertArrayEquals(CLUSTER_ID_BYTES, ByteArray.getAllBytes(output));

        final AttributesBuilder clusterIdOutput = new AttributesBuilder();
        PARSER.parseAttribute(Unpooled.wrappedBuffer(ByteArray.cutBytes(CLUSTER_ID_BYTES, 3)), clusterIdOutput);
        assertEquals(clusterId, clusterIdOutput.build());
    }

    @Test
    public void testParseEmptyListAttribute() {
        final List<ClusterIdentifier> list = Lists.newArrayList();
        final Attributes clusterId = new AttributesBuilder().setClusterId(new ClusterIdBuilder().setCluster
            (list).build()).build();
        final ByteBuf output = Unpooled.buffer();
        PARSER.serializeAttribute(clusterId, output);
        assertEquals(Unpooled.buffer(), output);
    }

    @Test
    public void testParseEmptyAttribute() {
        final Attributes clusterId = new AttributesBuilder().setClusterId(new ClusterIdBuilder().build()).build();
        final ByteBuf output = Unpooled.buffer();
        PARSER.serializeAttribute(clusterId, output);
        assertEquals(Unpooled.buffer(), output);
    }
}