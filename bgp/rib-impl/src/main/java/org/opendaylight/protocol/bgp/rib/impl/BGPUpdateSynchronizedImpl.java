/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import org.opendaylight.protocol.bgp.parser.BGPTableType;
import org.opendaylight.protocol.bgp.parser.BGPUpdateSynchronized;


/**
 *
 */
public class BGPUpdateSynchronizedImpl implements BGPUpdateSynchronized {

	private static final long serialVersionUID = -3574952849467738325L;

	private final BGPTableType tt;

	public BGPUpdateSynchronizedImpl(final BGPTableType tt) {
		this.tt = tt;
	}

	@Override
	public BGPTableType getTableType() {
		return this.tt;
	}
}
