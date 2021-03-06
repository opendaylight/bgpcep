/*
 * Copyright (c) 2019 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Attributes;
import org.opendaylight.yangtools.yang.common.QName;

/**
 * Utility constant {@link QName}s for various RIB constructs which are not covered by generated DTOs' QNAME constants.
 */
@NonNullByDefault
public final class RIBQNames {
    public static final QName UPTODATE_QNAME = QName.create(Attributes.QNAME, "uptodate").intern();
    public static final QName PEER_ID_QNAME = QName.create(Peer.QNAME, "peer-id").intern();
    public static final QName LLGR_STALE_QNAME = QName.create(Attributes.QNAME, "llgr-stale").intern();
    public static final QName AFI_QNAME = QName.create(Tables.QNAME, "afi").intern();
    public static final QName SAFI_QNAME = QName.create(Tables.QNAME, "safi").intern();

    private RIBQNames() {
        // Hidden on purpose
    }
}
