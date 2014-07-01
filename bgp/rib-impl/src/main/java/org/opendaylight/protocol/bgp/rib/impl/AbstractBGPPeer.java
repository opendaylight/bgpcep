/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.parser.BGPTerminationReason;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class representing a peer. We have a single instance for each peer, which provides translation from BGP events into
 * RIB actions.
 */
public abstract class AbstractBGPPeer implements BGPSessionListener, Peer, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractBGPPeer.class);

    @GuardedBy("this")
    private final Set<TablesKey> tables = new HashSet<>();
    private final RIB rib;
    private final BGPPeerRegistry sessionRegistry;

    private Comparator<PathAttributes> comparator;
    private BGPSession session;
    // Volatile for memory visibility. Name is set in onSessionUp() and read in non synchronized methods.
    private volatile String name;

    public AbstractBGPPeer(final String name, final RIB rib, final BGPPeerRegistry sessionRegistry) {
        this.rib = Preconditions.checkNotNull(rib);
        this.sessionRegistry = Preconditions.checkNotNull(sessionRegistry);
        this.name = name;
    }

    @Override
    public synchronized void close() {
        if (this.session != null) {
            this.session.close();
            this.session = null;
        }
    }

    @Override
    public void drop() {
        onSessionTerminated(session, new BGPTerminationReason(BGPError.CEASE));
        close();
    }

    @Override
    public void onMessage(final BGPSession session, final Notification message) {
        if (message instanceof Update) {
            this.rib.updateTables(this, (Update) message);
        } else {
            LOG.info("Ignoring unhandled message class {}", message.getClass());
        }
    }

    @Override
    public synchronized void onSessionUp(final BGPSession session) {
        // Add session to global session registry
        final Ipv4Address sourceBgpId = getSourceBgpId(session, rib);
        final Ipv4Address destinationBgpId = getDestinationBgpId(session, rib);

        switch (sessionRegistry.peerUp(this, sourceBgpId, destinationBgpId)) {
        case DUPLICATE : {
            LOG.warn("Session from {} to {} already present, dropping connection", sourceBgpId, destinationBgpId);
            onSessionTerminated(session, new BGPTerminationReason(BGPError.CEASE));
            return;
        }
        case DROPPED : {
            LOG.warn("Session from {} to {} already present in opposite direction, dropping connection due to lower source bgpId", sourceBgpId, destinationBgpId);
            onSessionTerminated(session, new BGPTerminationReason(BGPError.CEASE));
            return;
        }
        case DROPPED_PREVIOUS: {
            LOG.warn("Session from {} to {} already present in opposite direction, dropping connection from {} to {} due to lower source bgpId", sourceBgpId, destinationBgpId, destinationBgpId, sourceBgpId);
            // TODO Rib needs to be updated from other
        }
        }

        // Update generic name for bgp peer to its bgp ID
        updateName(name + ":" + sourceBgpId.toString());

        LOG.info("Session with peer {} went up with tables: {}", this.name, session.getAdvertisedTableTypes());

        this.session = session;
        this.comparator = new BGPObjectComparator(this.rib.getLocalAs(), destinationBgpId, session.getBgpId());

        for (final BgpTableType t : session.getAdvertisedTableTypes()) {
            final TablesKey key = new TablesKey(t.getAfi(), t.getSafi());

            this.tables.add(key);
            this.rib.initTable(this, key);
        }
    }

    protected abstract Ipv4Address getSourceBgpId(BGPSession session, RIB rib);
    protected abstract Ipv4Address getDestinationBgpId(BGPSession session, RIB rib);

    protected synchronized void updateName(final String name) {
        this.name = Preconditions.checkNotNull(name);
    }

    private synchronized void cleanup() {
        // FIXME: BUG-196: support graceful restart
        for (final TablesKey key : this.tables) {
            this.rib.clearTable(this, key);
        }

        this.tables.clear();
        this.session = null;
        this.comparator = null;
    }

    @Override
    public void onSessionDown(final BGPSession session, final Exception e) {
        LOG.info("Session with peer {} went down", this.name, e);
        cleanup();
    }

    @Override
    public void onSessionTerminated(final BGPSession session, final BGPTerminationReason cause) {
        LOG.info("Session with peer {} terminated: {}", this.name, cause);
        cleanup();
    }

    @Override
    public String toString() {
        return addToStringAttributes(Objects.toStringHelper(this)).toString();
    }

    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        toStringHelper.add("name", this.name);
        toStringHelper.add("tables", this.tables);
        return toStringHelper;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Comparator<PathAttributes> getComparator() {
        return this.comparator;
    }

    protected RIB getRib() {
        return rib;
    }
}
