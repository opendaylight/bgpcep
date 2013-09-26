/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.protocol.bgp.concepts.BGPObject;
import org.opendaylight.protocol.bgp.concepts.BGPTableType;
import org.opendaylight.protocol.bgp.parser.BGPLink;
import org.opendaylight.protocol.bgp.parser.BGPNode;
import org.opendaylight.protocol.bgp.parser.BGPPrefix;
import org.opendaylight.protocol.bgp.parser.BGPRoute;
import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.parser.BGPUpdateMessage;
import org.opendaylight.protocol.bgp.parser.BGPUpdateSynchronized;
import org.opendaylight.protocol.bgp.util.BGPIPv4RouteImpl;
import org.opendaylight.protocol.bgp.util.BGPIPv6RouteImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpSubsequentAddressFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * BGP speaker (without Graceful restart capability) sends KeepAlive message after sending all initial Update messages
 * with certain AFI/SAFI. For each AFI/SAFI, it sends one KA message. As it is undetermined which KA message belongs to
 * which AFI/SAFI, an algorithm needed to be implemented.
 */
public class BGPSynchronization {

	private static final Logger logger = LoggerFactory.getLogger(BGPSynchronization.class);

	private static class SyncVariables {

		private boolean upd = false;
		private boolean eor = false;

		public void setUpd(final boolean upd) {
			this.upd = upd;
		}

		public void setEorTrue() {
			this.eor = true;
		}

		public boolean getEor() {
			return this.eor;
		}

		public boolean getUpd() {
			return this.upd;
		}
	}

	private final Map<BGPTableType, SyncVariables> syncStorage = Maps.newHashMap();

	private final BGPSessionListener listener;

	private final BGPSession session;

	public BGPSynchronization(final BGPSession session, final BGPSessionListener listener, final Set<BGPTableType> types) {
		this.listener = Preconditions.checkNotNull(listener);
		this.session = Preconditions.checkNotNull(session);

		for (final BGPTableType type : types) {
			this.syncStorage.put(type, new SyncVariables());
		}
	}

	/**
	 * For each received Update message, the upd sync variable needs to be updated to true, for particular AFI/SAFI
	 * combination. Currently we only assume Unicast SAFI. From the Update message we have to extract the AFI. Each
	 * Update message can contain BGP Object with one type of AFI. If the object is BGP Link, BGP Node or BGPPrefix<?>
	 * the AFI is Linkstate. In case of BGPRoute, the AFI depends on the IP Address of the prefix.
	 * 
	 * @param msg received Update message
	 */
	public void updReceived(final BGPUpdateMessage msg) {
		BGPTableType type = null;
		if (!msg.getAddedObjects().isEmpty()) {
			final BGPObject obj = msg.getAddedObjects().iterator().next();
			if (obj instanceof BGPRoute) {
				if ((BGPRoute) obj instanceof BGPIPv4RouteImpl) {
					type = new BGPTableType(BgpAddressFamily.Ipv4, BgpSubsequentAddressFamily.Unicast);
				} else if ((BGPRoute) obj instanceof BGPIPv6RouteImpl) {
					type = new BGPTableType(BgpAddressFamily.Ipv6, BgpSubsequentAddressFamily.Unicast);
				}
			} else if (obj instanceof BGPLink || obj instanceof BGPNode || obj instanceof BGPPrefix<?>) {
				type = new BGPTableType(BgpAddressFamily.Linkstate, BgpSubsequentAddressFamily.Linkstate);
			}
		}
		final SyncVariables s = this.syncStorage.get(type);
		if (s == null) {
			logger.warn("BGPTableType was not present in open message : {}", type);
			return;
		}
		s.setUpd(true);
	}

	/**
	 * This method is called, when the second KA message is received. It checks each AFI/SAFI sync variables. If they
	 * are all false, which means, that there was at least one update message followed by one KA, the EOR is sent to
	 * session.
	 */
	public void kaReceived() {
		for (final Entry<BGPTableType, SyncVariables> entry : this.syncStorage.entrySet()) {
			final SyncVariables s = entry.getValue();
			if (!s.getEor()) {
				if (!s.getUpd()) {
					s.setEorTrue();
					final BGPUpdateSynchronized up = generateEOR(entry.getKey());
					logger.debug("Sending synchronization message: {}", up);
					this.listener.onMessage(this.session, up);
				}
				s.setUpd(false);
			}
		}
	}

	private BGPUpdateSynchronized generateEOR(final BGPTableType type) {
		return new BGPUpdateSynchronizedImpl(type);
	}
}
