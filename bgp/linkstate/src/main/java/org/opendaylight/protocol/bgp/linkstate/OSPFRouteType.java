/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

/**
 * @see <a href="http://tools.ietf.org/html/draft-ietf-idr-ls-distribution-02#section-3.2.1.4">OSPF Route Type SubTLV</a>
 */
public enum OSPFRouteType {
	Intra_Area, Inter_Area, External1, External2, NSSA1, NSSA2
}
