/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import org.opendaylight.protocol.bgp.concepts.BGPTableType;
import org.opendaylight.protocol.bgp.parser.BGPMessage;
import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.parser.BGPTerminationReason;
import org.opendaylight.protocol.bgp.parser.BGPUpdateMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Class representing a peer. We have a single instance for each peer, which provides translation from BGP events into
 * RIB actions.
 */
public final class BGPPeer implements BGPSessionListener {
	private static final Logger logger = LoggerFactory.getLogger(BGPPeer.class);
	private Set<BGPTableType> tables = Sets.newHashSet();
	private final String name;
	private final RIBImpl rib;

	public BGPPeer(final RIBImpl rib, final String name) {
		this.rib = Preconditions.checkNotNull(rib);
		this.name = Preconditions.checkNotNull(name);
	}

	@Override
	public void onMessage(final BGPSession session, final BGPMessage message) {
		if (message instanceof BGPUpdateMessage) {
			final BGPUpdateMessage m = (BGPUpdateMessage) message;
			this.rib.updateTables(this, m.getAddedObjects(), m.getRemovedObjects());
		} else {
			logger.info("Ignoring unhandled message class " + message.getClass());
		}
	}

	@Override
	public void onSessionUp(final BGPSession session) {
		logger.info("Session with peer {} went up with tables: {}", this.name, session.getAdvertisedTableTypes());
	}

	private void cleanup() {
		// FIXME: support graceful restart
		for (final BGPTableType t : this.tables) {
			this.rib.clearTable(this, t);
		}
	}

	@Override
	public void onSessionDown(final BGPSession session, final Exception e) {
		logger.info("Session with peer {} went down", this.name, e);
		cleanup();
	}

	@Override
	public void onSessionTerminated(final BGPSession session, final BGPTerminationReason cause) {
		logger.info("Session with peer {} terminated: {}", this.name, cause);
		cleanup();
	}

	@Override
	public final String toString() {
		return addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("name", this.name);
		toStringHelper.add("tables", this.tables);
		return toStringHelper;
	}
}
