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
import org.opendaylight.protocol.bgp.rib.spi.AbstractAdjRIBs;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsIn;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ApplicationPeer implements AutoCloseable, Peer, DataChangeListener {
    private final byte[] rawIdentifier;
    private final RIBImpl targetRib;
    private final String name;

    public ApplicationPeer(final ApplicationRibId applicationRibId, final Ipv4Address ipAddress, final RIBImpl targetRib) {
        this.name = applicationRibId.getValue().toString();
        this.targetRib = Preconditions.checkNotNull(targetRib);
        this.rawIdentifier = InetAddresses.forString(ipAddress.getValue()).getAddress();
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        final UpdateBuilder ub = new UpdateBuilder();
        final MpReachNlriBuilder reachBuilder = new MpReachNlriBuilder();
        final MpUnreachNlriBuilder unreachBuilder = new MpUnreachNlriBuilder();
        for (final Entry<InstanceIdentifier<?>, DataObject> data : change.getCreatedData().entrySet()) {
            final TablesKey key = data.getKey().firstKeyOf(Tables.class, TablesKey.class);
            final AdjRIBsIn<?,Route> ribsIn = this.targetRib.getTable(key);
            ribsIn.addAdvertisement(reachBuilder, (Route)data.getValue());
        }
        for (final Entry<InstanceIdentifier<?>, DataObject> data : change.getUpdatedData().entrySet()) {
            final TablesKey key = data.getKey().firstKeyOf(Tables.class, TablesKey.class);
            final AdjRIBsIn<?,Route> ribsIn = this.targetRib.getTable(key);
            ribsIn.addAdvertisement(reachBuilder, (Route)data.getValue());
        }
        for (final InstanceIdentifier<?> data : change.getRemovedPaths()) {
            final TablesKey key = data.firstKeyOf(Tables.class, TablesKey.class);
            final AbstractAdjRIBs<?,?,?> ribsIn = (AbstractAdjRIBs<?,?,?>)this.targetRib.getTable(key);
            ribsIn.addWith(unreachBuilder, data);
        }
        if (reachBuilder.getAdvertizedRoutes() != null) {
            ub.setPathAttributes(new PathAttributesBuilder().addAugmentation(PathAttributes1.class, new PathAttributes1Builder().setMpReachNlri(reachBuilder.build()).build()).build());
        } else if (unreachBuilder.getWithdrawnRoutes() != null) {
            ub.setPathAttributes(new PathAttributesBuilder().addAugmentation(PathAttributes2.class, new PathAttributes2Builder().setMpUnreachNlri(unreachBuilder.build()).build()).build());
        }
        this.targetRib.updateTables(this, ub.build());
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void close() {
        for (final BgpTableType t : this.targetRib.getLocalTables()) {
            this.targetRib.clearTable(this, new TablesKey(t.getAfi(), t.getSafi()));
        }
    }

    @Override
    public byte[] getRawIdentifier() {
        return this.rawIdentifier;
    }
}
