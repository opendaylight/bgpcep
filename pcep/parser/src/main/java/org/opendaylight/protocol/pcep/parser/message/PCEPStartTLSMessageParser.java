/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.StartTlsMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.StarttlsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.start.tls.message.StartTlsMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.Object;

public class PCEPStartTLSMessageParser extends AbstractMessageParser {

    // TODO: temporary value, to be assigned by IANA
    public static final int TYPE = 20;

    public PCEPStartTLSMessageParser(final ObjectRegistry registry) {
        super(registry);
    }

    @Override
    public void serializeMessage(final Message message, final ByteBuf out) {
        checkArgument(message instanceof StartTlsMessage,
                "Wrong instance of Message. Passed instance of %s. Need StartTlsMessage.", message.getClass());
        MessageUtil.formatMessage(TYPE, Unpooled.EMPTY_BUFFER, out);
    }

    @Override
    protected StartTlsMessage validate(final Queue<Object> objects, final List<Message> errors)
            throws PCEPDeserializerException {
        if (objects != null && !objects.isEmpty()) {
            throw new PCEPDeserializerException("StartTLS message should not contain any objects.");
        }

        return new StarttlsBuilder().setStartTlsMessage(new StartTlsMessageBuilder().build()).build();
    }
}
