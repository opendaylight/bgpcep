/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.UnsignedInts;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.protocol.bgp.parser.AsNumberUtil;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.impl.message.open.As4CapabilityHandler;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.PeerRegistryListener;
import org.opendaylight.protocol.bgp.rib.impl.spi.PeerRegistrySessionListener;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapability;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BGP peer registry that allows only 1 session per BGP peer.
 * If second session with peer is established, one of the sessions will be dropped.
 * The session with lower source BGP id will be dropped.
 */
@ThreadSafe
public final class StrictBGPPeerRegistry implements BGPPeerRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(StrictBGPPeerRegistry.class);

    @GuardedBy("this")
    private final Map<IpAddress, BGPSessionListener> peers = Maps.newHashMap();
    @GuardedBy("this")
    private final Map<IpAddress, BGPSessionId> sessionIds = Maps.newHashMap();
    @GuardedBy("this")
    private final Map<IpAddress, BGPSessionPreferences> peerPreferences = Maps.newHashMap();
    @GuardedBy("this")
    private final Set<PeerRegistryListener> listeners = new HashSet<>();
    @GuardedBy("this")
    private final Set<PeerRegistrySessionListener> sessionListeners = new HashSet<>();

    public static BGPPeerRegistry instance() {
        return new StrictBGPPeerRegistry();
    }

    @Override
    public synchronized void addPeer(final IpAddress ip, final BGPSessionListener peer, final BGPSessionPreferences preferences) {
        requireNonNull(ip);
        Preconditions.checkArgument(!this.peers.containsKey(ip), "Peer for %s already present", ip);
        this.peers.put(ip, requireNonNull(peer));
        requireNonNull(preferences.getMyAs());
        requireNonNull(preferences.getHoldTime());
        requireNonNull(preferences.getParams());
        requireNonNull(preferences.getBgpId());
        this.peerPreferences.put(ip, preferences);
        for (final PeerRegistryListener peerRegistryListener : this.listeners) {
            peerRegistryListener.onPeerAdded(ip, preferences);
        }
    }

    @Override
    public synchronized void removePeer(final IpAddress ip) {
        requireNonNull(ip);
        this.peers.remove(ip);
        for (final PeerRegistryListener peerRegistryListener : this.listeners) {
            peerRegistryListener.onPeerRemoved(ip);
        }
    }

    @Override
    public synchronized void removePeerSession(final IpAddress ip) {
        requireNonNull(ip);
        this.sessionIds.remove(ip);
        for (final PeerRegistrySessionListener peerRegistrySessionListener : this.sessionListeners) {
            peerRegistrySessionListener.onSessionRemoved(ip);
        }
    }

    @Override
    public boolean isPeerConfigured(final IpAddress ip) {
        requireNonNull(ip);
        return this.peers.containsKey(ip);
    }

    private void checkPeerConfigured(final IpAddress ip) {
        Preconditions.checkState(isPeerConfigured(ip), "BGP peer with ip: %s not configured, configured peers are: %s", ip, this.peers.keySet());
    }

    @Override
    public synchronized BGPSessionListener getPeer(final IpAddress ip, final Ipv4Address sourceId,
        final Ipv4Address remoteId, final Open openObj) throws BGPDocumentedException {
        requireNonNull(ip);
        requireNonNull(sourceId);
        requireNonNull(remoteId);
        final AsNumber remoteAsNumber = AsNumberUtil.advertizedAsNumber(openObj);
        requireNonNull(remoteAsNumber);

        final BGPSessionPreferences prefs = getPeerPreferences(ip);

        checkPeerConfigured(ip);

        final BGPSessionId currentConnection = new BGPSessionId(sourceId, remoteId, remoteAsNumber);
        final BGPSessionListener p = this.peers.get(ip);

        final BGPSessionId previousConnection = this.sessionIds.get(ip);

        if (previousConnection != null) {

            LOG.warn("Duplicate BGP session established with {}", ip);

            // Session reestablished with different ids
            if (!previousConnection.equals(currentConnection)) {
                LOG.warn("BGP session with {} {} has to be dropped. Same session already present {}", ip, currentConnection, previousConnection);
                throw new BGPDocumentedException(
                    String.format("BGP session with %s %s has to be dropped. Same session already present %s",
                        ip, currentConnection, previousConnection),
                        BGPError.CEASE);

                // Session reestablished with lower source bgp id, dropping current
            } else if (previousConnection.isHigherDirection(currentConnection) ||
                    previousConnection.hasHigherAsNumber(currentConnection)) {
                LOG.warn("BGP session with {} {} has to be dropped. Opposite session already present", ip, currentConnection);
                throw new BGPDocumentedException(
                    String.format("BGP session with %s initiated %s has to be dropped. Opposite session already present",
                        ip, currentConnection),
                        BGPError.CEASE);

                // Session reestablished with higher source bgp id, dropping previous
            } else if (currentConnection.isHigherDirection(previousConnection) ||
                    currentConnection.hasHigherAsNumber(previousConnection)) {
                LOG.warn("BGP session with {} {} released. Replaced by opposite session", ip, previousConnection);
                this.peers.get(ip).releaseConnection();
                return this.peers.get(ip);
                // Session reestablished with same source bgp id, dropping current as duplicate
            } else {
                LOG.warn("BGP session with %s initiated from %s to %s has to be dropped. Same session already present", ip, sourceId, remoteId);
                throw new BGPDocumentedException(
                    String.format("BGP session with %s initiated %s has to be dropped. Same session already present",
                        ip, currentConnection),
                        BGPError.CEASE);
            }
        }
        validateAs(remoteAsNumber, openObj, prefs);

        // Map session id to peer IP address
        this.sessionIds.put(ip, currentConnection);
        for (final PeerRegistrySessionListener peerRegistrySessionListener : this.sessionListeners) {
            peerRegistrySessionListener.onSessionCreated(ip);
        }
        return p;
    }

    private static void validateAs(final AsNumber remoteAs, final Open openObj, final BGPSessionPreferences localPref) throws BGPDocumentedException {
        if (!remoteAs.equals(localPref.getExpectedRemoteAs())) {
            LOG.warn("Unexpected remote AS number. Expecting {}, got {}", remoteAs, localPref.getExpectedRemoteAs());
            throw new BGPDocumentedException("Peer AS number mismatch", BGPError.BAD_PEER_AS);
        }

        // https://tools.ietf.org/html/rfc6286#section-2.2
        if (openObj.getBgpIdentifier() != null && openObj.getBgpIdentifier().getValue().equals(localPref.getBgpId().getValue())) {
            LOG.warn("Remote and local BGP Identifiers are the same: {}", openObj.getBgpIdentifier());
            throw new BGPDocumentedException("Remote and local BGP Identifiers are the same.", BGPError.BAD_BGP_ID);
        }
        final List<BgpParameters> prefs = openObj.getBgpParameters();
        if (prefs != null) {
            if (getAs4BytesCapability(localPref.getParams()).isPresent() && !getAs4BytesCapability(prefs).isPresent()) {
                throw new BGPDocumentedException("The peer must advertise AS4Bytes capability.", BGPError.UNSUPPORTED_CAPABILITY, serializeAs4BytesCapability(getAs4BytesCapability(localPref.getParams()).get()));
            }
            if (!prefs.containsAll(localPref.getParams())) {
                LOG.info("BGP Open message session parameters differ, session still accepted.");
            }
        } else {
            throw new BGPDocumentedException("Open message unacceptable. Check the configuration of BGP speaker.", BGPError.UNSPECIFIC_OPEN_ERROR);
        }
    }

    private static Optional<As4BytesCapability> getAs4BytesCapability(final List<BgpParameters> prefs) {
        for (final BgpParameters param : prefs) {
            for (final OptionalCapabilities capa : param.getOptionalCapabilities()) {
                final CParameters cParam = capa.getCParameters();
                if (cParam.getAs4BytesCapability() != null) {
                    return Optional.of(cParam.getAs4BytesCapability());
                }
            }
        }
        return Optional.absent();
    }

    private static byte[] serializeAs4BytesCapability(final As4BytesCapability as4Capability) {
        final ByteBuf buffer = Unpooled.buffer(1 /*CODE*/ + 1 /*LENGTH*/ + Integer.SIZE / Byte.SIZE /*4 byte value*/);
        final As4CapabilityHandler serializer = new As4CapabilityHandler();
        serializer.serializeCapability(new CParametersBuilder().setAs4BytesCapability(as4Capability).build(), buffer);
        return buffer.array();
    }

    @Override
    public BGPSessionPreferences getPeerPreferences(final IpAddress ip) {
        requireNonNull(ip);
        checkPeerConfigured(ip);
        return this.peerPreferences.get(ip);
    }

    /**
     * Creates IpAddress from SocketAddress. Only InetSocketAddress is accepted with inner address: Inet4Address and Inet6Address.
     *
     * @param socketAddress socket address to transform
     * @return IpAddress equivalent to given socket address
     * @throws IllegalArgumentException if submitted socket address is not InetSocketAddress[ipv4 | ipv6]
     */
    public static IpAddress getIpAddress(final SocketAddress socketAddress) {
        requireNonNull(socketAddress);
        Preconditions.checkArgument(socketAddress instanceof InetSocketAddress, "Expecting InetSocketAddress but was %s", socketAddress.getClass());
        final InetAddress inetAddress = ((InetSocketAddress) socketAddress).getAddress();

        Preconditions.checkArgument(inetAddress instanceof Inet4Address || inetAddress instanceof Inet6Address, "Expecting %s or %s but was %s", Inet4Address.class, Inet6Address.class, inetAddress.getClass());
        return IetfInetUtil.INSTANCE.ipAddressFor(inetAddress);
    }

    @Override
    public synchronized void close() {
        this.peers.clear();
        this.sessionIds.clear();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("peers", this.peers.keySet())
            .toString();
    }

    /**
     * Session identifier that contains (source Bgp Id) -> (destination Bgp Id) AsNumber is the remoteAs coming from
     * remote Open message
     */
    private static final class BGPSessionId {

        private final Ipv4Address from, to;
        private final AsNumber asNumber;

        BGPSessionId(final Ipv4Address from, final Ipv4Address to, final AsNumber asNumber) {
            this.from = requireNonNull(from);
            this.to = requireNonNull(to);
            this.asNumber = requireNonNull(asNumber);
        }

        /**
         * Equals does not take direction of connection into account id1 -> id2 and id2 -> id1 are equal
         */
        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final BGPSessionId bGPSessionId = (BGPSessionId) o;

            if (!this.from.equals(bGPSessionId.from) && !this.from.equals(bGPSessionId.to)) {
                return false;
            }
            if (!this.to.equals(bGPSessionId.to) && !this.to.equals(bGPSessionId.from)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = this.from.hashCode() + this.to.hashCode();
            result = prime * result;
            return result;
        }

        /**
         * Check if this connection is equal to other and if it contains higher source bgp id
         */
        boolean isHigherDirection(final BGPSessionId other) {
            return toLong(this.from) > toLong(other.from);
        }

        boolean hasHigherAsNumber(final BGPSessionId other) {
            return this.asNumber.getValue() > other.asNumber.getValue();
        }

        private static long toLong(final Ipv4Address from) {
            final int i = InetAddresses.coerceToInteger(InetAddresses.forString(from.getValue()));
            return UnsignedInts.toLong(i);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("from", this.from)
                .add("to", this.to)
                .toString();
        }
    }

    @Override
    public synchronized AutoCloseable registerPeerRegisterListener(final PeerRegistryListener listener) {
        this.listeners.add(listener);
        for (final Entry<IpAddress, BGPSessionPreferences> entry : this.peerPreferences.entrySet()) {
            listener.onPeerAdded(entry.getKey(), entry.getValue());
        }
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (StrictBGPPeerRegistry.this) {
                    StrictBGPPeerRegistry.this.listeners.remove(listener);
                }
            }
        };
    }

    @Override
    public synchronized AutoCloseable registerPeerSessionListener(final PeerRegistrySessionListener listener) {
        this.sessionListeners.add(listener);
        for (final IpAddress ipAddress : this.sessionIds.keySet()) {
            listener.onSessionCreated(ipAddress);
        }
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (StrictBGPPeerRegistry.this) {
                    StrictBGPPeerRegistry.this.sessionListeners.remove(listener);
                }
            }
        };
    }
}
