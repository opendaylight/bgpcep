/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static com.google.common.base.Verify.verify;
import static org.awaitility.Awaitility.dontCatchUncaughtExceptions;
import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.OPERATIONAL;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import io.netty.util.concurrent.Future;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

public final class CheckUtil {
    private static final Duration POLL_INTERVAL = Duration.ofMillis(200);
    private static final Duration NOT_PRESENT_TIMEOUT = Duration.ofSeconds(10);
    // Slack over the sustain window, covering poll granularity and read latency
    private static final Duration SUSTAIN_SLACK = Duration.ofSeconds(1);
    private static final int TIMEOUT = 30;
    private static final int FUTURE_TIMEOUT_SECONDS = 200;

    private CheckUtil() {
        // Hidden on purpose
    }

    public static <T extends Future<?>> void waitFutureSuccess(final T future) {
        waitFutureSuccess(future, FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @VisibleForTesting
    static <T extends Future<?>> void waitFutureSuccess(final T future, final long timeout, final TimeUnit unit) {
        final CountDownLatch latch = new CountDownLatch(1);
        future.addListener(future1 -> latch.countDown());
        Uninterruptibles.awaitUninterruptibly(latch, timeout, unit);
        verify(future.isSuccess());
    }

    public static <R, T extends DataObject> R readDataOperational(final DataBroker dataBroker,
            final DataObjectIdentifier<T> iid, final Function<T, R> function) {
        return readDataOperational(dataBroker, iid, function, TIMEOUT);
    }

    @VisibleForTesting
    static <R, T extends DataObject> R readDataOperational(final DataBroker dataBroker,
            final DataObjectIdentifier<T> iid, final Function<T, R> function, final int timeout) {
        return readData(dataBroker, OPERATIONAL, iid, function, timeout);
    }

    public static <R, T extends DataObject> R readDataConfiguration(final DataBroker dataBroker,
            final DataObjectIdentifier<T> iid, final Function<T, R> function) {
        return readDataConfiguration(dataBroker, iid, function, TIMEOUT);
    }

    @VisibleForTesting
    static <R, T extends DataObject> R readDataConfiguration(final DataBroker dataBroker,
            final DataObjectIdentifier<T> iid, final Function<T, R> function, final int timeout) {
        return readData(dataBroker, CONFIGURATION, iid, function, timeout);
    }

    private static <R, T extends DataObject> R readData(final DataBroker dataBroker, final LogicalDatastoreType ldt,
            final DataObjectIdentifier<T> iid, final Function<T, R> function, final int timeout) {
        final var result = new AtomicReference<R>();
        dontCatchUncaughtExceptions().atMost(Duration.ofSeconds(timeout)).pollInterval(POLL_INTERVAL)
            .pollDelay(Duration.ZERO)
            .untilAsserted(() -> {
                final ListenableFuture<Optional<T>> future;
                try (var tx = dataBroker.newReadOnlyTransaction()) {
                    future = tx.read(ldt, iid);
                }

                try {
                    result.set(function.apply(future.get().orElseThrow(
                        () -> new AssertionError("Data not present at " + iid))));
                } catch (ExecutionException e) {
                    // untilAsserted() retries AssertionError only, so surface a failed read as one
                    throw new AssertionError("Failed to read " + iid, e);
                } catch (InterruptedException e) {
                    // Anything but an AssertionError aborts the poll, which is what an interrupt calls for
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while reading " + iid, e);
                }
            });
        return result.get();
    }

    public static <T extends DataObject> T checkPresentOperational(final DataBroker dataBroker,
            final DataObjectIdentifier<T> iid) {
        return readData(dataBroker, OPERATIONAL, iid, bgpRib -> bgpRib, TIMEOUT);
    }

    public static <T extends DataObject> T checkPresentConfiguration(final DataBroker dataBroker,
            final DataObjectIdentifier<T> iid) {
        return readData(dataBroker, CONFIGURATION, iid, bgpRib -> bgpRib, TIMEOUT);
    }

    public static <T extends DataObject> void checkNotPresentOperational(final DataBroker dataBroker,
            final DataObjectIdentifier<T> iid) {
        checkNotPresent(dataBroker, OPERATIONAL, iid, Duration.ZERO, NOT_PRESENT_TIMEOUT);
    }

    /**
     * Asserts that {@code iid} is absent from the operational datastore and stays absent for {@code sustainFor}.
     *
     * <p>The plain overload waits up to ten seconds for the data to disappear and returns as soon as it has.
     * This one instead starts asserting immediately and requires absence to hold continuously. Should the data
     * appear within the window, the hold restarts; as the budget is only slightly longer than the window, the
     * check then runs out of time and fails with ConditionTimeoutException, carrying the assertion that last
     * failed as its cause.
     *
     * <p>Use this when absence must be verified across an asynchronous writer (e.g. a periodic task) rather
     * than just once.
     */
    public static <T extends DataObject> void checkNotPresentOperational(final DataBroker dataBroker,
            final DataObjectIdentifier<T> iid, final Duration sustainFor) {
        checkNotPresent(dataBroker, OPERATIONAL, iid, sustainFor, sustainFor.plus(SUSTAIN_SLACK));
    }

    public static <T extends DataObject> void checkNotPresentConfiguration(final DataBroker dataBroker,
            final DataObjectIdentifier<T> iid) {
        checkNotPresent(dataBroker, CONFIGURATION, iid, Duration.ZERO, NOT_PRESENT_TIMEOUT);
    }

    private static <T extends DataObject> void checkNotPresent(final DataBroker dataBroker,
            final LogicalDatastoreType ldt, final DataObjectIdentifier<T> iid, final Duration sustainFor,
            final Duration timeout) {
        dontCatchUncaughtExceptions().atMost(timeout).pollInterval(Duration.ofMillis(10))
            .pollDelay(Duration.ZERO).during(sustainFor)
            .untilAsserted(() -> {
                final ListenableFuture<Boolean> future;
                try (var tx = dataBroker.newReadOnlyTransaction()) {
                    future = tx.exists(ldt, iid);
                }

                try {
                    if (future.get()) {
                        throw new AssertionError("Data still exists at " + iid);
                    }
                } catch (ExecutionException e) {
                    throw new AssertionError("Failed to read " + iid, e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while reading " + iid, e);
                }
            });
    }

    public static void checkEquals(final CheckEquals function) {
        checkEquals(function, TIMEOUT);
    }

    public static void checkEquals(final CheckEquals function, final int timeout) {
        dontCatchUncaughtExceptions().atMost(Duration.ofSeconds(timeout)).pollInterval(Duration.ofMillis(10))
            .pollDelay(Duration.ZERO)
            .untilAsserted(() -> {
                try {
                    function.check();
                } catch (ExecutionException e) {
                    throw new AssertionError("Check failed", e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while checking", e);
                }
            });
    }

    public static void checkReceivedMessages(final ListenerCheck listener, final int numberOfMessages) {
        checkReceivedMessages(listener, numberOfMessages, TIMEOUT);
    }

    @VisibleForTesting
    static void checkReceivedMessages(final ListenerCheck listener, final int numberOfMessages,
            final int timeout) {
        dontCatchUncaughtExceptions().atMost(Duration.ofSeconds(timeout)).pollInterval(POLL_INTERVAL)
            .pollDelay(Duration.ZERO)
            .untilAsserted(() -> {
                if (listener.getListMessageSize() != numberOfMessages) {
                    throw new AssertionError("Expected " + numberOfMessages + " but received "
                        + listener.getListMsg());
                }
            });
    }

    public interface ListenerCheck {

        List<?> getListMsg();

        int getListMessageSize();
    }

    @FunctionalInterface
    public interface CheckEquals {
        void check() throws ExecutionException, InterruptedException;
    }
}
