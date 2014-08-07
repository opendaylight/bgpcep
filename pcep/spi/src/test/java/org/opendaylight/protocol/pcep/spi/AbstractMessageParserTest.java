/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import static org.junit.Assert.assertEquals;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;

public class AbstractMessageParserTest {

    private Object object;

    @Mock
    private ObjectRegistry registry;

    private class Abs extends AbstractMessageParser {

        protected Abs(ObjectRegistry registry) {
            super(registry);
        }

        @Override
        public void serializeMessage(Message message, ByteBuf buffer) {
        }

        @Override
        protected Message validate(List<Object> objects, List<Message> errors) throws PCEPDeserializerException {
            final short errorType = ((ErrorObject) objects.get(0)).getType();
            final short errorValue = ((ErrorObject) objects.get(0)).getValue();
            return createErrorMsg(PCEPErrors.forValue(errorType, errorValue), Optional.<Rp>absent());
        }
    };

    @Before
    public void setUp() throws PCEPDeserializerException {
        MockitoAnnotations.initMocks(this);
        this.object = new ErrorObjectBuilder().setType((short) 1).setValue((short) 1).build();
        Mockito.doNothing().when(this.registry).serializeObject(Mockito.any(Object.class), Mockito.any(ByteBuf.class));
        Mockito.doReturn(this.object).when(this.registry).parseObject(13, 1, new ObjectHeaderImpl(true, true), Unpooled.wrappedBuffer(new byte[] { 0, 0, 1, 1 }));
    }

    @Test
    public void testParseObjects() throws PCEPDeserializerException {
        Abs a = new Abs(this.registry);
        ByteBuf buffer = Unpooled.buffer();
        a.serializeObject(this.object, buffer);

        Mockito.verify(this.registry, Mockito.only()).serializeObject(Mockito.any(Object.class), Mockito.any(ByteBuf.class));;

        Message b = a.parseMessage(Unpooled.wrappedBuffer(new byte[] {0x0D, 0x13, 0, 0x08, 0, 0, 1, 1 }), Collections.<Message> emptyList());

        assertEquals(this.object, ((Pcerr) b).getPcerrMessage().getErrors().get(0).getErrorObject());
    }
}
