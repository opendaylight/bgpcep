/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import java.util.EventListener;
import java.util.List;

public final class InitialListenerEvents<L extends EventListener, E> {
	private final ListenerRegistration<L> registration;
	private final List<E> events;

	public InitialListenerEvents(final ListenerRegistration<L> registration, final List<E> events) {
		super();
		if (registration == null)
			throw new NullPointerException("Registration is mandatory!");
		this.registration = registration;
		if (events == null)
			throw new NullPointerException("Events are mandatory!");
		this.events = events;
	}

	/**
	 * @return the registration
	 */
	public ListenerRegistration<L> getRegistration() {
		return this.registration;
	}

	/**
	 * @return the events
	 */
	public List<E> getEvents() {
		return this.events;
	}
}
