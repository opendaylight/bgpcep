package org.opendaylight.protocol.bgp.bmp.impl.message.message;/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bmp.spi.parser.AbstractBmpPerPeerMessageParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.RouteMonitoringMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.RouteMonitoringMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.header.PeerHeader;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cgasparini on 13.5.2015.
 */
public class RouteMonitoringMessageHandler extends AbstractBmpPerPeerMessageParser {
    private static final Logger LOG = LoggerFactory.getLogger(RouteMonitoringMessageHandler.class);

    public RouteMonitoringMessageHandler(MessageRegistry bgpMssageRegistry) {
        super(bgpMssageRegistry);
    }

    @Override
    public void serializeMessage(final Notification message, final ByteBuf buffer) {
        Preconditions.checkArgument(message instanceof RouteMonitoringMessage, "BMP Notification message cannot be null");
        final RouteMonitoringMessage routeMonitor = (RouteMonitoringMessage) message;
        this.serializePerPeerHeader(routeMonitor.getPeerHeader(), buffer);
        this.getBmpMessageRegistry().serializeMessage(new UpdateBuilder(routeMonitor.getUpdate()).build(),buffer);
    }

    @Override
    public Notification parseMessage(final ByteBuf bytes) {
        Preconditions.checkArgument(bytes != null && bytes.readableBytes() != 0, "Byte buffer cannot be null.");
        LOG.trace("Started parsing of notification (PeerUp) message: {}", ByteBufUtil.hexDump(bytes));
        final PeerHeader header = this.parsePerPeerHeader(bytes);
        RouteMonitoringMessageBuilder routeMonitor = new RouteMonitoringMessageBuilder().setPeerHeader(header);
        UpdateMessage updateMessage = null;
        try {
            updateMessage = (UpdateMessage) this.getBmpMessageRegistry().parseMessage(bytes);
        } catch (Exception e) {
            LOG.warn("Error on Parse notification message", bytes);
        }

        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.route.monitoring
            .message.Update update = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message
            .rev150512.route.monitoring.message.UpdateBuilder(updateMessage).build();
        routeMonitor.setUpdate(update);

        return routeMonitor.build();
    }
}
