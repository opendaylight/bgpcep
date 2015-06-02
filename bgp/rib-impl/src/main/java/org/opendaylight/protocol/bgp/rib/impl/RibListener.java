/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.AdjRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.EffectiveRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.BindingMapping;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RibListener implements AutoCloseable, DOMDataTreeChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(RibListener.class);
    private static final NodeIdentifier ADJRIBIN_NID = new NodeIdentifier(AdjRibIn.QNAME);
    private static final NodeIdentifier TABLES_NID = new NodeIdentifier(Tables.QNAME);
    private static final NodeIdentifier EFFRIBIN_NID = new NodeIdentifier(EffectiveRibIn.QNAME);
    private static final NodeIdentifier PEER_ROLE_NID = new NodeIdentifier(QName.cachedReference(QName.create(Peer.QNAME, "peer-role")));

    private final Map<TablesKey, LocRibWriter> locRibs = new HashMap<>();
    private EffectiveRibInWriter effWriter;
    private final DOMDataTreeChangeService service;
    private final YangInstanceIdentifier targetPath;
    private ListenerRegistration<?> reg;

    public static RibListener create(@Nonnull final YangInstanceIdentifier target, @Nonnull final DOMDataTreeChangeService service) {
        return new RibListener(target, service);
    }

    private RibListener(final YangInstanceIdentifier target, final DOMDataTreeChangeService service) {
        this.service = service;
        this.targetPath = target.node(Peer.QNAME).node(Peer.QNAME);
    }

    public void setEffectiveRibInWriter(final EffectiveRibInWriter effWriter) {
        this.effWriter = Preconditions.checkNotNull(effWriter);
    }

    public void addLocRibWriter(final TablesKey tablesKey, final LocRibWriter locRibWriter) {
        Preconditions.checkNotNull(tablesKey);
        Preconditions.checkNotNull(locRibWriter);
        this.locRibs.put(tablesKey, locRibWriter);
    }

    public void registerListener() {
        final DOMDataTreeIdentifier treeId = new DOMDataTreeIdentifier(LogicalDatastoreType.OPERATIONAL, this.targetPath);
        this.reg = this.service.registerDataTreeChangeListener(treeId, this);
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeCandidate> changes) {
        LOG.trace("RibListener was called with changes {}", Arrays.toString(changes.toArray()));
        for (final DataTreeCandidate tc : changes) {
            final DataTreeCandidateNode root = tc.getRootNode();
            LOG.trace("Trying to process change {}", root);
            final YangInstanceIdentifier rootPath = tc.getRootPath();
            final NodeIdentifierWithPredicates peerKey = IdentifierUtils.peerKey(rootPath);

            // call out peer-role has changed
            final DataTreeCandidateNode roleChange =  root.getModifiedChild(PEER_ROLE_NID);
            if (roleChange != null) {
                final PeerRole role = extractPeerRole(roleChange);
                final YangInstanceIdentifier peerPath = IdentifierUtils.peerPath(rootPath);
                this.effWriter.peerRoleChanged(peerPath, role);
                for (final LocRibWriter writer : this.locRibs.values()) {
                    writer.peerRoleChanged(peerPath, role);
                }
            }

            // filter out changes in AdjRibsIn, advertise them to EffectiveWriter
            final DataTreeCandidateNode adjRibIn =  root.getModifiedChild(ADJRIBIN_NID);
            if (adjRibIn != null) {
                final DataTreeCandidateNode tables = adjRibIn.getModifiedChild(TABLES_NID);
                if (tables != null) {
                    this.effWriter.onDataTreeChanged(tables, rootPath, root, peerKey);
                    continue;
                }
            }

            // filter out changes in EffectiveRib, advertise them to LocRib
            final DataTreeCandidateNode effRibIn = root.getModifiedChild(EFFRIBIN_NID);
            if (effRibIn != null) {
                final DataTreeCandidateNode tables = effRibIn.getModifiedChild(TABLES_NID);
                if (tables != null) {
                    for (final Entry<TablesKey, LocRibWriter> entry : this.locRibs.entrySet()) {
                        final DataTreeCandidateNode table = tables.getModifiedChild(RibSupportUtils.toYangTablesKey(entry.getKey()));
                        if (table != null) {
                            entry.getValue().onDataTreeChanged(table, peerKey);
                        }
                    }
                }
                continue;
            }
            LOG.debug("Skipping change {}", root);
        }
    }

    private PeerRole extractPeerRole(final DataTreeCandidateNode change) {
        LOG.debug("Data Changed for Peer role {}", change);
        // Check for removal
        final Optional<NormalizedNode<?, ?>> maybePeerRole = change.getDataAfter();
        PeerRole role = null;
        if (maybePeerRole.isPresent()) {
            final LeafNode<?> peerRoleLeaf = (LeafNode<?>) maybePeerRole.get();
            // We could go for a codec, but this is simpler and faster
            role = PeerRole.valueOf(BindingMapping.getClassName((String) peerRoleLeaf.getValue()));
        }
        return role;
    }

    @Override
    public void close() {
        if (this.reg != null) {
            this.reg.close();
        }
    }
}
