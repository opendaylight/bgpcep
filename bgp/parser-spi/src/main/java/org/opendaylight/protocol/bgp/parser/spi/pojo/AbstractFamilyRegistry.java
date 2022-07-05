/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.concepts.Registration;

abstract class AbstractFamilyRegistry<C, N> {
    private final Map<C, N> classToNumber = new ConcurrentHashMap<>();
    private final Map<N, C> numberToClass = new ConcurrentHashMap<>();

    protected synchronized Registration registerFamily(final C clazz, final N number) {
        requireNonNull(clazz);

        final C c = this.numberToClass.get(number);
        checkState(c == null, "Number " + number + " already registered to " + c);

        final N n = this.classToNumber.get(clazz);
        checkState(n == null, "Class " + clazz + " already registered to " + n);

        this.numberToClass.put(number, clazz);
        this.classToNumber.put(clazz, number);

        final Object lock = this;
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (lock) {
                    AbstractFamilyRegistry.this.classToNumber.remove(clazz);
                    AbstractFamilyRegistry.this.numberToClass.remove(number);
                }
            }
        };
    }

    protected C classForFamily(final N number) {
        return this.numberToClass.get(number);
    }

    protected N numberForClass(final C clazz) {
        return this.classToNumber.get(clazz);
    }
}
