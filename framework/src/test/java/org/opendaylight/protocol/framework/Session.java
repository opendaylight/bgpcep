/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class Session implements ProtocolSession {

	private static final Logger logger = LoggerFactory.getLogger(Session.class);

	private final ProtocolOutputStream pos;

	public final List<ProtocolMessage> msgs = Lists.newArrayList();

	private final ProtocolMessageFactory pmf = new MessageFactory();

	private final SessionParent parent;

	public boolean up = false;

	private final int maxMsgSize;

	public Session(final SessionParent parent, final int maxMsgSize) {
		this.pos = new ProtocolOutputStream();
		this.parent = parent;
		this.maxMsgSize = maxMsgSize;
	}

	@Override
	public void close() throws IOException {

	}

	@Override
	public void startSession() {
		this.pos.putMessage(new Message("hello"), this.pmf);
		this.parent.checkOutputBuffer(this);
	}

	@Override
	public ProtocolOutputStream getStream() {
		return this.pos;
	}

	@Override
	public void handleMessage(ProtocolMessage msg) {
		logger.debug("Message received: {}", ((Message)msg).getMessage());
		this.up = true;
		this.msgs.add(msg);
		logger.debug(this.msgs.size() + "");
	}

	@Override
	public void handleMalformedMessage(DeserializerException e) {
		logger.debug("Malformed message: {}", e.getMessage(), e);
	}

	@Override
	public void handleMalformedMessage(DocumentedException e) {
		logger.debug("Malformed message: {}", e.getMessage(), e);
	}

	@Override
	public void endOfInput() {
		logger.debug("End of input reported.");
	}

	@Override
	public ProtocolMessageFactory getMessageFactory() {
		return null;
	}

	@Override
	public void onConnectionFailed(IOException e) {
		logger.debug("Connection failed: {}", e.getMessage(), e);
	}

	@Override
	public int maximumMessageSize() {
		return maxMsgSize;
	}
}
