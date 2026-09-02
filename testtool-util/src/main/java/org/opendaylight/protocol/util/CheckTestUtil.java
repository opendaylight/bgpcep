/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static org.awaitility.Awaitility.await;
import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.OPERATIONAL;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Verify;
import com.google.common.util.concurrent.Uninterruptibles;
import io.netty.util.concurrent.Future;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

public final class CheckTestUtil {
    private static final Duration SLEEP_FOR = Duration.ofMillis(200);
    private static final int TIMEOUT = 30;
    private static final int FUTURE_TIMEOUT_SECONDS = 200;

    private CheckTestUtil() {
        // Hidden on purpose
    }

    public static <T extends Future<?>> void waitFutureSuccess(final T future) {
        final var latch = new CountDownLatch(1);
        future.addListener(future1 -> latch.countDown());
        Uninterruptibles.awaitUninterruptibly(latch, FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Verify.verify(future.isSuccess());
    }

    public static <R, T extends DataObject> R readDataOperational(final DataBroker dataBroker,
            final DataObjectIdentifier<T> iid, final Function<T, R> function)
                throws ExecutionException, InterruptedException {
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
        await().atMost(Duration.ofSeconds(timeout)).pollInterval(SLEEP_FOR).pollDelay(Duration.ZERO)
                .untilAsserted(() -> {
                    try (var tx = dataBroker.newReadOnlyTransaction()) {
                        final var data = tx.read(ldt, iid).get();
                        result.set(function.apply(Objects.requireNonNull(data).orElseThrow(
                            () -> new AssertionError("Data not present at " + iid))));
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
            final DataObjectIdentifier<T> iid) throws ExecutionException, InterruptedException {
        checkNotPresent(dataBroker, OPERATIONAL, iid);
    }

    public static <T extends DataObject> void checkNotPresentConfiguration(final DataBroker dataBroker,
            final DataObjectIdentifier<T> iid) {
        checkNotPresent(dataBroker, CONFIGURATION, iid);
    }

    private static <T extends DataObject> void checkNotPresent(final DataBroker dataBroker,
            final LogicalDatastoreType ldt, final DataObjectIdentifier<T> iid) {
        await().pollDelay(Duration.ZERO).atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(10))
            .untilAsserted(() -> {
                try (ReadTransaction tx = dataBroker.newReadOnlyTransaction()) {
                    if (Objects.requireNonNull(tx.read(ldt, iid).get()).isPresent()) {
                        throw new AssertionError("Data still exists at " + iid);
                    }
                }
            });
    }

    public static void checkEquals(final CheckEquals function) throws Exception {
        checkEquals(function, TIMEOUT);
    }

    public static void checkEquals(final CheckEquals function, final int timeout) throws Exception {
        await().atMost(Duration.ofSeconds(timeout)).pollInterval(Duration.ofMillis(10)).pollDelay(Duration.ZERO)
            .untilAsserted(function::check);
    }

    public static void checkReceivedMessages(final ListenerCheck listener, final int numberOfMessages) {
        checkReceivedMessages(listener, numberOfMessages, TIMEOUT);
    }

    @VisibleForTesting
    static void checkReceivedMessages(final ListenerCheck listener, final int numberOfMessages,
            final int timeout) {
        await().atMost(Duration.ofSeconds(timeout)).pollInterval(SLEEP_FOR).pollDelay(Duration.ZERO)
                .untilAsserted(() -> {
                    if (listener.getListMessageSize() != numberOfMessages) {
                        throw new AssertionError("Expected " + numberOfMessages + " but received "
                            + listener.getListMessageSize());
                    }
                });
    }

    public interface ListenerCheck {
        int getListMessageSize();
    }

    @FunctionalInterface
    public interface CheckEquals {
        void check() throws ExecutionException, InterruptedException;
    }
}
