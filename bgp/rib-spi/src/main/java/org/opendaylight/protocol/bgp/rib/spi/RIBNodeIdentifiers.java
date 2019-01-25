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
    public static final NodeIdentifier BGPRIB = NodeIdentifier.create(BgpRib.QNAME);
    public static final NodeIdentifier RIB = NodeIdentifier.create(Rib.QNAME);
    public static final NodeIdentifier ADJRIBIN = NodeIdentifier.create(AdjRibIn.QNAME);
    public static final NodeIdentifier ADJRIBOUT = NodeIdentifier.create(AdjRibOut.QNAME);
    public static final NodeIdentifier EFFRIBIN = NodeIdentifier.create(EffectiveRibIn.QNAME);
    public static final NodeIdentifier LOCRIB = NodeIdentifier.create(LocRib.QNAME);
    public static final NodeIdentifier TABLES = NodeIdentifier.create(Tables.QNAME);
    public static final NodeIdentifier ROUTES = NodeIdentifier.create(Routes.QNAME);
    public static final NodeIdentifier ATTRIBUTES = NodeIdentifier.create(Attributes.QNAME);

    private RIBNodeIdentifiers() {

    }
}
