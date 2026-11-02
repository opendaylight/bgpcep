/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2026 PANTHEON.tech, s.r.o.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.state;

import static java.util.Objects.requireNonNull;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.rib.DefaultRibReference;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpId;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

public abstract class AbstractBGPRibState extends DefaultRibReference implements BGPRibState {
    private final @NonNull BgpId routeId;
    private final @NonNull AsNumber localAs;

    @GuardedBy("this")
    private boolean active;

    protected AbstractBGPRibState(final DataObjectIdentifier.@NonNull WithKey<Rib, RibKey> instanceIdentifier,
            final @NonNull BgpId routeId, final @NonNull AsNumber localAs) {
        super(instanceIdentifier);
        this.routeId = requireNonNull(routeId);
        this.localAs = requireNonNull(localAs);
    }

    @Override
    public final AsNumber getAs() {
        return localAs;
    }

    @Override
    public final BgpId getRouteId() {
        return routeId;
    }

    @Override
    public final synchronized boolean isActive() {
        return active;
    }

    protected final synchronized void setActive(final boolean active) {
        this.active = active;
    }
}
