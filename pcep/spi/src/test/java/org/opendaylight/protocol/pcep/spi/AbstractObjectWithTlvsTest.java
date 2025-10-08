/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.OfId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.of.list.tlv.OfList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.of.list.tlv.OfListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.vendor.information.tlvs.VendorInformationTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.vendor.information.tlvs.VendorInformationTlvBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;

@ExtendWith(MockitoExtension.class)
class AbstractObjectWithTlvsTest {
    private static final class Abs extends AbstractObjectWithTlvsParser<TlvsBuilder> {
        Abs(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
            super(tlvReg, viTlvReg, 0, 0);
        }

        @Override
        public Object parseObject(final ObjectHeader header, final ByteBuf buffer) {
            return null;
        }

        @Override
        public void serializeObject(final Object object, final ByteBuf buffer) {
            // No-op
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

    private static final EnterpriseNumber EN = new EnterpriseNumber(Uint32.ZERO);

    @Mock
    private TlvRegistry tlvRegistry;
    @Mock
    private VendorInformationTlvRegistry viTlvRegistry;

    @Test
    void testParseTlvs() throws Exception {
        final var tlv = new OfListBuilder().setCodes(Set.of(new OfId(Uint16.TEN))).build();

        doNothing().when(tlvRegistry).serializeTlv(any(), any());
        doReturn(tlv).when(tlvRegistry).parseTlv(4, Unpooled.wrappedBuffer(new byte[] { 5, 6 }));

        Abs abs = new Abs(tlvRegistry, viTlvRegistry);
        ByteBuf buffer = Unpooled.buffer();
        abs.serializeTlv(tlv, buffer);

        verify(tlvRegistry, only()).serializeTlv(any(), any());

        TlvsBuilder builder = new TlvsBuilder();
        abs.parseTlvs(builder, Unpooled.wrappedBuffer(new byte[] { 0, 4, 0, 2, 5, 6, 0, 0 }));

        assertEquals(tlv, builder.getOfList());
    }

    @Test
    void testParseVendorInformationTlv() throws Exception {
        final var viTlv = new VendorInformationTlvBuilder().setEnterpriseNumber(EN).build();

        doNothing().when(viTlvRegistry).serializeVendorInformationTlv(any(), any());
        doReturn(Optional.of(viTlv)).when(viTlvRegistry).parseVendorInformationTlv(EN, Unpooled.EMPTY_BUFFER);

        final Abs parser = new Abs(tlvRegistry, viTlvRegistry);
        final ByteBuf buffer = Unpooled.buffer();

        parser.serializeVendorInformationTlvs(List.of(viTlv), buffer);
        verify(viTlvRegistry, only()).serializeVendorInformationTlv(any(), any());

        final TlvsBuilder tlvsBuilder = new TlvsBuilder();
        parser.parseTlvs(tlvsBuilder, Unpooled.wrappedBuffer(
            new byte[] { 0x00, 0x07, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00 }));
        assertEquals(viTlv, tlvsBuilder.getVendorInformationTlv().get(0));
    }
}
