/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.mock;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.parser.impl.BGPMessageFactoryImpl;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.rib.impl.BGP;
import org.opendaylight.protocol.concepts.ListenerRegistration;
import org.opendaylight.protocol.framework.DeserializerException;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.protocol.framework.ProtocolMessageFactory;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.NotifyBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

/**
 * 
 * Mock implementation of {@link BGP}.
 * 
 */
@ThreadSafe
public final class BGPMock implements BGP, Closeable {

	private final static Logger logger = LoggerFactory.getLogger(BGPMock.class);

	static final Notification connectionLostMagicMessage = new NotifyBuilder().setErrorCode(BGPError.CEASE.getCode()).build();

	@GuardedBy("this")
	private final List<byte[]> allPreviousByteMessages;
	private final List<Notification> allPreviousBGPMessages;
	private final EventBus eventBus;

	@GuardedBy("this")
	private final List<EventBusRegistration> openRegistrations = Lists.newLinkedList();

	public BGPMock(final EventBus eventBus, final MessageRegistry registry, final List<byte[]> bgpMessages) {
		this.allPreviousByteMessages = Lists.newLinkedList(bgpMessages);
		this.eventBus = eventBus;
		this.allPreviousBGPMessages = this.parsePrevious(registry, this.allPreviousByteMessages);
	}

	private List<Notification> parsePrevious(final MessageRegistry registry, final List<byte[]> msgs) {
		final List<Notification> messages = Lists.newArrayList();
		final ProtocolMessageFactory<Notification> parser = new BGPMessageFactoryImpl(registry);
		try {
			for (final byte[] b : msgs) {

				final byte[] body = ByteArray.cutBytes(b, 1);

				messages.add(parser.parse(body));
			}
		} catch (final DeserializerException e) {
			logger.warn(e.getMessage(), e);
		} catch (final DocumentedException e) {
			logger.warn(e.getMessage(), e);
		}
		return messages;
	}

	/**
	 * @param listener BGPListener
	 * @return ListenerRegistration
	 */
	@Override
	public synchronized ListenerRegistration<BGPSessionListener> registerUpdateListener(final BGPSessionListener listener,
			final ReconnectStrategy strategy) {
		return EventBusRegistration.createAndRegister(this.eventBus, listener, this.allPreviousBGPMessages);
	}

	public synchronized void insertConnectionLostEvent() {
		this.insertMessage(connectionLostMagicMessage);
	}

	public synchronized void insertMessages(final List<Notification> messages) {
		for (final Notification message : messages) {
			this.insertMessage(message);
		}
	}

	private synchronized void insertMessage(final Notification message) {
		this.allPreviousBGPMessages.add(message);
		this.eventBus.post(message);
	}

	@Override
	public synchronized void close() {
		// unregister all EventBusRegistration instances
		for (final EventBusRegistration registration : this.openRegistrations) {
			registration.close();
		}
		this.openRegistrations.clear();
	}

	public boolean isMessageListSame(final List<byte[]> newMessages) {
		if (this.allPreviousBGPMessages.size() != newMessages.size()) {
			return false;
		}
		final Iterator<byte[]> i1 = this.allPreviousByteMessages.iterator();
		final Iterator<byte[]> i2 = this.allPreviousByteMessages.iterator();
		for (int i = 0; i < this.allPreviousBGPMessages.size(); i++) {
			if (!Arrays.equals(i1.next(), i2.next())) {
				return false;
			}
		}
		return true;
	}

	public EventBus getEventBus() {
		return this.eventBus;
	}
}
