/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.mock;

import com.google.common.eventbus.EventBus;
import io.netty.buffer.Unpooled;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.NotifyBuilder;
import org.opendaylight.yangtools.binding.Notification;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock BGP session. It provides a way how to route a set of messages to BGPSessionListener. This class is thread-safe.
 */
public final class BGPMock implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(BGPMock.class);

    static final Notify CONNECTION_LOST_MAGIC_MSG = new NotifyBuilder().setErrorCode(BGPError.CEASE.getCode()).build();

    @GuardedBy("this")
    private final List<byte[]> allPreviousByteMessages;
    private final List<Notification<?>> allPreviousBGPMessages;
    private final EventBus eventBus;

    @GuardedBy("this")
    private final List<EventBusRegistration> openRegistrations = new ArrayList<>();

    public BGPMock(final EventBus eventBus, final MessageRegistry registry, final List<byte[]> bgpMessages) {
        allPreviousByteMessages = new ArrayList<>(bgpMessages);
        this.eventBus = eventBus;
        allPreviousBGPMessages = parsePrevious(registry, allPreviousByteMessages);
    }

    private static List<Notification<?>> parsePrevious(final MessageRegistry registry, final List<byte[]> msgs) {
        final List<Notification<?>> messages = new ArrayList<>();
        try {
            for (final byte[] b : msgs) {

                final byte[] body = ByteArray.cutBytes(b, 1);

                messages.add(registry.parseMessage(Unpooled.copiedBuffer(body), null));
            }
        } catch (final BGPDocumentedException | BGPParsingException e) {
            LOG.warn("Failed to parse message", e);
        }
        return messages;
    }

    @Override
    public synchronized void close() {
        // unregister all EventBusRegistration instances
        for (final EventBusRegistration registration : openRegistrations) {
            registration.close();
        }
        openRegistrations.clear();
    }

    public Registration registerUpdateListener(final BGPSessionListener listener) {
        return EventBusRegistration.createAndRegister(eventBus, listener, allPreviousBGPMessages);
    }
}
