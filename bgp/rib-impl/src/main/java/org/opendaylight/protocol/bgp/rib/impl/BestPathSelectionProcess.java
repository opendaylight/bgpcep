/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BestPathSelectionProcess implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(BestPathSelectionProcess.class);
    private static final int BATCH_SIZE = 200;
    private final BlockingQueue<RibTableUpdate<?>> updatedEntries = new LinkedBlockingQueue<>();

    void routeTableUpdated(final RibTableUpdate<?> update) {
        updatedEntries.add(update);
    }

    @Override
    public void run() {
        try {
            while (true) {
                final Collection<RibTableUpdate<?>> batch = new ArrayList<>(BATCH_SIZE);

                batch.add(updatedEntries.take());
                updatedEntries.drainTo(batch);

                LOG.debug("Processing {} table updates", batch.size());
                for (RibTableUpdate<?> u : batch) {
                    // FIXME: set local AS number
                    u.selectBestPaths(null);
                }
            }
        } catch (InterruptedException e) {
            LOG.info("Best path selection process interrupted, exiting with {} unprocessed entries", updatedEntries.size(), e);
        }
    }
}
