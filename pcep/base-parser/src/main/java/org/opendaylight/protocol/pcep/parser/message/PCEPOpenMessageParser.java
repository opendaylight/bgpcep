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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.open.message.OpenMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.open.object.Open;

/**
 * Parser for {@link OpenMessage}.
 */
public class PCEPOpenMessageParser extends AbstractMessageParser {

    public static final int TYPE = 1;

    public PCEPOpenMessageParser(final ObjectRegistry registry) {
        super(registry);
    }

    @Override
    public void serializeMessage(final Message message, final ByteBuf out) {
        checkArgument(message instanceof OpenMessage,
                "Wrong instance of Message. Passed instance of %s. Need OpenMessage.", message.getClass());
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.open.message
            .OpenMessage open = ((OpenMessage) message).getOpenMessage();
        checkArgument(open.getOpen() != null, "Open Object must be present in Open Message.");
        final ByteBuf buffer = Unpooled.buffer();
        serializeObject(open.getOpen(), buffer);
        MessageUtil.formatMessage(TYPE, buffer, out);
    }

    @Override
    protected org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Open validate(
            final Queue<Object> objects, final List<Message> errors) throws PCEPDeserializerException {
        checkArgument(objects != null, "Passed list can't be null.");

        final Object open = objects.poll();
        if (!(open instanceof Open)) {
            throw new PCEPDeserializerException("Open message doesn't contain OPEN object.");
        }
        if (!objects.isEmpty()) {
            throw new PCEPDeserializerException("Unprocessed Objects: " + objects);
        }

        return new OpenBuilder().setOpenMessage(new OpenMessageBuilder().setOpen((Open) open).build()).build();
    }
}
