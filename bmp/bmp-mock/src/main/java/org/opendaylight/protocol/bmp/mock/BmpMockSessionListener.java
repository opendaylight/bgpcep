/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.mock;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import org.opendaylight.protocol.bmp.api.BmpSession;
import org.opendaylight.protocol.bmp.api.BmpSessionListener;
import org.opendaylight.yangtools.yang.binding.Notification;

public final class BmpMockSessionListener implements BmpSessionListener {
    private final LongAdder counter = new LongAdder();
    private final AtomicBoolean up = new AtomicBoolean(false);

    @Override
    public void onSessionUp(final BmpSession session) {
        up.set(true);
    }

    @Override
    public void onSessionDown(final Exception exception) {
        up.set(false);
    }

    @Override
    public void onMessage(final Notification<?> message) {
        counter.increment();
    }

    public boolean getStatus() {
        return up.get();
    }

    public long getNumberOfMessagesReceived() {
        return counter.longValue();
    }
}
