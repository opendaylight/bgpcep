/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import org.opendaylight.protocol.concepts.ASNumber;

public final class NodeIdentifierFactory {
	private final ASNumber as;
	private final DomainIdentifier domain;
	private final AreaIdentifier area;

	public NodeIdentifierFactory(final ASNumber as, final DomainIdentifier domain, final AreaIdentifier area) {
		this.as = as;
		this.area = area;
		this.domain = domain;
	}

	public NodeIdentifier identifierForRouter(final RouterIdentifier router) {
		return new NodeIdentifier(as, domain, area, router);
	}

	public static NodeIdentifier localIdentifier(final RouterIdentifier router) {
		return new NodeIdentifier(null, null, null, router);
	}
}
