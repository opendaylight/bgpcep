/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import io.netty.channel.Channel;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

/**
 * Representation of a server, created by {@link Dispatcher}. Should be extended by a protocol specific server
 * implementation.
 */
public final class ProtocolServer implements SessionParent {

	private static final Logger logger = LoggerFactory.getLogger(ProtocolServer.class);

	private static final int SESSIONS_LIMIT = 255;

	private final InetSocketAddress serverAddress;

	private final ProtocolConnectionFactory connectionFactory;
	private final ProtocolSessionFactory sessionFactory;

	/**
	 * Maps clients of this server to their address. The client is represented as PCEP session. Used BiMap for
	 * implementation to allow easy manipulation with both InetSocketAddress and PCEPSessionImpl representing a key.
	 */
	private final BiMap<InetSocketAddress, ProtocolSession> sessions;

	private final Map<InetSocketAddress, Integer> sessionIds;

	private final Dispatcher parent;

	/**
	 * Creates a Protocol server.
	 * 
	 * @param address address to which this server is bound
	 * @param connectionFactory factory for connection specific properties
	 * @param parent Dispatcher that created this server
	 * @param sessionFactory factory for sessions
	 */
	public ProtocolServer(final InetSocketAddress address, final ProtocolConnectionFactory connectionFactory,
			final ProtocolSessionFactory sessionFactory, final Dispatcher parent) {
		this.serverAddress = address;
		this.sessions = HashBiMap.create();
		this.connectionFactory = connectionFactory;
		this.sessionFactory = sessionFactory;
		this.parent = parent;
		this.sessionIds = Maps.newHashMap();
	}

	/**
	 * Creates a session. This method is called after the server accepts incoming client connection. A session is
	 * created for each client. If a session for a client (represented by the address) was already created, return this,
	 * else create a new one.
	 * 
	 * @param clientAddress IP address of the client
	 * @param timer Timer common for all sessions
	 * @return new or existing PCEPSession
	 * @see <a href="http://tools.ietf.org/html/rfc5440#appendix-A">RFC</a>
	 */
	public ProtocolSession createSession(final Timer timer, final Channel channel) {
		ProtocolSession session = null;
		final InetSocketAddress clientAddress = (InetSocketAddress) channel.remoteAddress();
		if (this.sessions.containsKey(clientAddress)) { // when the session is created, the key is the InetSocketAddress
			session = this.sessions.get(clientAddress);
			if (compareTo(this.serverAddress.getAddress(), clientAddress.getAddress()) > 0) {
				try {
					session.close();
				} catch (final IOException e) {
					logger.error("Could not close session: {}.", session);
				}
			}
		} else {
			final int sessionId = getNextId(this.sessionIds.get(clientAddress), SESSIONS_LIMIT - 1);
			session = this.sessionFactory.getProtocolSession(this, timer, this.connectionFactory.createProtocolConnection(clientAddress),
					sessionId, channel.pipeline().context(ProtocolSessionOutboundHandler.class));
			this.sessionIds.put(clientAddress, sessionId);
		}
		this.sessions.put(clientAddress, session);
		return session;
	}

	@Override
	public synchronized void close() throws IOException {
		((DispatcherImpl) this.parent).onServerClosed(this);
		logger.debug("Closed server {}.", this);
	}

	@Override
	public synchronized void onSessionClosed(final ProtocolSession session) {
		this.sessions.inverse().remove(session); // when the session is closed, the key is the instance of the session
		logger.debug("Closed session {}.", session);
	}

	private static int getNextId(Integer lastId, final int maxId) {
		return (lastId == null || maxId == lastId) ? 0 : ++lastId;
	}

	/**
	 * Compares byte array representations of two InetAddresses.
	 * 
	 * @param addrOne
	 * @param addrTwo
	 * @throws IllegalArgumentException if InetAddresses don't belong to the same subclass of InetAddress.
	 * @return 1 if addrOne is greater than addrTwo, 0 if they are the same, -1 if addrOne is lower than addrTwo
	 */
	private static int compareTo(final InetAddress addrOne, final InetAddress addrTwo) {
		if ((addrOne instanceof Inet4Address && addrOne instanceof Inet6Address)
				|| (addrOne instanceof Inet6Address && addrOne instanceof Inet4Address)) {
			throw new IllegalArgumentException("Cannot compare InetAddresses. They both have to be the same subclass of InetAddress.");
		}
		final byte[] byteOne = addrOne.getAddress();
		final byte[] byteTwo = addrTwo.getAddress();
		for (int i = 0; i < byteOne.length; i++) {
			if (byteOne[i] > byteTwo[i])
				return 1;
			else if (byteOne[i] < byteTwo[i])
				return -1;
		}
		return 0;
	}

	@Override
	public String toString() {
		return "ProtocolServer [serverAddress=" + this.serverAddress + ", hashCode()=" + hashCode() + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.serverAddress == null) ? 0 : this.serverAddress.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ProtocolServer other = (ProtocolServer) obj;
		if (this.serverAddress == null) {
			if (other.serverAddress != null)
				return false;
		} else if (!this.serverAddress.equals(other.serverAddress))
			return false;
		return true;
	}
}
