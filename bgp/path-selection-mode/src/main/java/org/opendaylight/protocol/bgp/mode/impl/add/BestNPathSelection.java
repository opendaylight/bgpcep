/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.add;

import java.util.stream.IntStream;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntry;
import org.opendaylight.yangtools.yang.common.Uint8;

@NonNullByDefault
public final class BestNPathSelection implements PathSelectionMode {
    private static final @NonNull BestNPathSelection [] INSTANCES = IntStream.rangeClosed(2, 8)
        .mapToObj(npaths -> new BestNPathSelection((short) npaths))
        .toArray(BestNPathSelection[]::new);

    private final short npaths;

    private BestNPathSelection(final short npaths) {
        this.npaths = npaths;
    }

    public static BestNPathSelection of(final Uint8 npaths) {
        final var unsigned = npaths.toJava();
        final var shifted = unsigned - 2;
        if (shifted < 0) {
            throw new IllegalArgumentException("npaths < 2");
        }
        return shifted < INSTANCES.length ? INSTANCES[shifted] : new BestNPathSelection(unsigned);
    }

    @Override
    public RouteEntry createRouteEntry() {
        return new AddNRouteEntry(npaths);
    }
}
