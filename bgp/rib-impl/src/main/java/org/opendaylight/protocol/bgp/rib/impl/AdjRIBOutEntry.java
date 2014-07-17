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

import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;

final class AdjRIBOutEntry<T extends DataObject> {
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<AdjRIBOutEntry, DataObject> NLRIENTRY_ADV_UPDATER = AtomicReferenceFieldUpdater.newUpdater(AdjRIBOutEntry.class, DataObject.class, "advertisedValue");
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<AdjRIBOutEntry, DataObject> NLRIENTRY_CUR_UPDATER = AtomicReferenceFieldUpdater.newUpdater(AdjRIBOutEntry.class, DataObject.class, "currentValue");

    /*
     * Marker object for uninitialized value. Distinct from null.
     */
    private static final DataObject NLRIENTRY_NONE_VALUE = new DataObject() {
        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            throw new IllegalStateException("This method should never be invoked");
        }
    };

    // Referenced via AtomicReferenceFieldUpdaters
    @SuppressWarnings("unused")
    private volatile DataObject currentValue = NLRIENTRY_NONE_VALUE;
    @SuppressWarnings("unused")
    private volatile DataObject advertisedValue = NLRIENTRY_NONE_VALUE;

    DataObject getAdverised() {
        return NLRIENTRY_ADV_UPDATER.get(this);
    }

    void setAdverised(final T value) {
        NLRIENTRY_ADV_UPDATER.set(this, value);
    }

    @SuppressWarnings("unchecked")
    T getCurrent() {
        final DataObject o = NLRIENTRY_CUR_UPDATER.get(this);
        Preconditions.checkState(!isNone(o), "Value cannot be NONE here");
        return (T) o;
    }

    DataObject getAndSetCurrent(final T value) {
        return NLRIENTRY_CUR_UPDATER.getAndSet(this, value);
    }

    static final boolean isNone(final Object o) {
        return o == NLRIENTRY_NONE_VALUE;
    }
}