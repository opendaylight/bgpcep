/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ClusterIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.ClusterIdentifier;

public class ClusterIdAttributeParserTest {
    private static final byte[] CLUSTER_ID_BYTES = {
        (byte) 0x80, (byte) 0x0A, (byte) 0x08,
        (byte) 0xC0, (byte) 0xA8, (byte) 0x1, (byte) 0x1, (byte) 0xC0, (byte) 0xA8, (byte) 0x1, (byte) 0x2
    };
    ClusterIdAttributeParser parser;

    @Before
    public void setUp() {
        this.parser = new ClusterIdAttributeParser();
    }

    @Test
    public void testParserAttribute() throws Exception {
        final List<ClusterIdentifier> list = new ArrayList<>();
        final Ipv4AddressNoZone ip1 = new Ipv4AddressNoZone("192.168.1.1");
        final Ipv4AddressNoZone ip2 = new Ipv4AddressNoZone("192.168.1.2");
        list.add(new ClusterIdentifier(ip1));
        list.add(new ClusterIdentifier(ip2));
        final Attributes clusterId = new AttributesBuilder().setClusterId(new ClusterIdBuilder().setCluster(list)
            .build()).build();


        final ByteBuf output = Unpooled.buffer();
        this.parser.serializeAttribute(clusterId, output);

        assertArrayEquals(CLUSTER_ID_BYTES, ByteArray.getAllBytes(output));

        AttributesBuilder clusterIdOutput = new AttributesBuilder();
        this.parser.parseAttribute(Unpooled.wrappedBuffer(ByteArray.cutBytes(CLUSTER_ID_BYTES, 3)), clusterIdOutput,
            null);
        assertEquals(clusterId, clusterIdOutput.build());
    }

    @Test
    public void testParseEmptyListAttribute() {
        final List<ClusterIdentifier> list = new ArrayList<>();
        final Attributes clusterId = new AttributesBuilder().setClusterId(new ClusterIdBuilder().setCluster(list)
            .build()).build();
        final ByteBuf output = Unpooled.buffer();
        this.parser.serializeAttribute(clusterId, output);
        assertEquals(Unpooled.buffer(), output);
    }

    @Test
    public void testParseEmptyAttribute() {
        final Attributes clusterId = new AttributesBuilder().setClusterId(new ClusterIdBuilder().build()).build();
        final ByteBuf output = Unpooled.buffer();
        this.parser.serializeAttribute(clusterId, output);
        assertEquals(Unpooled.buffer(), output);
    }
}