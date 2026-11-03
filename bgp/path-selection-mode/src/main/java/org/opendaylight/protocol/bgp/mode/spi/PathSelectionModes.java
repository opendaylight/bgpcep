/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.spi;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.impl.add.all.paths.AllPathSelection;
import org.opendaylight.protocol.bgp.mode.impl.add.n.paths.AddPathBestNPathSelection;
import org.opendaylight.protocol.bgp.mode.impl.base.BasePathSelection;
import org.opendaylight.yangtools.yang.common.Uint8;

/**
 * Various {@link PathSelectionMode} implementations
 */
@NonNullByDefault
public final class PathSelectionModes {
    private PathSelectionModes() {
        // hidden on purpose
    }

    public static PathSelectionMode base() {
        return BasePathSelection.INSTANCE;
    }

    public static PathSelectionMode addAllPaths() {
        return AllPathSelection.INSTANCE;
    }

    public static PathSelectionMode addNPaths(final Uint8 npaths) {
        return npaths.compareTo(Uint8.TWO) < 0 ? AllPathSelection.INSTANCE : AddPathBestNPathSelection.of(npaths);
    }

    public static PathSelectionMode addNPaths(final int npaths) {
        return addNPaths(Uint8.valueOf(npaths));
    }
}
