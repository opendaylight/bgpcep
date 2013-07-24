/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import org.opendaylight.protocol.concepts.AbstractIdentifier;
import org.opendaylight.protocol.util.ByteArray;
import com.google.common.base.Objects.ToStringHelper;

public final class UniverseIdentifier extends AbstractIdentifier<UniverseIdentifier> {
	public static final UniverseIdentifier L3_PACKET_TOPOLOGY = new UniverseIdentifier(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 });
	public static final UniverseIdentifier L1_OPTICAL_TOPOLOGY = new UniverseIdentifier(new byte[] { 0, 0, 0, 0, 0, 0, 0, 1 });

	private static final long serialVersionUID = 1L;
	private final byte[] value;

	public UniverseIdentifier(final byte[] value) {
		this.value = value;
	}

	@Override
	protected byte[] getBytes() {
		return value;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		return toStringHelper.add("value", ByteArray.toHexString(value, "."));
	}
}

