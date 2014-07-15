/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;

import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ApplicationPeer implements AutoCloseable, Peer, DataChangeListener {
    private final byte[] rawIdentifier;
    private final RIB targetRib;
    private final String name;

    public ApplicationPeer(final ApplicationRibId applicationRibId, final Ipv4Address ipAddress, final RIB targetRib) {
        this.name = applicationRibId.getValue().toString();
        this.targetRib = Preconditions.checkNotNull(targetRib);
        this.rawIdentifier = InetAddresses.forString(ipAddress.getValue()).getAddress();
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        // FIXME: first notification needs to simulate connection-up and populate the peer tables

        final UpdateBuilder ub = new UpdateBuilder();

        // NOTE: this is pretty much what AbstractTopologyBuilder.onLocRIBChange() does
        for (Entry<InstanceIdentifier<?>, DataObject> data : change.getCreatedData().entrySet()) {
            // FIXME: populate MPReach with these
        }
        for (Entry<InstanceIdentifier<?>, DataObject> data : change.getUpdatedData().entrySet()) {
            // FIXME: populate MPReach with these
        }
        for (InstanceIdentifier<?> data : change.getRemovedPaths()) {
            // FIXME: populate MPUnreach with these
        }

        targetRib.updateTables(this, ub.build());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void close() {
        for (BgpTableType t : targetRib.getLocalTables()) {
            targetRib.clearTable(this, new TablesKey(t.getAfi(), t.getSafi()));
        }
    }

    @Override
    public byte[] getRawIdentifier() {
        return rawIdentifier;
    }
}
