/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import java.util.List;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBIn;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBInFactory;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;

/**
 * Internal reference to a RIB instance.
 */
public interface RIB extends RibReference {
    AsNumber getLocalAs();

    Ipv4Address getBgpIdentifier();

    List<? extends BgpTableType> getLocalTables();

    void initTable(Peer bgpPeer, TablesKey key);

    void clearTable(Peer bgpPeer, AdjRIBIn key);

    //void updateTables(Update message);

    BGPDispatcher getDispatcher();

    ReconnectStrategyFactory getTcpStrategyFactory();

    ReconnectStrategyFactory getSessionStrategyFactory();

    //NEW
    DataModificationTransaction getTransaction();

    AdjRIBInFactory getAdjRIBInFactory(Class<? extends AddressFamily> afi, Class<? extends SubsequentAddressFamily> safi);

}
