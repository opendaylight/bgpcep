/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi.util;

import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.TimeUnit;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class which provides helper functionality for ClusterSingletonService.
 */
public final class ClusterSingletonServiceRegistrationHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterSingletonServiceRegistrationHelper.class);
    private static final int MAX_REGISTRATION_ATTEMPTS = 10;
    private static final int SLEEP_TIME_MILLIS = MAX_REGISTRATION_ATTEMPTS;

    private ClusterSingletonServiceRegistrationHelper() {
        throw new UnsupportedOperationException();
    }

    /**
     * This helper function wraps
     * {@link ClusterSingletonServiceProvider#registerClusterSingletonService(ClusterSingletonService)} in order to
     * execute repeated registration attempts while catching RuntimeException. If registration is not successful,
     * RuntimeException is re-thrown.
     *
     * @param singletonProvider Cluster Singleton Service Provider
     * @param clusterSingletonService Cluster Singleton Service
     * @param maxAttempts             Upper bound for registration retries count.
     * @param sleepTime               Sleep time between registration retries in milliseconds.
     * @return Registration
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    public static ClusterSingletonServiceRegistration registerSingletonService(
        final ClusterSingletonServiceProvider singletonProvider,
        final ClusterSingletonService clusterSingletonService, final int maxAttempts, final int sleepTime) {
        int attempts = maxAttempts;
        while (true) {
            try {
                return singletonProvider.registerClusterSingletonService(clusterSingletonService);
            } catch (final RuntimeException e) {
                if (attempts == 0) {
                    LOG.error("Giving up after {} registration attempts for service {}.", maxAttempts,
                        clusterSingletonService, e);
                    throw e;
                }
                attempts--;
                LOG.warn("Failed to register {} service to ClusterSingletonServiceProvider. Try again in {} ms. {}",
                    clusterSingletonService, sleepTime, e);
                Uninterruptibles.sleepUninterruptibly(sleepTime, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * This helper function wraps
     * {@link ClusterSingletonServiceProvider#registerClusterSingletonService(ClusterSingletonService)} in order to
     * execute repeated registration attempts while catching RuntimeException. If registration is not successful,
     * RuntimeException is re-thrown. 10 registration attempts will be tried with 10 ms pause between each
     * other.
     *
     * @param singletonProvider Cluster Singleton Service Provider
     * @param clusterSingletonService Cluster Singleton Service
     * @return Registration
     */
    public static ClusterSingletonServiceRegistration registerSingletonService(final ClusterSingletonServiceProvider
        singletonProvider, final ClusterSingletonService clusterSingletonService) {
        return ClusterSingletonServiceRegistrationHelper.registerSingletonService(singletonProvider,
            clusterSingletonService, MAX_REGISTRATION_ATTEMPTS, SLEEP_TIME_MILLIS);
    }
}
