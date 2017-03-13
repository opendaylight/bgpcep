/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import com.google.common.base.Preconditions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.protocol.concepts.AbstractRegistration;

abstract class AbstractFamilyRegistry<C, N> {
    private final Map<Class<? extends C>, N> classToNumber = new ConcurrentHashMap<>();
    private final Map<N, Class<? extends C>> numberToClass = new ConcurrentHashMap<>();

    protected synchronized AutoCloseable registerFamily(final Class<? extends C> clazz, final N number) {
        Preconditions.checkNotNull(clazz);

        final Class<?> c = this.numberToClass.get(number);
        Preconditions.checkState(c == null, "Number " + number + " already registered to " + c);

        final N n = this.classToNumber.get(clazz);
        Preconditions.checkState(n == null, "Class " + clazz + " already registered to " + n);

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

    protected Class<? extends C> classForFamily(final N number) {
        return this.numberToClass.get(number);
    }

    protected N numberForClass(final Class<? extends C> clazz) {
        return this.classToNumber.get(clazz);
    }
}
