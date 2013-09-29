/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.mock;

import java.util.List;
import java.util.Set;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.protocol.bgp.parser.BGPParameter;
import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.parser.BGPTableType;
import org.opendaylight.protocol.bgp.parser.message.BGPOpenMessage;
import org.opendaylight.protocol.bgp.parser.parameter.MultiprotocolCapability;
import org.opendaylight.protocol.concepts.ListenerRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Keepalive;
import org.opendaylight.yangtools.yang.binding.Notification;

import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * This class has @Subscribe annotated methods which receive events from {@link EventBus} . Events are produced by
 * {@link BGPMock}, and each instance notifies exactly one {@link BGPSessionListener}.
 */
class EventBusRegistration implements ListenerRegistration<BGPSessionListener> {
	private final EventBus eventBus;
	private final BGPSessionListener listener;
	@GuardedBy("this")
	private boolean closed = false;

	public static EventBusRegistration createAndRegister(final EventBus eventBus, final BGPSessionListener listener,
			final List<Notification> allPreviousMessages) {
		final EventBusRegistration instance = new EventBusRegistration(eventBus, listener, allPreviousMessages);
		eventBus.register(instance);
		return instance;
	}

	private EventBusRegistration(final EventBus eventBus, final BGPSessionListener listener, final List<Notification> allPreviousMessages) {
		this.eventBus = eventBus;
		this.listener = listener;
		for (final Notification message : allPreviousMessages) {
			sendMessage(listener, message);
		}
	}

	@Subscribe
	public void onMessage(final Notification message) {
		sendMessage(this.listener, message);
	}

	@Override
	public synchronized void close() {
		if (this.closed) {
			return;
		}
		this.eventBus.unregister(this);
		this.closed = true;
	}

	private static void sendMessage(final BGPSessionListener listener, final Notification message) {
		if (BGPMock.connectionLostMagicMessage.equals(message)) {
			listener.onSessionTerminated(null, null);
		} else if (message instanceof BGPOpenMessage) {
			final Set<BGPTableType> tts = Sets.newHashSet();
			for (final BGPParameter param : ((BGPOpenMessage) message).getOptParams()) {
				if (param instanceof MultiprotocolCapability) {
					tts.add(((MultiprotocolCapability) param).getTableType());
				}
			}

			listener.onSessionUp(new BGPSession() {

				@Override
				public void close() {
					// TODO Auto-generated method stub

				}

				@Override
				public Set<BGPTableType> getAdvertisedTableTypes() {
					return tts;
				}
			});
		} else if (message instanceof Keepalive) {
			// do nothing
		} else {
			listener.onMessage(null, message);
		}
	}

	@Override
	public BGPSessionListener getListener() {
		return this.listener;
	}
}
