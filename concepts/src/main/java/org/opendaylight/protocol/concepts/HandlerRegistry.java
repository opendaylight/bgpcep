/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class HandlerRegistry<C, P, S> {
    private final MultiRegistry<Class<? extends C>, S> serializers = new MultiRegistry<>();
    private final MultiRegistry<Integer, P> parsers = new MultiRegistry<>();

    public AbstractRegistration registerParser(final int type, final P parser) {
        return parsers.register(type, parser);
    }

    public P getParser(final int type) {
        return parsers.get(type);
    }

    public AbstractRegistration registerSerializer(final Class<? extends C> clazz, final S serializer) {
        return serializers.register(clazz, serializer);
    }

    public S getSerializer(final Class<? extends C> clazz) {
        return serializers.get(clazz);
    }
}
