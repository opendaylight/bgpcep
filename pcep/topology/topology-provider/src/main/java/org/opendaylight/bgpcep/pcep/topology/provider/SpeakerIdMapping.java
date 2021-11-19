/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.net.InetAddress;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.concepts.Immutable;

public final class SpeakerIdMapping implements Immutable {
    private static final @NonNull SpeakerIdMapping EMPTY = new SpeakerIdMapping(ImmutableMap.of());

    private final ImmutableMap<InetAddress, byte[]> map;

    private SpeakerIdMapping(final Map<InetAddress, byte[]> map) {
        this.map = ImmutableMap.copyOf(map);
    }

    public static @NonNull SpeakerIdMapping of() {
        return EMPTY;
    }

    public static @NonNull SpeakerIdMapping copyOf(final Map<InetAddress, byte[]> map) {
        return map.isEmpty() ? of()
            // Defensive: disconnect byte[]s from caller
            : new SpeakerIdMapping(Maps.transformValues(map, byte[]::clone));
    }

    public byte @Nullable [] speakerIdForAddress(final InetAddress address) {
        final byte[] found = map.get(address);
        // Defensive: do not leak byte[]
        return found == null ? null : found.clone();
    }
}