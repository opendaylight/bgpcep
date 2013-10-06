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

import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.concepts.ListenerRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.open.bgp.parameters.c.parameters.CMultiprotocol;
import org.opendaylight.yangtools.yang.binding.Notification;

import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * This class has @Subscribe annotated methods which receive events from {@link EventBus} . Events are produced by
 * {@link BGPMock}, and each instance notifies exactly one {@link BGPSessionListener}.
 */
class EventBusRegistration extends ListenerRegistration<BGPSessionListener> {
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
		sendMessage(this.listener, message);
	}

	@Override
	public synchronized void removeRegistration() {
		this.eventBus.unregister(this);
	}

	private static void sendMessage(final BGPSessionListener listener, final Notification message) {
		if (BGPMock.connectionLostMagicMessage.equals(message)) {
			listener.onSessionTerminated(null, null);
		} else if (message instanceof Open) {
			final Set<BgpTableType> tts = Sets.newHashSet();
			for (final BgpParameters param : ((Open) message).getBgpParameters()) {
				if (param instanceof CParameters) {
					final CParameters p = (CParameters) param;
					final BgpTableType type = new BgpTableTypeImpl(((CMultiprotocol) p).getMultiprotocolCapability().getAfi(), ((CMultiprotocol) p).getMultiprotocolCapability().getSafi());
					tts.add(type);
				}
			}

			listener.onSessionUp(new BGPSession() {

				@Override
				public void close() {
					// TODO Auto-generated method stub

				}

				@Override
				public Set<BgpTableType> getAdvertisedTableTypes() {
					return tts;
				}
			});
		} else if (message instanceof Keepalive) {
			// do nothing
		} else {
			listener.onMessage(null, message);
		}
	}
}
