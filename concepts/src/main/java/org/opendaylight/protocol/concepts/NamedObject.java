/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

/**
 * General interface for identifyiable objects. Useful when you have an object
 * which has a sense of identity -- by having a "name".
 *
 * @param <T> template reference to the object name's Identifier class
 */
public interface NamedObject<T extends Identifier> {
	/**
	 * Get the object's Identifier (or "name"). A name uniquely identifies
	 * an object among its peers. Two named objects can have the same
	 * identifier, but need not necessarily be equal.
	 *
	 * @return The object's identifier
	 */
	public T getName();
}

