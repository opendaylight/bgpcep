/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import java.net.Inet4Address;
import java.net.UnknownHostException;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;

/**
 * Util class for creating generated Ipv4Address.
 */
public final class Ipv4Util {

	public static Ipv4Address addressForBytes(final byte[] bytes) {
		try {
			return new Ipv4Address(Inet4Address.getByAddress(bytes).toString());
		} catch (final UnknownHostException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}
}
