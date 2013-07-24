/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

/**
 * Interface marking object which hold some state, which can be captured and saved. Capturing this state is useful for
 * various entities which need to see a consistent, point-in-time view of the state.
 * 
 * @param <T> Type reference of the returned state object
 */
public interface Stateful<T extends State> {
	/**
	 * Return a reference to the current state of the object. The returned object must remain immutable throughout its
	 * lifetime. Furthermore using equals() on two state objects returned from this method must return true if and only
	 * if the internal state of this object the the two points was equivalent.
	 * 
	 * @return Reference to a point-in-time consistent state of the object.
	 */
	public T currentState();
}
