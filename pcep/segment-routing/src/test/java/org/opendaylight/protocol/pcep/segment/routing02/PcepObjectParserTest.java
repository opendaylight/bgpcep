/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.segment.routing02;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.spi.ObjectHeaderImpl;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.sr.pce.capability.tlv.SrPceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

public class PcepObjectParserTest {

    private static final byte[] openObjectBytes = { 0x01, 0x10, 0x00, 0x10, 0x20, 0x1e, 0x78, 0x01,
        /* sr-capability-tlv */
        0x0, 0x1a, 0x0, 0x4, 0x0, 0x0, 0x0, 0x1 };

    private TlvRegistry tlvRegistry;

    private SimplePCEPExtensionProviderContext ctx;
    private SegmentRoutingActivator act;

    @Before
    public void setUp() {
        this.ctx = new SimplePCEPExtensionProviderContext();
        this.act = new SegmentRoutingActivator();
        this.act.start(this.ctx);
        this.tlvRegistry = this.ctx.getTlvHandlerRegistry();
    }

    @Test
    public void testOpenObjectWithSpcTlv() throws PCEPDeserializerException {
        final PcepOpenObjectWithSpcTlvParser parser = new PcepOpenObjectWithSpcTlvParser(tlvRegistry);

        final OpenBuilder builder = new OpenBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        builder.setVersion(new ProtocolVersion((short) 1));
        builder.setKeepalive((short) 30);
        builder.setDeadTimer((short) 120);
        builder.setSessionId((short) 1);

        Tlvs1 tlv = new Tlvs1Builder().setSrPceCapability(new SrPceCapabilityBuilder().setMsd((short) 1).build())
                .build();
        builder.setTlvs(new TlvsBuilder().addAugmentation(Tlvs1.class, tlv).build());

        final ByteBuf result = Unpooled.wrappedBuffer(openObjectBytes);
        assertEquals(builder.build(),
                parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));
        assertArrayEquals(openObjectBytes, parser.serializeObject(builder.build()));
    }

}
