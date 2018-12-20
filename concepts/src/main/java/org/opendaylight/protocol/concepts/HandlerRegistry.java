/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.yangtools.concepts.Registration;

@ThreadSafe
public class HandlerRegistry<C, P, S> {
    private final MultiRegistry<Class<? extends C>, S> serializers = new MultiRegistry<>();
    private final MultiRegistry<Integer, P> parsers = new MultiRegistry<>();

    public Registration registerParser(final int type, final P parser) {
        return this.parsers.register(type, parser);
    }

    public P getParser(final int type) {
        return this.parsers.get(type);
    }

    public Registration registerSerializer(final Class<? extends C> clazz, final S serializer) {
        return this.serializers.register(clazz, serializer);
    }

    public S getSerializer(final Class<? extends C> clazz) {
        return this.serializers.get(clazz);
    }

    public Iterable<S> getAllSerializers() {
        return this.serializers.getAllValues();
    }
}
