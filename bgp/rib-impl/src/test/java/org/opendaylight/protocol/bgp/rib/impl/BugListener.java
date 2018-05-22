/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.routes.MvpnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.PeerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.peer.AdjRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BugListener implements ClusteredDataTreeChangeListener<Tables>, AutoCloseable {
    static final PeerId PEER_ID = new PeerId("bgp://1.1.1.2");
    static final KeyedInstanceIdentifier<Rib, RibKey> RIB_IID = InstanceIdentifier.create(BgpRib.class)
            .child(Rib.class, new RibKey(new RibId("test-rib")));
    static final KeyedInstanceIdentifier<Peer, PeerKey> PEER_IID = RIB_IID
            .child(Peer.class, new PeerKey(PEER_ID));
    static final InstanceIdentifier<Tables>  TABLES_IID = PEER_IID.child(AdjRibIn.class).child(Tables.class);
    private static final Logger LOG = LoggerFactory.getLogger(BugListener.class);
    private ListenerRegistration reg;

    public BugListener(final DataBroker databroker) {
        final DataTreeIdentifier treeId = new DataTreeIdentifier(LogicalDatastoreType.OPERATIONAL, TABLES_IID);
        LOG.debug("Registered Effective RIB on {}", this.PEER_IID);
        this.reg = requireNonNull(databroker).registerDataTreeChangeListener(treeId, this);
    }

    @Override
    public void onDataTreeChanged(@Nonnull final Collection<DataTreeModification<Tables>> changes) {
        for (final DataTreeModification<Tables> tc : changes) {
            final DataObjectModification<Tables> table = tc.getRootNode();
            final DataObjectModification.ModificationType modificationType = table.getModificationType();
            switch (modificationType) {
                case DELETE:
                    break;
                case SUBTREE_MODIFIED:
                    final DataObjectModification routesChangesContainer =
                            table.getModifiedChildContainer((Class) MvpnRoutes.class);

                    if (routesChangesContainer == null) {
                        LOG.error("This shouldn't be null");
                        break;
                    }
                    break;
                case WRITE:
                    final DataObjectModification routesWriteContainer =
                            table.getModifiedChildContainer((Class) MvpnRoutes.class);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public synchronized void close() {
        if (this.reg != null) {
            this.reg.close();
            this.reg = null;
        }
    }
}
