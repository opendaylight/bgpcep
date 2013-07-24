/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import org.opendaylight.protocol.concepts.NetworkAddress;
import org.opendaylight.protocol.bgp.linkstate.PrefixIdentifier;

/**
 * Generic, IGP-independent prefix advertisement.
 * @param <T> Network Address type of the prefix
 */
public interface NetworkPrefix<T extends NetworkAddress<?>> extends NetworkObject<PrefixIdentifier<T>> {
	@Override
	public NetworkPrefixState currentState();
}

