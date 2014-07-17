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

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * A per-peer collection of data which needs to be sent out. This class is kept lockfree
 * on purpose.
 *
 * @param <K> NLRI object type
 * @param <V> Attribute object type
 */
abstract class AbstractAdjRIBOut<K, V extends Route> {
    private final ConcurrentMap<K, AdjRIBOutEntry<V>> data = new ConcurrentHashMap<>();
    private final Queue<K> queue = new ConcurrentLinkedQueue<>();

    /**
     * Callback invoked from RIB backend when new data becomes available. Implementations
     * are expected to be able to handle multiple notifications and perform state
     * compression as appropriate.
     */
    protected abstract void wantWrite();

    /**
     * Write a single PDU onto the peer. This callback is issued from {@link #process()}.
     *
     * @param key Route key (NLRI)
     * @param value Route value (attributes)
     * @return True if the peer is ready to accept another PDU.
     */
    protected abstract boolean writePDU(K key, V value);

    public void put(final K key, final V newValue) {
        AdjRIBOutEntry<V> e = data.get(key);
        if (e == null) {
            if (newValue == null) {
                // Already not advertised, nothing to do
                return;
            }

            e = new AdjRIBOutEntry<V>();
        }

        // Make sure the new value is visible to the advertiser thread
        final DataObject oldValue = e.getAndSetCurrent(newValue);

        // Now read what is being currently advertised
        final DataObject advValue = e.getAdverised();

        if (advValue == newValue) {
            /*
             * We raced with the advertiser, which has sent out the this advertisement.
             * This means our job is done.
             */
            return;
        }

        if (newValue != null && AdjRIBOutEntry.isNone(advValue)) {
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
            wantWrite();
        }
    }

    /**
     * Call this method from superclass when you are ready to process outgoing PDUs.
     */
    protected final void process() {
        boolean writable = true;

        while (writable) {
            final K key = queue.poll();
            if (key == null) {
                break;
            }

            final AdjRIBOutEntry<V> e = data.get(key);
            if (e == null) {
                /*
                 * This was a notification for a value which has already been withdrawn,
                 * nothing to do, continue to next key.
                 */
                continue;
            }

            // First read what is it that we are advertising
            final DataObject oldValue = e.getAdverised();
            Preconditions.checkState(oldValue != null, "Unexpected withdrawn entry %s for %s", e, key);

            // Now read what we should be advertising
            final V newValue = e.getCurrent();

            if (!oldValue.equals(newValue)) {
                /*
                 * The advertised value is not the same as what we want to advertise,
                 * so we need to send it out.
                 */
                writable = writePDU(key, newValue);
            }

            /*
             * Save what we are advertising. We need to store this even for withdrawals,
             * as the entry may have been picked up again.
             */
            e.setAdverised(newValue);

            /*
             * Ready to clean the entry. Just a tiny check first: has the new value
             * changed? If it has, we need to keep the entry, as there is an incoming
             * update.
             */
            if (newValue == null && e.getCurrent() == newValue) {
                data.remove(key, e);
            }
        }
    }
}
