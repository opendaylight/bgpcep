/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import java.util.Comparator;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;

/**
 * Marker interface identifying a BGP peer.
 */
public interface Peer {
    /**
     * Return peer's symbolic name.
     *
     * @return symbolic name.
     */
    String getName();

    /**
     * Return the peer's attached path attribute comparator.
     *
     * @return Path attribute comparator, as viewed from the peer.
     */
    Comparator<PathAttributes> getComparator();
}
