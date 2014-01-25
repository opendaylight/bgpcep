/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;
import java.util.Comparator;
import java.util.Set;

import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.parser.BGPTerminationReason;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

/**
 * Class representing a peer. We have a single instance for each peer, which provides translation from BGP events into
 * RIB actions.
 */
public final class BGPPeer implements BGPSessionListener, Peer, AutoCloseable {
	private static final Logger LOG = LoggerFactory.getLogger(BGPPeer.class);
	private final Set<TablesKey> tables = Sets.newHashSet();
	private final String name;
	private final RIB rib;
	private Future<Void> cf;
	private BGPSession session;
	private Comparator<PathAttributes> comparator;

	public BGPPeer(final String name, final InetSocketAddress address, final BGPSessionPreferences prefs,
			final RIB rib) {
		this.rib = Preconditions.checkNotNull(rib);
		this.name = Preconditions.checkNotNull(name);
		cf = rib.getDispatcher().createReconnectingClient(address, prefs, this, rib.getTcpStrategyFactory(), rib.getSessionStrategy());
	}

	@Override
	public synchronized void close() {
		if (cf != null) {
			cf.cancel(true);
			if (session != null) {
				session.close();
				session = null;
			}
			cf = null;
		}
	}

	@Override
	public void onMessage(final BGPSession session, final Notification message) {
		if (message instanceof Update) {
			this.rib.updateTables(this, (Update) message);
		} else {
			LOG.info("Ignoring unhandled message class " + message.getClass());
		}
	}

	@Override
	public synchronized void onSessionUp(final BGPSession session) {
		LOG.info("Session with peer {} went up with tables: {}", this.name, session.getAdvertisedTableTypes());

		this.session = session;
		this.comparator = new BGPObjectComparator(rib.getLocalAs(), rib.getBgpIdentifier(), session.getBgpId());

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
		return name;
	}

	@Override
	public Comparator<PathAttributes> getComparator() {
		return this.comparator;
	}
}
