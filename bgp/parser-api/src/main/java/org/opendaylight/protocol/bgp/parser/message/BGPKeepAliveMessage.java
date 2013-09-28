/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.message;

import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * BGP KeepAlive message. Always empty.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc4271#section-4.4">BGP KeepAlive message</a>
 */
public final class BGPKeepAliveMessage implements Notification {

	/**
	 * Creates a BGP KeepAlive message.
	 */
	public BGPKeepAliveMessage() {

	}
}
