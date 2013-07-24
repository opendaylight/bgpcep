/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.Set;

import org.opendaylight.protocol.bgp.concepts.BGPTableType;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPMessage;
import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.parser.BGPUpdateMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

/**
 * Class representing a peer. We have a single instance for each peer, which provides translation from BGP events into
 * RIB actions.
 */
public final class BGPPeer extends BGPSessionListener {
	private static final Logger logger = LoggerFactory.getLogger(BGPPeer.class);
	private Set<BGPTableType> tables;
	private final String name;
	private final RIBImpl rib;

	public BGPPeer(final RIBImpl rib, final String name) {
		this.rib = Preconditions.checkNotNull(rib);
		this.name = Preconditions.checkNotNull(name);
	}

	@Override
	public void onMessage(final BGPMessage message) {
		if (message instanceof BGPUpdateMessage) {
			final BGPUpdateMessage m = (BGPUpdateMessage) message;
			this.rib.updateTables(this, m.getAddedObjects(), m.getRemovedObjects());
		} else
			logger.info("Ignoring unhandled message class " + message.getClass());
	}

	@Override
	public void onSessionUp(final Set<BGPTableType> remoteParams) {
		logger.info("Session with peer {} went up with tables: {}", this.name, remoteParams);
	}

	@Override
	public void onSessionDown(final BGPSession session, final Exception e) {
		// FIXME: support graceful restart
		for (final BGPTableType t : this.tables)
			this.rib.clearTable(this, t);
	}

	@Override
	public void onSessionTerminated(final BGPError cause) {
		logger.info("Session with peer {} terminated", this.name);
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
