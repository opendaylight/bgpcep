/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

/**
 * The simple interface needed to battle object proliferation when many objects of a type with low cardinality are
 * created. The idea is that you still create the object, but rather than hanging on to it, you pass it through the
 * cache, which may replace your object with a reference to a previously-created object.
 */
public interface ReferenceCache {
    /**
     * Get a shared reference to an object. The contract here is that the returned object is an instance of the same
     * class and compares equal to the passed object.
     *
     * @param object An object to which you'd like to
     * @return Shared reference to the object.
     */
    <T> T getSharedReference(T object);
}
