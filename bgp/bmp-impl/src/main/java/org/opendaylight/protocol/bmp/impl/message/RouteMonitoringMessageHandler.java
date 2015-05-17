/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl.message;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bmp.spi.parser.AbstractBmpPerPeerMessageParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.RouteMonitoringMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.RouteMonitoringMessageBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * Created by cgasparini on 13.5.2015.
 */
public class RouteMonitoringMessageHandler extends AbstractBmpPerPeerMessageParser {

    private static final int MESSAGE_TYPE = 0;

    public RouteMonitoringMessageHandler(MessageRegistry bgpMssageRegistry) {
        super(bgpMssageRegistry);
    }

    @Override
    public void serializeMessageBody(final Notification message, final ByteBuf buffer) {
        //FIXME fix message
        Preconditions.checkArgument(message instanceof RouteMonitoringMessage, "BMP Notification message cannot be null");
        final RouteMonitoringMessage routeMonitor = (RouteMonitoringMessage) message;
        this.getBmpMessageRegistry().serializeMessage(new UpdateBuilder(routeMonitor.getUpdate()).build(), buffer);
    }

    @Override
    public Notification parseMessageBody(final ByteBuf bytes) {
        final RouteMonitoringMessageBuilder routeMonitor = new RouteMonitoringMessageBuilder().setPeerHeader(parsePerPeerHeader(bytes));
        try {
            final UpdateMessage updateMessage = (UpdateMessage) this.getBmpMessageRegistry().parseMessage(bytes);
            Preconditions.checkNotNull(updateMessage, "updateMessage may not be null");
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.route.monitoring
                .message.Update update = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message
                .rev150512.route.monitoring.message.UpdateBuilder(updateMessage).build();
            routeMonitor.setUpdate(update);
        } catch (final BGPDocumentedException | BGPParsingException e) {
            throw new IllegalStateException("Error on Parse notification message", e);
        }
        return routeMonitor.build();
    }

    @Override
    public int getBmpMessageType() {
        return MESSAGE_TYPE;
    }
}
