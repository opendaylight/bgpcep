/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi.util;

import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClusterSingletonServiceRegistrationHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterSingletonServiceRegistrationHelper.class);

    private ClusterSingletonServiceRegistrationHelper() {
        throw new UnsupportedOperationException();
    }

    /**
     * This helper function wraps {@link ClusterSingletonServiceProvider#registerClusterSingletonService(ClusterSingletonService)} in order to
     * execute repeated registration attempts while catching RuntimeException. If registration is not successful, IllegalStateException is thrown.
     * @param singletonProvider
     * @param clusterSingletonService
     * @param maxAttempts
     * @param sleepTime
     * @return Registration
     */
    public static ClusterSingletonServiceRegistration registerSingletonService(final ClusterSingletonServiceProvider singletonProvider,
            final ClusterSingletonService clusterSingletonService, final int maxAttempts, final int sleepTime) {
        int attempts = maxAttempts;
        while (true) {
            try {
              return singletonProvider.registerClusterSingletonService(clusterSingletonService);
            } catch (final RuntimeException e) {
                if (attempts-- == 0) {
                    LOG.error("Giving up after {} registration attempts for service {}.", maxAttempts, clusterSingletonService, e);
                    throw e;
                }
                LOG.warn("Failed to register {} service to ClusterSingletonServiceProvider. Try again in {} ms.", clusterSingletonService, sleepTime);
                try {
                    Thread.sleep(sleepTime);
                } catch (final InterruptedException e1) {
                    //ignore
                }
            }
        }
    }

}
