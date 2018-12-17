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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Pcrep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.PcrepBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcrep.message.PcrepMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcrep.message.pcrep.message.RepliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.objects.VendorInformationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.objects.VendorInformationObjectBuilder;

@RunWith(MockitoJUnitRunner.class)
public class AbstractMessageParserTest {

    private static final EnterpriseNumber EN = new EnterpriseNumber(0L);

    private Object object;

    private VendorInformationObject viObject;

    @Mock
    private ObjectRegistry registry;

    private class Abs extends AbstractMessageParser {

        protected Abs(final ObjectRegistry registry) {
            super(registry);
        }

        @Override
        public void serializeMessage(final Message message, final ByteBuf buffer) {
        }

        @Override
        protected Message validate(final List<Object> objects, final List<Message> errors) {
            if (objects.get(0) instanceof VendorInformationObject) {
                final RepliesBuilder repsBuilder = new RepliesBuilder();
                repsBuilder.setVendorInformationObject(addVendorInformationObjects(objects));
                return new PcrepBuilder().setPcrepMessage(
                    new PcrepMessageBuilder().setReplies(Arrays.asList(repsBuilder.build())).build())
                        .build();
            } else if (objects.get(0) instanceof ErrorObject) {
                final short errorType = ((ErrorObject) objects.get(0)).getType();
                final short errorValue = ((ErrorObject) objects.get(0)).getValue();
                return createErrorMsg(PCEPErrors.forValue(errorType, errorValue), Optional.empty());
            }
            return null;
        }
    }

    @Before
    public void setUp() throws PCEPDeserializerException {
        this.object = new ErrorObjectBuilder().setType((short) 1).setValue((short) 1).build();
        this.viObject = new VendorInformationObjectBuilder().setEnterpriseNumber(EN).build();
        doNothing().when(this.registry).serializeVendorInformationObject(any(VendorInformationObject.class),
            any(ByteBuf.class));
        doReturn(Optional.of(this.viObject)).when(this.registry).parseVendorInformationObject(eq(EN),
            eq(new ObjectHeaderImpl(true, true)), any(ByteBuf.class));
        doNothing().when(this.registry).serializeObject(any(Object.class), any(ByteBuf.class));
        doReturn(this.object).when(this.registry).parseObject(13, 1, new ObjectHeaderImpl(true, true),
            Unpooled.wrappedBuffer(new byte[] { 0, 0, 1, 1 }));
    }

    @Test
    public void testParseObjects() throws PCEPDeserializerException {
        final Abs a = new Abs(this.registry);
        final ByteBuf buffer = Unpooled.buffer();
        a.serializeObject(this.object, buffer);

        verify(this.registry, only()).serializeObject(any(Object.class), any(ByteBuf.class));

        final Message b = a.parseMessage(Unpooled.wrappedBuffer(new byte[] {0x0D, 0x13, 0, 0x08, 0, 0, 1, 1 }),
            Collections.emptyList());

        assertEquals(this.object, ((Pcerr) b).getPcerrMessage().getErrors().get(0).getErrorObject());
    }

    @Test
    public void testParseVendorInformationObject() throws PCEPDeserializerException {
        final Abs parser = new Abs(this.registry);
        final ByteBuf buffer = Unpooled.buffer();

        parser.serializeVendorInformationObjects(Lists.newArrayList(this.viObject), buffer);
        verify(this.registry, only()).serializeVendorInformationObject(any(VendorInformationObject.class),
            any(ByteBuf.class));

        final Message msg = parser.parseMessage(
            Unpooled.wrappedBuffer(new byte[] { 0x22, 0x13, 0x00, 0x08, 0, 0, 0, 0 }), Collections.emptyList());

        assertEquals(this.viObject, ((Pcrep)msg).getPcrepMessage().getReplies().get(0).getVendorInformationObject()
            .get(0));
    }
}
