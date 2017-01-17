/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import io.netty.util.concurrent.Future;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.Assert;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class CheckUtil {
    private static final int TIMEOUT = 40;
    private static final int LATCH_TIMEOUT = 10;
    private static final int SLEEP_FOR = 20;
    private static final int SLEEP_UNINTERRUPTIBLY = 50;
    static void checkReceivedMessages(final SimpleSessionListener listener, final int numberOfMessages)
        throws ReadFailedException {
        final Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= TIMEOUT) {
            if (listener.getListMsg().size() != numberOfMessages) {
                Uninterruptibles.sleepUninterruptibly(SLEEP_UNINTERRUPTIBLY, TimeUnit.MILLISECONDS);
            } else {
                return;
            }
        }
        Assert.fail();
    }

    public static <R, T extends DataObject> R readData(final DataBroker dataBroker, final InstanceIdentifier<T> iid,
        final Function<T, R> function)
        throws ReadFailedException {
        AssertionError lastError = null;
        final Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= TIMEOUT) {
            try (final ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction()) {
                final Optional<T> data = tx.read(LogicalDatastoreType.OPERATIONAL, iid).checkedGet();
                if (data.isPresent()) {
                    try {
                        return function.apply(data.get());
                    } catch (final AssertionError e) {
                        lastError = e;
                        Uninterruptibles.sleepUninterruptibly(SLEEP_FOR, TimeUnit.MILLISECONDS);
                    }
                }
            }
        }
        Assert.fail(lastError.getMessage());
        throw lastError;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Future> void waitFutureSuccess(final T future) {
        final CountDownLatch latch = new CountDownLatch(1);
        future.addListener(future1 -> latch.countDown());
        Uninterruptibles.awaitUninterruptibly(latch, LATCH_TIMEOUT, TimeUnit.SECONDS);
    }
}