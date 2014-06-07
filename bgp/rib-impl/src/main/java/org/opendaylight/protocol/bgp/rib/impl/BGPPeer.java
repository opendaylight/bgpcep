/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.bgpcep.tcpmd5.KeyMapping;
import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.parser.BGPTerminationReason;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
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
public final class BGPPeer implements BGPSessionListener, Peer, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(BGPPeer.class);

    @GuardedBy("this")
    private final Set<TablesKey> tables = new HashSet<>();
    private final String name;
    private final RIB rib;

    private Comparator<PathAttributes> comparator;
    private Future<Void> cf;
    private BGPSession session;

    public BGPPeer(final String name, final InetSocketAddress address, final String password, final BGPSessionPreferences prefs,
            final AsNumber remoteAs, final RIB rib) {
        this.rib = Preconditions.checkNotNull(rib);
        this.name = Preconditions.checkNotNull(name);

        final KeyMapping keys;
        if (password != null) {
            keys = new KeyMapping();
            keys.put(address.getAddress(), password.getBytes(Charsets.US_ASCII));
        } else {
            keys = null;
        }

        this.cf = rib.getDispatcher().createReconnectingClient(address, prefs, remoteAs, this, rib.getTcpStrategyFactory(),
                rib.getSessionStrategyFactory(), keys);
    }

    @Override
    public synchronized void close() {
        if (this.cf != null) {
            this.cf.cancel(true);
            if (this.session != null) {
                this.session.close();
                this.session = null;
            }
            this.cf = null;
        }
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
        LOG.info("Session with peer {} went up with tables: {}", this.name, session.getAdvertisedTableTypes());

        this.session = session;
        this.comparator = new BGPObjectComparator(this.rib.getLocalAs(), this.rib.getBgpIdentifier(), session.getBgpId());

        for (final BgpTableType t : session.getAdvertisedTableTypes()) {
            final TablesKey key = new TablesKey(t.getAfi(), t.getSafi());

            this.tables.add(key);
            this.rib.initTable(this, key);
        }
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
}
