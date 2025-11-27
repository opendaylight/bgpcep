/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.message;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Queue;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.MessageUtil;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Close;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.CloseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.CloseMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.close.message.CCloseMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.close.message.CCloseMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.close.object.CClose;

/**
 * Parser for {@link CloseMessage}.
 */
public class PCEPCloseMessageParser extends AbstractMessageParser {

    public static final int TYPE = 7;

    public PCEPCloseMessageParser(final ObjectRegistry registry) {
        super(registry);
    }

    @Override
    public void serializeMessage(final Message message, final ByteBuf out) {
        checkArgument(message instanceof CloseMessage,
                "Wrong instance of Message. Passed instance of %s. Need CloseMessage.", message.getClass());
        final CCloseMessage close = ((CloseMessage) message).getCCloseMessage();
        checkArgument(close.getCClose() != null, "Close Object must be present in Close Message.");
        final ByteBuf buffer = Unpooled.buffer();
        serializeObject(close.getCClose(), buffer);
        MessageUtil.formatMessage(TYPE, buffer, out);
    }

    @Override
    protected Close validate(final Queue<Object> objects, final List<Message> errors) throws PCEPDeserializerException {
        checkArgument(objects != null, "Passed list can't be null.");
        final Object o = objects.poll();
        if (!(o instanceof CClose)) {
            throw new PCEPDeserializerException("Close message doesn't contain CLOSE object.");
        }
        final CCloseMessage msg = new CCloseMessageBuilder().setCClose((CClose) o).build();
        if (!objects.isEmpty()) {
            throw new PCEPDeserializerException("Unprocessed Objects: " + objects);
        }
        return new CloseBuilder().setCCloseMessage(msg).build();
    }
}
