/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib;

import javax.annotation.concurrent.ThreadSafe;

import org.opendaylight.protocol.concepts.InitialListenerEvents;
import org.opendaylight.protocol.concepts.ListenerRegistration;

@ThreadSafe
public abstract class AbstractRIBChangeListener implements RIBEventListener {
	@Override
	synchronized public final void onRIBEvent(final RIBEvent event) {
		onRIBEventImpl(event);
	}

	abstract protected void onRIBEventImpl(final RIBEvent event);

	synchronized public final ListenerRegistration<RIBEventListener> register(final RIB rib) {
		InitialListenerEvents<RIBEventListener, RIBEvent> ile = rib.registerListener(this);
		for (RIBEvent e : ile.getEvents())
			onRIBEvent(e);

		return ile.getRegistration();
	}
}
