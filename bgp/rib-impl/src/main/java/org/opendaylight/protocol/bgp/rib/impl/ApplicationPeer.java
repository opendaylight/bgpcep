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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.route.Attributes;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationPeer implements AutoCloseable, Peer, DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationPeer.class);

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
        for (final Entry<InstanceIdentifier<?>, DataObject> data : change.getCreatedData().entrySet()) {
            fillMpReach(ub, data);
        }
        for (final Entry<InstanceIdentifier<?>, DataObject> data : change.getUpdatedData().entrySet()) {
            fillMpReach(ub, data);
        }
        for (final InstanceIdentifier<?> data : change.getRemovedPaths()) {
            final MpUnreachNlriBuilder unreachBuilder = new MpUnreachNlriBuilder();
            final TablesKey key = data.firstKeyOf(Tables.class, TablesKey.class);
            unreachBuilder.setAfi(key.getAfi());
            unreachBuilder.setSafi(key.getSafi());
            final AbstractAdjRIBs<?,?,?> ribsIn = (AbstractAdjRIBs<?,?,?>)this.targetRib.getTable(key);
            ribsIn.addWith(unreachBuilder, data);
            ub.setPathAttributes(new PathAttributesBuilder().addAugmentation(PathAttributes2.class, new PathAttributes2Builder().setMpUnreachNlri(unreachBuilder.build()).build()).build());
            LOG.debug("Updating RIB with {}", ub.build());
            this.targetRib.updateTables(this, ub.build());
        }

    }

    private void fillMpReach(final UpdateBuilder ub, final Entry<InstanceIdentifier<?>, DataObject> data) {
        if (data.getValue() instanceof Route) {
            final Route r = (Route) data.getValue();
            final MpReachNlriBuilder reachBuilder = new MpReachNlriBuilder();
            final TablesKey key = data.getKey().firstKeyOf(Tables.class, TablesKey.class);
            reachBuilder.setAfi(key.getAfi());
            reachBuilder.setSafi(key.getSafi());
            final AdjRIBsIn<?,Route> ribsIn = this.targetRib.getTable(key);
            ribsIn.addAdvertisement(reachBuilder, (Route)data.getValue());
            final PathAttributesBuilder pa = new PathAttributesBuilder();
            pa.addAugmentation(PathAttributes1.class, new PathAttributes1Builder().setMpReachNlri(reachBuilder.build()).build());
            this.addAttributes(pa, r.getAttributes());
            pa.setCNextHop(reachBuilder.getCNextHop());
            ub.setPathAttributes(pa.build());
            LOG.debug("Updating RIB with {}", ub.build());
            this.targetRib.updateTables(this, ub.build());
        }
    }

    private void addAttributes(final PathAttributesBuilder pa, final Attributes a) {
        if (a != null) {
            pa.setAggregator(a.getAggregator());
            pa.setAsPath(a.getAsPath());
            pa.setAtomicAggregate(a.getAtomicAggregate());
            pa.setClusterId(a.getClusterId());
            pa.setCommunities(a.getCommunities());
            pa.setExtendedCommunities(a.getExtendedCommunities());
            pa.setLocalPref(a.getLocalPref());
            pa.setMultiExitDisc(a.getMultiExitDisc());
            pa.setOrigin(a.getOrigin());
            pa.setOriginatorId(a.getOriginatorId());
        }
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
