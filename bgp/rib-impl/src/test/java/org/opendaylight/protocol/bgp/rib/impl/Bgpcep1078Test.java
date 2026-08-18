/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.UpdateBuilder;
import org.opendaylight.yangtools.binding.Notification;

/**
 * Tests {@link LocRibWriter} fan-out pacing. Within one route change batch every peer is served independently.
 * Across batches the writer keeps at most one outstanding AdjRibsOut commit per peer. A peer is served again
 * only after its previous commit completes.
 */
@ExtendWith(MockitoExtension.class)
class Bgpcep1078Test {
    @Mock
    private BGPSessionImpl limiterSession;
    @Mock
    private ChannelHandlerContext limiterContext;
    @Mock
    private Channel limiterChannel;

    /*
     * The session reports itself unwritable and no writability event is delivered. A write method has to wait. It
     * reaches the session once the session reports itself writable and the event fires.
     */
    @Test
    void testLimiterWaitsOnCurrentChannelState() throws Exception {
        final var writable = new AtomicBoolean(false);
        doAnswer(inv -> writable.get()).when(limiterSession).isWritable();
        doNothing().when(limiterSession).write(any(Notification.class));
        doNothing().when(limiterSession).flush();
        doReturn(limiterChannel).when(limiterContext).channel();
        doReturn(true).when(limiterChannel).isWritable();
        // The limiter passes the event on to the rest of the pipeline
        doReturn(limiterContext).when(limiterContext).fireChannelWritabilityChanged();

        final var limiter = new ChannelOutputLimiter(limiterSession);
        final var writer = Executors.newSingleThreadExecutor();
        try {
            final var written = writer.submit(() -> limiter.write(new UpdateBuilder().build()));
            // No event was delivered, the state of the channel is all the limiter has to go on
            verify(limiterSession, after(500).never()).write(any(Notification.class));
            assertFalse(written.isDone());

            // Now the channel reports itself writable again and the event wakes the waiting writer up
            writable.set(true);
            limiter.channelWritabilityChanged(limiterContext);

            written.get(5, TimeUnit.SECONDS);
            verify(limiterSession).write(any(Notification.class));
        } finally {
            writer.shutdownNow();
        }
    }
}
