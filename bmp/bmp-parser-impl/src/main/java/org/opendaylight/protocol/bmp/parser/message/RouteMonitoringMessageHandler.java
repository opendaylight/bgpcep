/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.parser.message;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bmp.spi.parser.AbstractBmpPerPeerMessageParser;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.RouteMonitoringMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.RouteMonitoringMessageBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;

public class RouteMonitoringMessageHandler extends AbstractBmpPerPeerMessageParser<RouteMonitoringMessageBuilder> {

    private static final int MESSAGE_TYPE = 0;
    private final MessageRegistry msgRegistry;

    public RouteMonitoringMessageHandler(final MessageRegistry bgpMssageRegistry) {
        super(bgpMssageRegistry);
        this.msgRegistry = getBgpMessageRegistry();
    }

    @Override
    public void serializeMessageBody(final Notification message, final ByteBuf buffer) {
        super.serializeMessageBody(message, buffer);
        Preconditions.checkArgument(message instanceof RouteMonitoringMessage, "An instance of RouteMonitoringMessage is required");
        final RouteMonitoringMessage routeMonitor = (RouteMonitoringMessage) message;
        this.msgRegistry.serializeMessage(new UpdateBuilder(routeMonitor.getUpdate()).build(), buffer);
    }

    @Override
    public Notification parseMessageBody(final ByteBuf bytes) throws BmpDeserializationException {
        final RouteMonitoringMessageBuilder routeMonitor = new RouteMonitoringMessageBuilder().setPeerHeader(parsePerPeerHeader(bytes));
        try {
            final Notification message = this.msgRegistry.parseMessage(bytes, null);
            Preconditions.checkNotNull(message, "UpdateMessage may not be null");
            Preconditions.checkArgument(message instanceof UpdateMessage, "An instance of UpdateMessage is required");
            final UpdateMessage updateMessage = (UpdateMessage) message;
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.route.monitoring
                .message.Update update = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message
                .rev150512.route.monitoring.message.UpdateBuilder(updateMessage).build();
            routeMonitor.setUpdate(update);
        } catch (final BGPDocumentedException | BGPParsingException e) {
            throw new BmpDeserializationException("Error while parsing Update Message.", e);
        }

        return routeMonitor.build();
    }

    @Override
    public int getBmpMessageType() {
        return MESSAGE_TYPE;
    }
}
