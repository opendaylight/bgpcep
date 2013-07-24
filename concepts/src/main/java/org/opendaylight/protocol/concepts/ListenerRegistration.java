/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import java.io.Closeable;
import java.util.EventListener;

/**
 * An interface representing a listener registration. Objects offering
 * the ability to register listener should return an implementation of this
 * interface upon successful registration. The users are required to call
 * #close() before losing the reference to that object.
 *
 * @param <T> template reference to associated EventListener implementation
 */
public interface ListenerRegistration<T extends EventListener> extends Closeable {
	/**
	 * Access the listener object associated with this registration.
	 *
	 * @return Associated listener.
	 */
	public T getListener();

	@Override
	public void close();
}

