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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.KeepaliveMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.keepalive.message.KeepaliveMessageBuilder;

/**
 * Parser for {@link KeepaliveMessage}.
 */
public class PCEPKeepAliveMessageParser extends AbstractMessageParser {
    private static final KeepaliveMessage MESSAGE = new KeepaliveBuilder()
            .setKeepaliveMessage(new KeepaliveMessageBuilder().build()).build();
    public static final int TYPE = 2;

    public PCEPKeepAliveMessageParser(final ObjectRegistry registry) {
        super(registry);
    }

    @Override
    public void serializeMessage(final Message message, final ByteBuf out) {
        checkArgument(message instanceof KeepaliveMessage,
                "Wrong instance of Message. Passed instance of %s. Need KeepaliveMessage.", message.getClass());
        MessageUtil.formatMessage(TYPE, Unpooled.EMPTY_BUFFER, out);
    }

    @Override
    protected KeepaliveMessage validate(final Queue<Object> objects, final List<Message> errors)
            throws PCEPDeserializerException {
        if (objects != null && !objects.isEmpty()) {
            throw new PCEPDeserializerException("Keepalive message should not contain any objects.");
        }
        return MESSAGE;
    }
}
