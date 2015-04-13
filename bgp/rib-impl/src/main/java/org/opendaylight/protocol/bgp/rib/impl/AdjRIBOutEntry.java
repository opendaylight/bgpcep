/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.opendaylight.protocol.bgp.rib.spi.RouteEncoder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yangtools.yang.binding.DataContainer;

@Deprecated
final class AdjRIBOutEntry<K, V extends Route> {
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<AdjRIBOutEntry, Route> NLRIENTRY_ADV_UPDATER = AtomicReferenceFieldUpdater.newUpdater(AdjRIBOutEntry.class, Route.class, "advertisedValue");
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<AdjRIBOutEntry, Route> NLRIENTRY_CUR_UPDATER = AtomicReferenceFieldUpdater.newUpdater(AdjRIBOutEntry.class, Route.class, "currentValue");

    /*
     * Marker object for uninitialized value. Distinct from null.
     */
    private static final Route NLRIENTRY_NONE_VALUE = new Route() {
        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            throw new IllegalStateException("This method should never be invoked");
        }

        @Override
        public Attributes getAttributes() {
            throw new IllegalStateException("This method should never be invoked");
        }
    };

    // Referenced via AtomicReferenceFieldUpdaters
    @SuppressWarnings("unused")
    private volatile Route currentValue = NLRIENTRY_NONE_VALUE;
    @SuppressWarnings("unused")
    private volatile Route advertisedValue = NLRIENTRY_NONE_VALUE;
    private final RouteEncoder ribOut;

    AdjRIBOutEntry(final RouteEncoder ribOut) {
        this.ribOut = Preconditions.checkNotNull(ribOut);
    }

    Route getAdverised() {
        return NLRIENTRY_ADV_UPDATER.get(this);
    }

    void setAdverised(final V value) {
        NLRIENTRY_ADV_UPDATER.set(this, value);
    }

    @SuppressWarnings("unchecked")
    V getCurrent() {
        final Route o = NLRIENTRY_CUR_UPDATER.get(this);
        Preconditions.checkState(!isNone(o), "Value cannot be NONE here");
        return (V) o;
    }

    Route getAndSetCurrent(final V value) {
        return NLRIENTRY_CUR_UPDATER.getAndSet(this, value);
    }

    RouteEncoder getRibOut() {
        return this.ribOut;
    }

    static boolean isNone(final Object o) {
        return NLRIENTRY_NONE_VALUE.equals(o);
    }
}