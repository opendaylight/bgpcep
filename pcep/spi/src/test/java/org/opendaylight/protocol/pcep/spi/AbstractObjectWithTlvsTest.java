/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.OfId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.of.list.tlv.OfList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.of.list.tlv.OfListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.tlvs.VendorInformationTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.tlvs.VendorInformationTlvBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class AbstractObjectWithTlvsTest {

    private static final EnterpriseNumber EN = new EnterpriseNumber(Uint32.ZERO);

    private Tlv tlv;

    private VendorInformationTlv viTlv;

    @Mock
    private TlvRegistry tlvRegistry;

    @Mock
    private VendorInformationTlvRegistry viTlvRegistry;

    private static class Abs extends AbstractObjectWithTlvsParser<TlvsBuilder> {

        protected Abs(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
            super(tlvReg, viTlvReg, 0, 0);
        }

        @Override
        public Object parseObject(final ObjectHeader header, final ByteBuf buffer) {
            return null;
        }

        @Override
        public void serializeObject(final Object object, final ByteBuf buffer) {
        }

        @Override
        public void addTlv(final TlvsBuilder builder, final Tlv newTlv) {
            builder.setOfList((OfList) newTlv);
        }

        @Override
        protected void addVendorInformationTlvs(final TlvsBuilder builder, final List<VendorInformationTlv> tlvs) {
            builder.setVendorInformationTlv(tlvs);
        }
    }

    @Before
    public void setUp() throws PCEPDeserializerException {
        tlv = new OfListBuilder().setCodes(Set.of(new OfId(Uint16.TEN))).build();
        viTlv = new VendorInformationTlvBuilder().setEnterpriseNumber(EN).build();
        doNothing().when(viTlvRegistry).serializeVendorInformationTlv(any(VendorInformationTlv.class),
            any(ByteBuf.class));
        doReturn(Optional.of(viTlv)).when(viTlvRegistry).parseVendorInformationTlv(EN,
            Unpooled.wrappedBuffer(new byte[0]));
        doNothing().when(tlvRegistry).serializeTlv(any(Tlv.class), any(ByteBuf.class));
        doReturn(tlv).when(tlvRegistry).parseTlv(4, Unpooled.wrappedBuffer(new byte[] { 5, 6 }));
    }

    @Test
    public void testParseTlvs() throws PCEPDeserializerException {
        Abs abs = new Abs(tlvRegistry, viTlvRegistry);
        ByteBuf buffer = Unpooled.buffer();
        abs.serializeTlv(tlv, buffer);

        verify(tlvRegistry, only()).serializeTlv(any(Tlv.class), any(ByteBuf.class));

        TlvsBuilder builder = new TlvsBuilder();
        abs.parseTlvs(builder, Unpooled.wrappedBuffer(new byte[] { 0, 4, 0, 2, 5, 6, 0, 0 }));

        assertEquals(tlv, builder.getOfList());
    }

    @Test
    public void testParseVendorInformationTlv() throws PCEPDeserializerException {
        final Abs parser = new Abs(tlvRegistry, viTlvRegistry);
        final ByteBuf buffer = Unpooled.buffer();

        parser.serializeVendorInformationTlvs(Lists.newArrayList(viTlv), buffer);
        verify(viTlvRegistry, only()).serializeVendorInformationTlv(any(VendorInformationTlv.class),
            any(ByteBuf.class));

        final TlvsBuilder tlvsBuilder = new TlvsBuilder();
        parser.parseTlvs(tlvsBuilder, Unpooled.wrappedBuffer(
            new byte[] { 0x00, 0x07, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00 }));
        assertEquals(viTlv, tlvsBuilder.getVendorInformationTlv().get(0));
    }
}
