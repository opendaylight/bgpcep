/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.entry;

import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * An update to advertized {@link BestPath}.
 *
 * @param adverized collection of {@link AdvertizedRoute}s to advertize after processing update of stale
 *                  and new advertisement of routes
 * @param stale the {@link StaleBestPathRoute} to withdraw, or {@code null} if there is nothing to withdraw
 * @since 3.0.0
 */
@NonNullByDefault
public record BestPathUpdate(List<AdvertizedRoute> adverized, @Nullable StaleBestPathRoute stale) {
    public BestPathUpdate {
        adverized = List.copyOf(adverized);
    }
}