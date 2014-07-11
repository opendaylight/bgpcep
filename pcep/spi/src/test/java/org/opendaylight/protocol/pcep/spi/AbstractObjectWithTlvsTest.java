/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OfId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.list.tlv.OfList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.list.tlv.OfListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

public class AbstractObjectWithTlvsTest {

    private Tlv tlv;

    @Mock
    private TlvRegistry tlvRegistry;

    private class Abs extends AbstractObjectWithTlvsParser<TlvsBuilder> {

        protected Abs(TlvRegistry tlvReg) {
            super(tlvReg);
        }

        @Override
        public Object parseObject(ObjectHeader header, ByteBuf buffer) throws PCEPDeserializerException {
            return null;
        }

        @Override
        public void serializeObject(Object object, ByteBuf buffer) {
        }

        @Override
        public void addTlv(final TlvsBuilder builder, final Tlv tlv) {
            builder.setOfList((OfList) tlv);
        }
    };

    @Before
    public void setUp() throws PCEPDeserializerException {
        MockitoAnnotations.initMocks(this);
        this.tlv = new OfListBuilder().setCodes(Lists.newArrayList(new OfId(10))).build();
        Mockito.doNothing().when(this.tlvRegistry).serializeTlv(Mockito.any(Tlv.class), Mockito.any(ByteBuf.class));
        Mockito.doReturn(this.tlv).when(this.tlvRegistry).parseTlv(4, Unpooled.wrappedBuffer(new byte[] { 5, 6 }));
    }

    @Test
    public void testParseTlvs() throws PCEPDeserializerException {
        Abs a = new Abs(this.tlvRegistry);
        ByteBuf buffer = Unpooled.buffer();
        a.serializeTlv(this.tlv, buffer);

        Mockito.verify(this.tlvRegistry, Mockito.only()).serializeTlv(Mockito.any(Tlv.class), Mockito.any(ByteBuf.class));;

        TlvsBuilder b = new TlvsBuilder();
        a.parseTlvs(b, Unpooled.wrappedBuffer(new byte[] { 0, 4, 0, 2, 5, 6, 0, 0 }));

        assertEquals(this.tlv, b.getOfList());
    }
}
