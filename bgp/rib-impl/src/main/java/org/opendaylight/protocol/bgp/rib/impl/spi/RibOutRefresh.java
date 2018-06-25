/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;

/**
 * Forces a reevaluation of paths and update on peer ribout.
 */
public interface RibOutRefresh {
    /**
     * Triggers the reevaluation.
     *
     * @param tk     table key of table route paths to be reevaluated
     * @param peerId peer to advertize / withdraw paths after reevaluation
     */
    void refreshTable(TablesKey tk, PeerId peerId);
}
