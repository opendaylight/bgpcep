package org.opendaylight.protocol.pcep.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.primitives.UnsignedBytes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.framework.AbstractSessionNegotiator;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCEPSessionNegotiator extends AbstractSessionNegotiator<Message, PCEPSessionImpl> {

    private static final Logger LOG = LoggerFactory.getLogger(PCEPSessionNegotiator.class);

    private static final Comparator<byte[]> COMPARATOR = UnsignedBytes.lexicographicalComparator();

    /**
     * The maximum lifetime for which we should hold on to a session ID before assuming it is okay to reuse it.
     */
    private static final long ID_CACHE_SECONDS = 3 * 3600;

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

    @GuardedBy("this")
    private final Cache<byte[], PeerRecord> formerClients = CacheBuilder.newBuilder().expireAfterAccess(PEER_CACHE_SECONDS,
            TimeUnit.SECONDS).maximumSize(PEER_CACHE_SIZE).build();

    private final Channel channel;

    private final Promise<PCEPSessionImpl> promise;

    private final SessionListenerFactory<PCEPSessionListener> factory;

    private final AbstractPCEPSessionNegotiatorFactory negFactory;

    @GuardedBy("this")
    private final BiMap<byte[], SessionReference> sessions = HashBiMap.create();

    private interface SessionReference extends AutoCloseable {
        Short getSessionId();
    }

    public PCEPSessionNegotiator(final Channel channel, final Promise<PCEPSessionImpl> promise, final SessionListenerFactory<PCEPSessionListener> factory,
        final AbstractPCEPSessionNegotiatorFactory negFactory) {
        super(promise, channel);
        this.channel = channel;
        this.promise = promise;
        this.factory = factory;
        this.negFactory = negFactory;
    }

    @Override
    protected void startNegotiation() throws ExecutionException {
        final Object lock = this;

        LOG.debug("Bootstrap negotiation for channel {} started", this.channel);

        /*
         * We have a chance to see if there's a client session already
         * registered for this client.
         */
        final byte[] clientAddress = ((InetSocketAddress) this.channel.remoteAddress()).getAddress().getAddress();

        synchronized (lock) {
            if (this.sessions.containsKey(clientAddress)) {
                final byte[] serverAddress = ((InetSocketAddress) this.channel.localAddress()).getAddress().getAddress();
                if (COMPARATOR.compare(serverAddress, clientAddress) > 0) {
                    final SessionReference n = this.sessions.remove(clientAddress);
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
            final AbstractPCEPSessionNegotiator n = this.negFactory.createNegotiator(this.promise, this.factory.getSessionListener(), this.channel, sessionId);

            this.sessions.put(clientAddress, new SessionReference() {
                @Override
                public void close() throws ExecutionException {
                    try {
                        PCEPSessionNegotiator.this.formerClients.get(clientAddress, new Callable<PeerRecord>() {
                            @Override
                            public PeerRecord call() {
                                return new PeerRecord(ID_CACHE_SECONDS, getSessionId());
                            }
                        });
                    } finally {
                        PCEPSessionNegotiator.this.channel.close();
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
                        PCEPSessionNegotiator.this.sessions.inverse().remove(this);
                    }
                }
            });

            LOG.info("Replacing bootstrap negotiator for channel {}", this.channel);
            this.channel.pipeline().replace(this, "negotiator", n);
            n.startNegotiation();
        }
    }

    @GuardedBy("this")
    protected Short nextSession(final byte[] clientAddress) throws ExecutionException {
        final PeerRecord peer = this.formerClients.get(clientAddress, new Callable<PeerRecord>() {
            @Override
            public PeerRecord call() {
                return new PeerRecord(ID_CACHE_SECONDS, null);
            }
        });

        return peer.allocId();
    }

    @Override
    protected void handleMessage(final Message msg) {
        throw new IllegalStateException("Bootstrap negotiator should have been replaced");
    }
}
