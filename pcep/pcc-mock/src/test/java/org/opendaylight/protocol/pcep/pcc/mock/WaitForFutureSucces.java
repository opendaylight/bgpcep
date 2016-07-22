/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import com.google.common.util.concurrent.Uninterruptibles;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

final class WaitForFutureSucces {
    private WaitForFutureSucces() {
        throw new UnsupportedOperationException();
    }

    static <T extends Future> void waitFutureSuccess(final T future) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        future.addListener(new FutureListener() {
            @Override
            public void operationComplete(final Future future) throws Exception {
                latch.countDown();
            }
        });
        Uninterruptibles.awaitUninterruptibly(latch, 10, TimeUnit.SECONDS);
    }
}
