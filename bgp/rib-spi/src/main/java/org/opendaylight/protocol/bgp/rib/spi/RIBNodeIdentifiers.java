/*
 * Copyright (c) 2019 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.annotations.Beta;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.peer.AdjRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.peer.AdjRibOut;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.peer.EffectiveRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;

/**
 * Utility constant {@link NodeIdentifier}s for various RIB constructs.
 */
@Beta
public final class RIBNodeIdentifiers {
    public static final NodeIdentifier BGPRIB_NID = NodeIdentifier.create(BgpRib.QNAME);
    public static final NodeIdentifier RIB_NID = NodeIdentifier.create(Rib.QNAME);
    public static final NodeIdentifier PEER = NodeIdentifier.create(Peer.QNAME);

    public static final NodeIdentifier ADJRIBIN_NID = NodeIdentifier.create(AdjRibIn.QNAME);
    public static final NodeIdentifier ADJRIBOUT_NID = NodeIdentifier.create(AdjRibOut.QNAME);
    public static final NodeIdentifier EFFRIBIN_NID = NodeIdentifier.create(EffectiveRibIn.QNAME);
    public static final NodeIdentifier LOCRIB_NID = NodeIdentifier.create(LocRib.QNAME);
    public static final NodeIdentifier TABLES_NID = NodeIdentifier.create(Tables.QNAME);
    public static final NodeIdentifier ROUTES_NID = NodeIdentifier.create(Routes.QNAME);
    public static final NodeIdentifier ATTRIBUTES_NID = NodeIdentifier.create(Attributes.QNAME);

    public static final NodeIdentifier UPTODATE_NID = NodeIdentifier.create(RIBQNames.UPTODATE_QNAME);

    private RIBNodeIdentifiers() {

    }
}
