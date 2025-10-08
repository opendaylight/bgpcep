/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Pcrep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.PcrepBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcrep.message.PcrepMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcrep.message.pcrep.message.RepliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.vendor.information.objects.VendorInformationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.vendor.information.objects.VendorInformationObjectBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

@ExtendWith(MockitoExtension.class)
class AbstractMessageParserTest {
    private static final class Abs extends AbstractMessageParser {
        Abs(final ObjectRegistry registry) {
            super(registry);
        }

        @Override
        public void serializeMessage(final Message message, final ByteBuf buffer) {
            // No-op
        }

        @Override
        protected Message validate(final Queue<Object> objects, final List<Message> errors) {
            return switch (objects.element()) {
                case VendorInformationObject obj ->
                    new PcrepBuilder()
                        .setPcrepMessage(new PcrepMessageBuilder()
                            .setReplies(List.of(new RepliesBuilder()
                                .setVendorInformationObject(addVendorInformationObjects(objects))
                                .build()))
                            .build())
                        .build();
                case ErrorObject obj ->
                    createErrorMsg(PCEPErrors.forValue(obj.getType(), obj.getValue()), Optional.empty());
                default -> null;
            };
        }
    }

    private static final EnterpriseNumber EN = new EnterpriseNumber(Uint32.ZERO);

    @Mock
    private ObjectRegistry registry;

    private Object object;
    private VendorInformationObject viObject;

    @BeforeEach
    void beforeEach() throws Exception {
        object = new ErrorObjectBuilder().setType(Uint8.ONE).setValue(Uint8.ONE).build();
        viObject = new VendorInformationObjectBuilder().setEnterpriseNumber(EN).build();
    }

    @Test
    void testParseObjects() throws Exception {
        doNothing().when(registry).serializeObject(any(Object.class), any(ByteBuf.class));
        doReturn(object).when(registry).parseObject(13, 1, new ObjectHeaderImpl(true, true),
            Unpooled.wrappedBuffer(new byte[] { 0, 0, 1, 1 }));

        final Abs a = new Abs(registry);
        final ByteBuf buffer = Unpooled.buffer();
        a.serializeObject(object, buffer);

        verify(registry, only()).serializeObject(any(Object.class), any(ByteBuf.class));

        final var parsed = a.parseMessage(Unpooled.wrappedBuffer(new byte[] {0x0D, 0x13, 0, 0x08, 0, 0, 1, 1 }),
            List.of());
        final var msg = assertInstanceOf(Pcerr.class, parsed);
        assertEquals(object, msg.getPcerrMessage().nonnullErrors().getFirst().getErrorObject());
    }

    @Test
    void testParseVendorInformationObject() throws Exception {
        doNothing().when(registry).serializeVendorInformationObject(any(VendorInformationObject.class),
            any(ByteBuf.class));
        doReturn(Optional.of(viObject)).when(registry).parseVendorInformationObject(eq(EN),
            eq(new ObjectHeaderImpl(true, true)), any(ByteBuf.class));

        final Abs parser = new Abs(registry);
        final ByteBuf buffer = Unpooled.buffer();

        parser.serializeVendorInformationObjects(List.of(viObject), buffer);
        verify(registry, only()).serializeVendorInformationObject(any(VendorInformationObject.class),
            any(ByteBuf.class));

        final var parsed = parser.parseMessage(
            Unpooled.wrappedBuffer(new byte[] { 0x22, 0x13, 0x00, 0x08, 0, 0, 0, 0 }), List.of());
        final var msg = assertInstanceOf(Pcrep.class, parsed);

        assertEquals(viObject,
            msg.getPcrepMessage().nonnullReplies().getFirst().nonnullVendorInformationObject().getFirst());
    }
}
