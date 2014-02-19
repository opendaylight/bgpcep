/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

public class SimpleListenerRegistration<K extends EventListener> extends ListenerRegistration<K> {

	public List<K> listeners = new ArrayList<>();

	protected SimpleListenerRegistration(K listener) {
		super(listener);
		this.listeners.add(listener);
	}

	@Override
	protected void removeRegistration() {
		this.listeners.clear();
	}
}
