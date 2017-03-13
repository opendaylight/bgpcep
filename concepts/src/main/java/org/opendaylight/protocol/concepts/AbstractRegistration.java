/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Utility registration handle. It is a convenience for register-style method which can return an AutoCloseable realized
 * by a subclass of this class. Invoking the close() method triggers unregistration of the state the method installed.
 */
@ThreadSafe
public abstract class AbstractRegistration implements AutoCloseable {
    @GuardedBy("this")
    private boolean closed = false;

    /**
     * Remove the state referenced by this registration. This method is guaranteed to be called at most once. The
     * referenced state must be retained until this method is invoked.
     */
    protected abstract void removeRegistration();

    @Override
    public final synchronized void close() {
        if (!this.closed) {
            this.closed = true;
            removeRegistration();
        }
    }
}
