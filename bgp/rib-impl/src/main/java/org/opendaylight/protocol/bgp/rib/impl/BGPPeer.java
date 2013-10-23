/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.Set;

import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.parser.BGPTerminationReason;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

/**
 * Class representing a peer. We have a single instance for each peer, which provides translation from BGP events into
 * RIB actions.
 */
public final class BGPPeer implements BGPSessionListener, Peer {
	private static final Logger logger = LoggerFactory.getLogger(BGPPeer.class);
	private Set<TablesKey> tables;
	private final String name;
	private final RIBImpl rib;

	public BGPPeer(final RIBImpl rib, final String name) {
		this.rib = Preconditions.checkNotNull(rib);
		this.name = Preconditions.checkNotNull(name);
	}

	@Override
	public void onMessage(final BGPSession session, final Notification message) {
		if (message instanceof Update) {
			this.rib.updateTables(this, (Update) message);
		} else {
			logger.info("Ignoring unhandled message class " + message.getClass());
		}
	}

	@Override
	public void onSessionUp(final BGPSession session) {
		logger.info("Session with peer {} went up with tables: {}", this.name, session.getAdvertisedTableTypes());

		for (final BgpTableType t : session.getAdvertisedTableTypes()) {
			this.tables.add(new TablesKey(t.getAfi(), t.getSafi()));
		}
	}

	@Override
	public void onSessionDown(final BGPSession session, final Exception e) {
		// FIXME: support graceful restart
		for (final TablesKey key : this.tables) {
			this.rib.clearTable(this, key);
		}

		this.tables.clear();
	}

	@Override
	public void onSessionTerminated(final BGPSession session, final BGPTerminationReason cause) {
		logger.info("Session with peer {} terminated: {}", this.name, cause);
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
}
