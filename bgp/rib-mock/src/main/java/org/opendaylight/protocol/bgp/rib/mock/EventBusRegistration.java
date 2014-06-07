/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.mock;

import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.util.List;
import java.util.Set;

import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.MultiprotocolCase;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class has @Subscribe annotated methods which receive events from {@link EventBus} . Events are produced by
 * {@link BGPMock}, and each instance notifies exactly one {@link BGPSessionListener}.
 */
final class EventBusRegistration extends AbstractListenerRegistration<BGPSessionListener> {

    private static final Logger LOG = LoggerFactory.getLogger(EventBusRegistration.class);

    private final EventBus eventBus;

    public static EventBusRegistration createAndRegister(final EventBus eventBus, final BGPSessionListener listener,
            final List<Notification> allPreviousMessages) {
        final EventBusRegistration instance = new EventBusRegistration(eventBus, listener, allPreviousMessages);
        eventBus.register(instance);
        return instance;
    }

    private EventBusRegistration(final EventBus eventBus, final BGPSessionListener listener, final List<Notification> allPreviousMessages) {
        super(listener);
        this.eventBus = eventBus;
        for (final Notification message : allPreviousMessages) {
            sendMessage(listener, message);
        }
    }

    @Subscribe
    public void onMessage(final Notification message) {
        sendMessage(this.getInstance(), message);
    }

    @Override
    public synchronized void removeRegistration() {
        this.eventBus.unregister(this);
    }

    private static void sendMessage(final BGPSessionListener listener, final Notification message) {
        if (BGPMock.CONNECTION_LOST_MAGIC_MSG.equals(message)) {
            listener.onSessionTerminated(null, null);
        } else if (message instanceof Open) {
            final Set<BgpTableType> tts = Sets.newHashSet();
            for (final BgpParameters param : ((Open) message).getBgpParameters()) {
                if (param.getCParameters() instanceof MultiprotocolCase) {
                    final MultiprotocolCase p = (MultiprotocolCase) param.getCParameters();
                    LOG.debug("Adding open parameter {}", p);
                    final BgpTableType type = new BgpTableTypeImpl(p.getMultiprotocolCapability().getAfi(), p.getMultiprotocolCapability().getSafi());
                    tts.add(type);
                }
            }

            listener.onSessionUp(new BGPSession() {

                @Override
                public void close() {
                    LOG.debug("Session {} closed", this);
                }

                @Override
                public Set<BgpTableType> getAdvertisedTableTypes() {
                    return tts;
                }

                @Override
                public Ipv4Address getBgpId() {
                    return new Ipv4Address("127.0.0.1");
                }

                @Override
                public AsNumber getAsNumber() {
                    return new AsNumber(30L);
                }
            });
        } else if (!(message instanceof Keepalive)) {
            listener.onMessage(null, message);
        }
    }
}
