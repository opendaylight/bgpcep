/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.primitives.UnsignedBytes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.framework.AbstractSessionNegotiator;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiator;
import org.opendaylight.protocol.framework.SessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SessionNegotiator which takes care of making sure sessions between PCEP peers are kept unique. This needs to be
 * further subclassed to provide either a client or server factory.
 */
public abstract class AbstractPCEPSessionNegotiatorFactory implements
        SessionNegotiatorFactory<Message, PCEPSessionImpl, PCEPSessionListener> {
    private static final Comparator<byte[]> COMPARATOR = UnsignedBytes.lexicographicalComparator();
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPCEPSessionNegotiatorFactory.class);

    /**
     * The total amount of time we should remember a peer having been present, unless some other pressure forces us to
     * forget about it due to {@link PEER_CACHE_SIZE}.
     */
    private static final long PEER_CACHE_SECONDS = 24 * 3600;

    /**
     * Maximum total number of peers we keep track of. Combined with {@link PEER_CACHE_SECONDS}, this defines how many
     * peers we can see turn around.
     */
    private static final long PEER_CACHE_SIZE = 1024;

    /**
     * The maximum lifetime for which we should hold on to a session ID before assuming it is okay to reuse it.
     */
    private static final long ID_CACHE_SECONDS = 3 * 3600;

    @GuardedBy("this")
    private final Map<ByteArrayWrapper, SessionReference> sessions = new HashMap<>();

    @GuardedBy("this")
    private final Cache<byte[], PeerRecord> formerClients = CacheBuilder.newBuilder().expireAfterAccess(PEER_CACHE_SECONDS,
            TimeUnit.SECONDS).maximumSize(PEER_CACHE_SIZE).build();

    private interface SessionReference extends AutoCloseable {
        Short getSessionId();
    }

    /**
     * Create a new negotiator. This method needs to be implemented by subclasses to actually provide a negotiator.
     *
     * @param promise Session promise to be completed by the negotiator
     * @param channel Associated channel
     * @param sessionId Session ID assigned to the resulting session
     * @return a PCEP session negotiator
     */
    protected abstract AbstractPCEPSessionNegotiator createNegotiator(Promise<PCEPSessionImpl> promise, PCEPSessionListener listener,
            Channel channel, short sessionId);

    @Override
    public final SessionNegotiator<PCEPSessionImpl> getSessionNegotiator(final SessionListenerFactory<PCEPSessionListener> factory,
            final Channel channel, final Promise<PCEPSessionImpl> promise) {

        final Object lock = this;

        LOG.debug("Instantiating bootstrap negotiator for channel {}", channel);
        return new AbstractSessionNegotiator<Message, PCEPSessionImpl>(promise, channel) {
            @Override
            protected void startNegotiation() throws ExecutionException {
                LOG.debug("Bootstrap negotiation for channel {} started", this.channel);

                /*
                 * We have a chance to see if there's a client session already
                 * registered for this client.
                 */
                final byte[] clientAddress = ((InetSocketAddress) this.channel.remoteAddress()).getAddress().getAddress();
                final ByteArrayWrapper clientAddressWrapper = new ByteArrayWrapper(clientAddress);

                synchronized (lock) {
                    if (AbstractPCEPSessionNegotiatorFactory.this.sessions.containsKey(clientAddressWrapper)) {
                        final byte[] serverAddress = ((InetSocketAddress) this.channel.localAddress()).getAddress().getAddress();
                        if (COMPARATOR.compare(serverAddress, clientAddress) > 0) {
                            final SessionReference n = AbstractPCEPSessionNegotiatorFactory.this.sessions.remove(clientAddressWrapper);
                            try {
                                n.close();
                            } catch (final Exception e) {
                                LOG.error("Unexpected failure to close old session", e);
                            }
                        } else {
                            negotiationFailed(new IllegalStateException("A conflicting session for address "
                                    + ((InetSocketAddress) this.channel.remoteAddress()).getAddress() + " found."));
                            return;
                        }
                    }

                    final Short sessionId = nextSession(clientAddress);
                    final AbstractPCEPSessionNegotiator n = createNegotiator(promise, factory.getSessionListener(), this.channel, sessionId);

                    AbstractPCEPSessionNegotiatorFactory.this.sessions.put(clientAddressWrapper, new SessionReference() {
                        @Override
                        public void close() throws ExecutionException {
                            try {
                                formerClients.get(clientAddress, new Callable<PeerRecord>() {
                                    @Override
                                    public PeerRecord call() {
                                        return new PeerRecord(ID_CACHE_SECONDS, getSessionId());
                                    }
                                });
                            } finally {
                                channel.close();
                            }
                        }

                        @Override
                        public Short getSessionId() {
                            return sessionId;
                        }
                    });

                    this.channel.closeFuture().addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(final ChannelFuture future) {
                            synchronized (lock) {
                                AbstractPCEPSessionNegotiatorFactory.this.sessions.remove(clientAddressWrapper);
                            }
                        }
                    });

                    LOG.info("Replacing bootstrap negotiator for channel {}", this.channel);
                    this.channel.pipeline().replace(this, "negotiator", n);
                    n.startNegotiation();
                }
            }

            @Override
            protected void handleMessage(final Message msg) {
                throw new IllegalStateException("Bootstrap negotiator should have been replaced");
            }
        };
    }

    @GuardedBy("this")
    private Short nextSession(final byte[] clientAddress) throws ExecutionException {
        final PeerRecord peer = formerClients.get(clientAddress, new Callable<PeerRecord>() {
            @Override
            public PeerRecord call() {
                return new PeerRecord(ID_CACHE_SECONDS, null);
            }
        });

        return peer.allocId();
    }

    private static final class ByteArrayWrapper {

        final byte[] byteArray;

        ByteArrayWrapper(final byte[] byteArray) {
            this.byteArray = byteArray;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(byteArray);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ByteArrayWrapper other = (ByteArrayWrapper) obj;
            if (!Arrays.equals(byteArray, other.byteArray))
                return false;
            return true;
        }
    }
}
