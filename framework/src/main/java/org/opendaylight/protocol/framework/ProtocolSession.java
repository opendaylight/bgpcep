/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import java.io.Closeable;
import java.io.IOException;

/**
 * Protocol Session represents the finite state machine in underlying
 * protocol, including timers and its purpose is to create a connection
 * between server and client. Session is automatically started, when TCP
 * connection is created, but can be stopped manually.
 * If the session is up, it has to redirect messages to/from user.
 * Handles also malformed messages and unknown requests.
 *
 * This interface should be implemented by a final class representing
 * a protocol specific session.
 */
public interface ProtocolSession extends Closeable {

	/**
	 * Starts the session. This method should be used only internally by
	 * the Dispatcher.
	 */
	public void startSession();

	/**
	 * Returns underlying output stream to provide writable stream to the
	 * Dispatcher.
	 * @return underlying protocol specific output stream
	 */
	public ProtocolOutputStream getStream();

	/**
	 * Handles incoming message (parsing, reacting if necessary).
	 * @param msg incoming message
	 */
	public void handleMessage(final ProtocolMessage msg);

	/**
	 * Handles malformed message when a deserializer exception occurred.
	 * The handling might be different from when a documented exception
	 * is thrown.
	 * @param e deserializer exception that occurred
	 */
	public void handleMalformedMessage(final DeserializerException e);

	/**
	 * Handles malformed message when a documented exception occurred.
	 * The handling might be different from when a deserializer exception
	 * is thrown.
	 * @param e documented exception that occurred
	 */
	public void handleMalformedMessage(final DocumentedException e);

	/**
	 * Called when reached the end of input stream while reading.
	 */
	public void endOfInput();

	/**
	 * Getter for message factory
	 * @return protocol specific message factory
	 */
	public ProtocolMessageFactory getMessageFactory();

	/**
	 * Session is notified about the connection not being
	 * established successfully.
	 *
	 * @param e IOException that was the cause of
	 * failed connection.
	 */
	public void onConnectionFailed(final IOException e);

	/**
	 * Returns the maximum message size (in bytes) for purposes of dispatcher
	 * buffering -- the dispatcher allocates a buffer this big, and if it gets
	 * full without making decoding progress, the dispatcher terminates the
	 * session.
	 * 
	 * @return maximum message size
	 */
	public int maximumMessageSize();
}
