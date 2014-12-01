/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

/**
 * A simple reference cache which actually does not cache anything.
 */
public final class NoopReferenceCache implements ReferenceCache {
    private static final class Holder {
        static final NoopReferenceCache INSTANCE = new NoopReferenceCache();

        private Holder() {
        }
    }

    private NoopReferenceCache() {

    }

    @Override
    public <T> T getSharedReference(final T object) {
        return object;
    }

    public static NoopReferenceCache getInstance() {
        return Holder.INSTANCE;
    }
}
