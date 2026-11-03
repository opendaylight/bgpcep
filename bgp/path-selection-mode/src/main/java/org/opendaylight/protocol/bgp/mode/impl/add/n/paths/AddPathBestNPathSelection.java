/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.add.n.paths;

import java.util.stream.IntStream;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.api.RouteEntry;
import org.opendaylight.yangtools.yang.common.Uint8;

@NonNullByDefault
public final class AddPathBestNPathSelection implements PathSelectionMode {
    private static final @NonNull AddPathBestNPathSelection [] INSTANCES = IntStream.rangeClosed(2, 8)
        .mapToObj(npaths -> new AddPathBestNPathSelection((short) npaths))
        .toArray(AddPathBestNPathSelection[]::new);

    private final short npaths;

    private AddPathBestNPathSelection(final short npaths) {
        this.npaths = npaths;
    }

    public static AddPathBestNPathSelection of(final Uint8 npaths) {
        final var unsigned = npaths.toJava();
        final var shifted = unsigned - 2;
        if (shifted < 0) {
            throw new IllegalArgumentException("npaths < 2");
        }
        return shifted < INSTANCES.length ? INSTANCES[shifted] : new AddPathBestNPathSelection(unsigned);
    }

    @Override
    public RouteEntry createRouteEntry() {
        return new NPathsRouteEntry(npaths);
    }
}
