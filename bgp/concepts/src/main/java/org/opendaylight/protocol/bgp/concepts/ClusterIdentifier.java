/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.concepts;

import org.opendaylight.protocol.concepts.AbstractIdentifier;
import org.opendaylight.protocol.util.ByteArray;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

/**
 * Class representing a BGP route-reflector cluster identifier.
 */
public final class ClusterIdentifier extends AbstractIdentifier<ClusterIdentifier> {
	private static final long serialVersionUID = 7119651900750614105L;
	/**
	 * Size of cluster identifier in bytes.
	 */
	public static final int SIZE = 4;
	private final byte[] clusterId;

	/**
	 * Initialize a new Cluster Identifier.
	 * 
	 * @param clusterId 4-byte identifier
	 * @throws IllegalArgumentException if the length of supplied clusterId is not 6 bytes
	 */
	public ClusterIdentifier(final byte[] clusterId) {
		Preconditions.checkArgument(clusterId.length == 4, "Invalid Cluster ID");
		this.clusterId = clusterId;
	}

	@Override
	public byte[] getBytes() {
		return this.clusterId;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		return toStringHelper.add("clusterId", ByteArray.toHexString(clusterId, "."));
	}
}
