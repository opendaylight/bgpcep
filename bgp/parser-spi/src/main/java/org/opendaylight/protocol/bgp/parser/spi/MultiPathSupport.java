/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.BgpTableType;

/**
 * Holds user specific add-path constraints per AFI/SAFI (table type)
 *
 */
public interface MultiPathSupport extends PeerConstraint {

    /**
     * Check if requested AFI/SAFI is supported.
     * @param tableType
     * @return True if the table type is supported.
     */
    boolean isTableTypeSupported(BgpTableType tableType);

}
