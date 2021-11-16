/*
 * Copyright (c) 2016 AT&T Services, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.Immutable;

public final class KeyMapping implements Immutable {
    private static final @NonNull KeyMapping EMPTY = new KeyMapping(ImmutableMap.of());

    private final ImmutableMap<InetAddress, byte[]> map;

    private KeyMapping(final Map<InetAddress, byte[]> map) {
        this.map = ImmutableMap.copyOf(map);
    }

    public static @NonNull KeyMapping of() {
        return EMPTY;
    }

    public static @NonNull KeyMapping of(final @NonNull InetAddress inetAddress, final @NonNull String password) {
        return new KeyMapping(ImmutableMap.of(inetAddress, password.getBytes(StandardCharsets.US_ASCII)));
    }

    public static @NonNull KeyMapping of(final Map<InetAddress, String> passwords) {
        return passwords.isEmpty() ? of()
            : new KeyMapping(Maps.transformValues(passwords, password -> password.getBytes(StandardCharsets.US_ASCII)));
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public @NonNull Map<InetAddress, byte[]> asMap() {
        // Careful: do not leak our byte[]s
        return Maps.transformValues(map, byte[]::clone);
    }
}
