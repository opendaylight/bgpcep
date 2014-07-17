/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * A per-peer collection of data which needs to be sent out. This class is kept lockfree
 * on purpose.
 */
final class AdjRIBOut {
    private static final class NlriEntry {
        // Referenced via AtomicReferenceFieldUpdaters below
        @SuppressWarnings("unused")
        private volatile DataObject currentValue = NLRIENTRY_NONE_VALUE;
        @SuppressWarnings("unused")
        private volatile DataObject advertisedValue = NLRIENTRY_NONE_VALUE;
    }

    /*
     * Marker object for uninitialized value. Distinct from null.
     */
    private static final DataObject NLRIENTRY_NONE_VALUE = new DataObject() {
        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            throw new IllegalStateException("This method should never be invoked");
        }
    };

    private static final AtomicReferenceFieldUpdater<NlriEntry, DataObject> NLRIENTRY_CUR_UPDATER = AtomicReferenceFieldUpdater.newUpdater(NlriEntry.class, DataObject.class, "currentValue");
    private static final AtomicReferenceFieldUpdater<NlriEntry, DataObject> NLRIENTRY_ADV_UPDATER = AtomicReferenceFieldUpdater.newUpdater(NlriEntry.class, DataObject.class, "advertisedValue");

    private final Queue<Object> queue = new ConcurrentLinkedQueue<>();
    private final ConcurrentMap<Object, NlriEntry> data = new ConcurrentHashMap<>();

    public void put(final Object key, final DataObject value) {
        NlriEntry e = data.get(key);
        if (e == null) {
            if (value == null) {
                // Already not advertised, nothing to do
                return;
            }

            e = new NlriEntry();
        }

        // Make sure the new value is visible to the advertiser thread
        final DataObject oldValue = NLRIENTRY_CUR_UPDATER.getAndSet(e, value);

        // Now read what is being currently advertised
        final DataObject advValue = NLRIENTRY_ADV_UPDATER.get(e);

        if (advValue == value) {
            /*
             * We raced with the advertiser, which has sent out the this advertisement.
             * This means our job is done.
             */
            return;
        }

        if (value != null && advValue == NLRIENTRY_NONE_VALUE) {
            /*
             * We are advertising a new value and this is not a entry: need to put
             * it into the map.
             */
            data.put(key, e);
        }

        if (oldValue == advValue) {
            /*
             * The old value was being advertised, so the advertiser is not aware that
             * this key needs updating. Enqueue the key, so it will see it.
             */
            queue.add(key);

            // FIXME: we need to Object.notify() something to unblock the advertiser
        }

    }

    public void process() {
        for (Object key = queue.poll(); key != null; key = queue.poll()) {
            final NlriEntry e = data.get(key);
            if (e == null) {
                /*
                 * This was a notification for a value which has already been withdrawn,
                 * nothing to do, continue to next key.
                 */
                continue;
            }

            // First read what is it that we are advertising
            final DataObject advValue = NLRIENTRY_ADV_UPDATER.get(e);
            Preconditions.checkState(advValue != null, "Unexpected withdrawn entry %s for %s", e, key);

            // Now read what we should be advertising
            final DataObject newValue = NLRIENTRY_CUR_UPDATER.get(e);

            if (!advValue.equals(newValue)) {
                /*
                 * The advertised value is not the same as what we want to advertise,
                 * so we need to send it out.
                 *
                 * FIXME: format the PDU and send it off
                 */
            }

            /*
             * Save what we are advertising. We need to store this even for withdrawals,
             * as the entry may have been picked up again.
             */
            NLRIENTRY_ADV_UPDATER.set(e, advValue);

            /*
             * Ready to clean the entry. Just a tiny check first: has the new value
             * changed? If it has, we need to keep the entry, as there is an incoming
             * update.
             */
            if (newValue == null && NLRIENTRY_CUR_UPDATER.get(e) == newValue) {
                data.remove(key, e);
            }
        }
    }
}
