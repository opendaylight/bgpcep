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
import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.UnsignedInts;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.parser.AsNumberUtil;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.impl.message.open.As4CapabilityHandler;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.PeerRegistryListener;
import org.opendaylight.protocol.bgp.rib.impl.spi.PeerRegistrySessionListener;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapability;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BGP peer registry that allows only 1 session per BGP peer. If a second session with peer is established, one of
 * the sessions will be dropped. The session with lower source BGP id will be dropped. This class is thread-safe.
 */
public final class StrictBGPPeerRegistry implements BGPPeerRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(StrictBGPPeerRegistry.class);

    @GuardedBy("this")
    private final Map<IpAddressNoZone, BGPSessionListener> peers = new HashMap<>();
    @GuardedBy("this")
    private final Map<IpAddressNoZone, BGPSessionId> sessionIds = new HashMap<>();
    @GuardedBy("this")
    private final Map<IpAddressNoZone, BGPSessionPreferences> peerPreferences = new HashMap<>();
    private final Set<PeerRegistryListener> listeners = ConcurrentHashMap.newKeySet();
    private final Set<PeerRegistrySessionListener> sessionListeners = ConcurrentHashMap.newKeySet();

    public static BGPPeerRegistry instance() {
        return new StrictBGPPeerRegistry();
    }

    @Override
    public synchronized void addPeer(final IpAddressNoZone oldIp, final BGPSessionListener peer,
            final BGPSessionPreferences preferences) {
        IpAddressNoZone fullIp = getFullIp(oldIp);
        Preconditions.checkArgument(!this.peers.containsKey(fullIp),
                "Peer for %s already present", fullIp);
        this.peers.put(fullIp, requireNonNull(peer));
        requireNonNull(preferences.getMyAs());
        requireNonNull(preferences.getParams());
        requireNonNull(preferences.getBgpId());
        this.peerPreferences.put(fullIp, preferences);
        for (final PeerRegistryListener peerRegistryListener : this.listeners) {
            peerRegistryListener.onPeerAdded(fullIp, preferences);
        }
    }

    private static IpAddressNoZone getFullIp(final IpAddressNoZone ip) {
        final Ipv6AddressNoZone addr = ip.getIpv6AddressNoZone();
        return addr == null ? ip : new IpAddressNoZone(Ipv6Util.getFullForm(addr));
    }

    @Override
    public synchronized void removePeer(final IpAddressNoZone oldIp) {
        IpAddressNoZone fullIp = getFullIp(oldIp);
        this.peers.remove(fullIp);
        for (final PeerRegistryListener peerRegistryListener : this.listeners) {
            peerRegistryListener.onPeerRemoved(fullIp);
        }
    }

    @Override
    public synchronized void removePeerSession(final IpAddressNoZone oldIp) {
        IpAddressNoZone fullIp = getFullIp(oldIp);
        this.sessionIds.remove(fullIp);
        for (final PeerRegistrySessionListener peerRegistrySessionListener : this.sessionListeners) {
            peerRegistrySessionListener.onSessionRemoved(fullIp);
        }
    }

    @Override
    public boolean isPeerConfigured(final IpAddressNoZone oldIp) {
        IpAddressNoZone fullIp = getFullIp(oldIp);
        return this.peers.containsKey(fullIp);
    }

    private void checkPeerConfigured(final IpAddressNoZone ip) {
        Preconditions.checkState(isPeerConfigured(ip),
                "BGP peer with ip: %s not configured, configured peers are: %s",
                ip, this.peers.keySet());
    }

    @Override
    public synchronized BGPSessionListener getPeer(final IpAddressNoZone ip, final Ipv4AddressNoZone sourceId,
            final Ipv4AddressNoZone remoteId, final Open openObj) throws BGPDocumentedException {
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
                LOG.warn("BGP session with {} {} has to be dropped. Same session already present {}", ip,
                        currentConnection, previousConnection);
                throw new BGPDocumentedException(
                        String.format("BGP session with %s %s has to be dropped. Same session already present %s",
                                ip, currentConnection, previousConnection),
                        BGPError.CEASE);

                // Session reestablished with lower source bgp id, dropping current
            } else if (previousConnection.isHigherDirection(currentConnection)
                    || previousConnection.hasHigherAsNumber(currentConnection)) {
                LOG.warn("BGP session with {} {} has to be dropped. Opposite session already present",
                        ip, currentConnection);
                throw new BGPDocumentedException(
                        String.format("BGP session with %s initiated %s has to be dropped. "
                                + "Opposite session already present", ip, currentConnection), BGPError.CEASE);

                // Session reestablished with higher source bgp id, dropping previous
            } else if (currentConnection.isHigherDirection(previousConnection)
                    || currentConnection.hasHigherAsNumber(previousConnection)) {
                LOG.warn("BGP session with {} {} released. Replaced by opposite session", ip, previousConnection);
                this.peers.get(ip).releaseConnection();
                return this.peers.get(ip);
                // Session reestablished with same source bgp id, dropping current as duplicate
            } else {
                LOG.warn("BGP session with {} initiated from {} to {} has to be dropped. Same session already present",
                        ip, sourceId, remoteId);
                throw new BGPDocumentedException(
                        String.format("BGP session with %s initiated %s has to be dropped. "
                                        + "Same session already present", ip, currentConnection), BGPError.CEASE);
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

    private static void validateAs(final AsNumber remoteAs, final Open openObj, final BGPSessionPreferences localPref)
            throws BGPDocumentedException {
        if (!remoteAs.equals(localPref.getExpectedRemoteAs())) {
            LOG.warn("Unexpected remote AS number. Expecting {}, got {}", localPref.getExpectedRemoteAs(), remoteAs);
            throw new BGPDocumentedException("Peer AS number mismatch", BGPError.BAD_PEER_AS);
        }

        // https://tools.ietf.org/html/rfc6286#section-2.2
        if (openObj.getBgpIdentifier() != null
                && openObj.getBgpIdentifier().getValue().equals(localPref.getBgpId().getValue())) {
            LOG.warn("Remote and local BGP Identifiers are the same: {}", openObj.getBgpIdentifier());
            throw new BGPDocumentedException("Remote and local BGP Identifiers are the same.", BGPError.BAD_BGP_ID);
        }
        final List<BgpParameters> prefs = openObj.getBgpParameters();
        if (prefs != null) {
            final As4BytesCapability localCap = getAs4BytesCapability(localPref.getParams());
            if (localCap != null && getAs4BytesCapability(prefs) == null) {
                throw new BGPDocumentedException("The peer must advertise AS4Bytes capability.",
                        BGPError.UNSUPPORTED_CAPABILITY, serializeAs4BytesCapability(localCap));
            }
            if (!prefs.containsAll(localPref.getParams())) {
                LOG.info("BGP Open message session parameters differ, session still accepted.");
            }
        } else {
            throw new BGPDocumentedException("Open message unacceptable. Check the configuration of BGP speaker.",
                    BGPError.UNSPECIFIC_OPEN_ERROR);
        }
    }

    private static @Nullable As4BytesCapability getAs4BytesCapability(final List<BgpParameters> prefs) {
        for (final BgpParameters param : prefs) {
            for (final OptionalCapabilities capa : param.getOptionalCapabilities()) {
                final CParameters cParam = capa.getCParameters();
                final As4BytesCapability asCapa = cParam.getAs4BytesCapability();
                if (asCapa != null) {
                    return asCapa;
                }
            }
        }
        return null;
    }

    private static byte[] serializeAs4BytesCapability(final As4BytesCapability as4Capability) {
        final ByteBuf buffer = Unpooled.buffer(1 /*CODE*/ + 1 /*LENGTH*/ + Integer.BYTES /*4 byte value*/);
        final As4CapabilityHandler serializer = new As4CapabilityHandler();
        serializer.serializeCapability(new CParametersBuilder().setAs4BytesCapability(as4Capability).build(), buffer);
        return buffer.array();
    }

    @Override
    public BGPSessionPreferences getPeerPreferences(final IpAddressNoZone ip) {
        requireNonNull(ip);
        checkPeerConfigured(ip);
        return this.peerPreferences.get(ip);
    }

    /**
     * Creates IpAddress from SocketAddress. Only InetSocketAddress
     * is accepted with inner address: Inet4Address and Inet6Address.
     *
     * @param socketAddress socket address to transform
     * @return IpAddress equivalent to given socket address
     * @throws IllegalArgumentException if submitted socket address is not InetSocketAddress[ipv4 | ipv6]
     */
    public static IpAddressNoZone getIpAddress(final SocketAddress socketAddress) {
        requireNonNull(socketAddress);
        Preconditions.checkArgument(socketAddress instanceof InetSocketAddress,
                "Expecting InetSocketAddress but was %s", socketAddress.getClass());
        final InetAddress inetAddress = ((InetSocketAddress) socketAddress).getAddress();

        Preconditions.checkArgument(inetAddress instanceof Inet4Address
                || inetAddress instanceof Inet6Address, "Expecting %s or %s but was %s",
                Inet4Address.class, Inet6Address.class, inetAddress.getClass());
        return IetfInetUtil.INSTANCE.ipAddressNoZoneFor(inetAddress);
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
     * remote Open message.
     */
    private static final class BGPSessionId {

        private final Ipv4Address from;
        private final Ipv4Address to;
        private final AsNumber asNumber;

        BGPSessionId(final Ipv4Address from, final Ipv4Address to, final AsNumber asNumber) {
            this.from = requireNonNull(from);
            this.to = requireNonNull(to);
            this.asNumber = requireNonNull(asNumber);
        }

        /**
         * Equals does not take direction of connection into account id1 -> id2 and id2 -> id1 are equal.
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }

            final BGPSessionId bGPSessionId = (BGPSessionId) obj;

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
         * Check if this connection is equal to other and if it contains higher source bgp id.
         */
        boolean isHigherDirection(final BGPSessionId other) {
            return toLong(this.from) > toLong(other.from);
        }

        boolean hasHigherAsNumber(final BGPSessionId other) {
            return this.asNumber.getValue().compareTo(other.asNumber.getValue()) > 0;
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
    public synchronized Registration registerPeerRegisterListener(final PeerRegistryListener listener) {
        this.listeners.add(listener);
        for (final Entry<IpAddressNoZone, BGPSessionPreferences> entry : this.peerPreferences.entrySet()) {
            listener.onPeerAdded(entry.getKey(), entry.getValue());
        }
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                StrictBGPPeerRegistry.this.listeners.remove(listener);
            }
        };
    }

    @Override
    public synchronized Registration registerPeerSessionListener(final PeerRegistrySessionListener listener) {
        this.sessionListeners.add(listener);
        for (final IpAddressNoZone ipAddress : this.sessionIds.keySet()) {
            listener.onSessionCreated(ipAddress);
        }
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                StrictBGPPeerRegistry.this.sessionListeners.remove(listener);
            }
        };
    }

    @Override
    public void updatePeerPreferences(final IpAddressNoZone address, final BGPSessionPreferences preferences) {
        if (this.peerPreferences.containsKey(address)) {
            this.peerPreferences.put(address, preferences);
        }
    }
}
