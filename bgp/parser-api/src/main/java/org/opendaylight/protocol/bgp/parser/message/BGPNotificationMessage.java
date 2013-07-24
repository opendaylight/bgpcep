/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.message;

import java.util.Arrays;

import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPMessage;


/**
 * Representation of BGPNotification message.
 * 
 * @see <a link="http://tools.ietf.org/html/rfc4271#section-4.5">BGP Notification Message</a>
 */
public final class BGPNotificationMessage implements BGPMessage {

	private static final long serialVersionUID = -5860147919167775673L;

	private final BGPError error;

	private final byte[] data;

	/**
	 * Creates a BGPNotification message with no data.
	 * 
	 * @param error cause
	 */
	public BGPNotificationMessage(final BGPError error) {
		this(error, null);
	}

	/**
	 * Creates a BGPNotification message with error cause and data.
	 * 
	 * @param error cause
	 * @param data associated with this message
	 */
	public BGPNotificationMessage(final BGPError error, final byte[] data) {
		super();
		this.error = error;
		this.data = data;
	}

	/**
	 * Returns BGPError contained in this message.
	 * 
	 * @return the error
	 */
	public BGPError getError() {
		return this.error;
	}

	/**
	 * Returns possible data associated with this message.
	 * 
	 * @return the data
	 */
	public byte[] getData() {
		return this.data;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("BGPNotificationMessage [error=");
		builder.append(this.error);
		builder.append(", data=");
		builder.append(Arrays.toString(this.data));
		builder.append("]");
		return builder.toString();
	}
}
