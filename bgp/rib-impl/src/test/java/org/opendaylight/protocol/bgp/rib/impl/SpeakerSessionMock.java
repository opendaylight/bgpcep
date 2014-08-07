/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.mockito.Mockito.mock;

import io.netty.channel.Channel;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.OpenBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * Mock of the BGP speakers session.
 */
public class SpeakerSessionMock extends BGPSessionImpl {

    private final BGPSessionListener client;

    SpeakerSessionMock(final BGPSessionListener listener, final BGPSessionListener client) {
        super(listener, mock(Channel.class), new OpenBuilder().setHoldTimer(5).build(), 10);
        this.client = client;
    }

    @Override
    public void sendMessage(final Notification msg) {
        this.setLastMessageSentAt(System.nanoTime());
        this.client.onMessage(this, msg);
    }
}
