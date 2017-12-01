/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.base.Verify;
import com.google.common.util.concurrent.Uninterruptibles;
import io.netty.util.concurrent.Future;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class CheckUtil {
    private static final int LATCH_TIMEOUT = 10;
    private static final int SLEEP_FOR = 200;
    private static final int TIMEOUT = 30;

    private CheckUtil() {
        throw new UnsupportedOperationException();
    }

    public static <T extends Future<?>> void waitFutureSuccess(final T future) {
        final CountDownLatch latch = new CountDownLatch(1);
        future.addListener(future1 -> latch.countDown());
        Uninterruptibles.awaitUninterruptibly(latch, LATCH_TIMEOUT, TimeUnit.SECONDS);
        Verify.verify(future.isSuccess());
    }

    public static <R, T extends DataObject> R readDataOperational(final DataBroker dataBroker,
            final InstanceIdentifier<T> iid, final Function<T, R> function) throws ReadFailedException {
        return readDataOperational(dataBroker, iid, function, TIMEOUT);
    }

    @VisibleForTesting
    static <R, T extends DataObject> R readDataOperational(final DataBroker dataBroker,
            final InstanceIdentifier<T> iid, final Function<T, R> function, final int timeout)
            throws ReadFailedException {
        return readData(dataBroker, OPERATIONAL, iid, function, timeout);
    }

    public static <R, T extends DataObject> R readDataConfiguration(final DataBroker dataBroker,
            final InstanceIdentifier<T> iid, final Function<T, R> function) throws ReadFailedException {
        return readDataConfiguration(dataBroker, iid, function, TIMEOUT);
    }

    @VisibleForTesting
    static <R, T extends DataObject> R readDataConfiguration(final DataBroker dataBroker,
            final InstanceIdentifier<T> iid, final Function<T, R> function, final int timeout) throws ReadFailedException {
        return readData(dataBroker, CONFIGURATION, iid, function, timeout);
    }

    private static <R, T extends DataObject> R readData(final DataBroker dataBroker, final LogicalDatastoreType ldt,
            final InstanceIdentifier<T> iid, final Function<T, R> function, final int timeout)
            throws ReadFailedException {
        AssertionError lastError = null;
        final Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= timeout) {
            try (final ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction()) {
                final Optional<T> data = tx.read(ldt, iid).checkedGet();
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
        throw lastError;
    }

    public static <T extends DataObject> T checkPresentOperational(final DataBroker dataBroker,
            final InstanceIdentifier<T> iid) throws ReadFailedException {
        return readData(dataBroker, OPERATIONAL, iid, bgpRib -> bgpRib, TIMEOUT);
    }

    public static <T extends DataObject> T checkPresentConfiguration(final DataBroker dataBroker,
            final InstanceIdentifier<T> iid) throws ReadFailedException {
        return readData(dataBroker, CONFIGURATION, iid, bgpRib -> bgpRib, TIMEOUT);
    }

    public static <T extends DataObject> void checkNotPresentOperational(final DataBroker dataBroker,
            final InstanceIdentifier<T> iid) throws ReadFailedException {
        checkNotPresent(dataBroker, OPERATIONAL, iid);
    }

    public static <T extends DataObject> void checkNotPresentConfiguration(final DataBroker dataBroker,
            final InstanceIdentifier<T> iid) throws ReadFailedException {
        checkNotPresent(dataBroker, CONFIGURATION, iid);
    }

    private static <T extends DataObject> void checkNotPresent(final DataBroker dataBroker,
            final LogicalDatastoreType ldt, final InstanceIdentifier<T> iid) throws ReadFailedException {
        AssertionError lastError = null;
        final Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= 10) {
            try (final ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction()) {
                final com.google.common.base.Optional<T> data = tx.read(ldt, iid).checkedGet();
                try {
                    assert !data.isPresent();
                    return;
                } catch (final AssertionError e) {
                    lastError = e;
                    Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MILLISECONDS);
                }
            }
        }
        throw lastError;
    }

    public static void checkEquals(final CheckEquals function) throws Exception {
        checkEquals(function, TIMEOUT);
    }

    public static void checkEquals(final CheckEquals function, final int timeout) throws Exception {
        AssertionError lastError = null;
        final Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= timeout) {
            try {
                function.check();
                return;
            } catch (final AssertionError e) {
                lastError = e;
                Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MILLISECONDS);
            }
        }
        throw lastError;
    }

    public static void checkReceivedMessages(final ListenerCheck listener, final int numberOfMessages)
            throws ReadFailedException {
        checkReceivedMessages(listener, numberOfMessages, TIMEOUT);
    }

    @VisibleForTesting
    static void checkReceivedMessages(final ListenerCheck listener, final int numberOfMessages,
            final int timeout) throws ReadFailedException {
        final Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= timeout) {
            if (listener.getListMessageSize() != numberOfMessages) {
                Uninterruptibles.sleepUninterruptibly(SLEEP_FOR, TimeUnit.MILLISECONDS);
            } else {
                return;
            }
        }
        throw new AssertionError("Expected " + numberOfMessages + " but received "
                + listener.getListMessageSize());
    }

    public interface ListenerCheck {
        int getListMessageSize();
    }

    @FunctionalInterface
    public interface CheckEquals {
        void check() throws ExecutionException, InterruptedException;
    }
}