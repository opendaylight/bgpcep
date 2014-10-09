/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import java.util.List;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;

/**
 * Internal reference to a RIB instance.
 */
public interface RIB {
    AsNumber getLocalAs();

    Ipv4Address getBgpIdentifier();

    List<? extends BgpTableType> getLocalTables();

    void initTable(Peer bgpPeer, TablesKey key);

    void clearTable(Peer bgpPeer, TablesKey key);

    void updateTables(Peer bgpPeer, Update message);

    BGPDispatcher getDispatcher();

    ReconnectStrategyFactory getTcpStrategyFactory();

    ReconnectStrategyFactory getSessionStrategyFactory();

    AdjRIBsOutRegistration registerRIBsOut(Peer bgpPeer, AdjRIBsOut aro);

    long getRoutesCount(TablesKey key);
}
