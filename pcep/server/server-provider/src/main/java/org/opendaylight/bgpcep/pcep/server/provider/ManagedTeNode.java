/*
 * Copyright (c) 2021 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.server.provider;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.MoreObjects;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.ManagedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.PathType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.managed.path.ManagedNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.managed.path.ManagedNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.managed.path.ManagedNodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.managed.path.managed.node.TePath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.managed.path.managed.node.TePathKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.CodeHelpers;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagedTeNode {

    private enum NodeState {
        Disabled,
        Enabled,
        Sync
    }

    private final NodeId id;
    private NodeState state;
    private Map<TePathKey, ManagedTePath> mngPaths = new HashMap<TePathKey, ManagedTePath>();
    private final TransactionChain chain;
    private static final InstanceIdentifier<ManagedPath> MANAGED_PATH_IDENTIFIER =
            InstanceIdentifier.builder(ManagedPath.class).build();
    private static final Logger LOG = LoggerFactory.getLogger(ManagedTeNode.class);

    public ManagedTeNode(final NodeId id, final TransactionChain chain) {
        this.id = id;
        this.chain = chain;
        this.state = NodeState.Enabled;
    }

    public ManagedTeNode(final NodeId id, TransactionChain chain, final NodeState state) {
        this.id = id;
        this.chain = chain;
        this.state = state;
    }

    public NodeState getState() {
        return state;
    }

    public NodeId getId() {
        return id;
    }

    public Map<TePathKey, ManagedTePath> getTePaths() {
        return mngPaths;
    }

    public WriteTransaction getTransaction() {
        return chain.newWriteOnlyTransaction();
    }

    public boolean isSync() {
        return state == NodeState.Sync;
    }

    public void sync() {
        state = NodeState.Sync;
    }

    public void disable() {
        /* Remove associated TE Paths that are not managed by the PCE i.e. TE Path which are not delegated */
        for (ManagedTePath mngPath: mngPaths.values()) {
            final TePath tePath = mngPath.getPath();
            if (tePath.getPathType() == PathType.Stateless || tePath.getPathType() == PathType.Pcc) {
                mngPaths.remove(tePath.key());
            } else {
                mngPath.disabled();
            }
        }

        removeFromDataStore();
        state = NodeState.Disabled;
        LOG.debug("Managed TE Node {} has been disabled. Keep configuration {}", id, this);
    }

    public void addManagedTePath(final ManagedTePath mngPath) {
        mngPaths.put(mngPath.getPath().key(), mngPath);
    }

    public ManagedTePath removeManagedTePath(final TePathKey key) {
        checkArgument(key != null, "Provided TE Path Key is a null object");

        /* Get corresponding Managed TE Path */
        final ManagedTePath mngPath = mngPaths.remove(key);
        if (mngPath == null) {
            return null;
        }

        /* Remove corresponding Managed Path from the Data Store */
        mngPath.removeFromDataStore();

        return mngPath;
    }

    public ManagedTePath getManagedTePath(final TePathKey key) {
        checkArgument(key != null, "Provided TE Path Key is a null object");

        return mngPaths.get(key);
    }

    private InstanceIdentifier<ManagedNode> getManagedTeNodeInstanceIdentifier() {
        return MANAGED_PATH_IDENTIFIER.child(ManagedNode.class, new ManagedNodeKey(id));
    }

    /**
     * Add Managed Node components to the Data Store.
     */
    public synchronized ManagedNode addToDataStore() {
        final Map<TePathKey, TePath> tePaths = new HashMap<TePathKey, TePath>();
        for (ManagedTePath path: mngPaths.values()) {
            tePaths.put(path.getPath().key(), path.getPath());
        }
        final ManagedNode node = new ManagedNodeBuilder().setNodeId(id).setTePath(tePaths).build();
        final WriteTransaction trans = chain.newWriteOnlyTransaction();
        trans.put(LogicalDatastoreType.OPERATIONAL, getManagedTeNodeInstanceIdentifier(), node);
        trans.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.debug("Managed Node {} has been published in operational datastore ", id);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Cannot write Managed Node {} to the operational datastore (transaction: {})", id,
                        trans.getIdentifier());
            }
        }, MoreExecutors.directExecutor());
        return node;
    }

    /**
     * Remove Managed Node components to the Data Store.
     */
    public synchronized void removeFromDataStore() {
        final WriteTransaction trans = chain.newWriteOnlyTransaction();
        trans.delete(LogicalDatastoreType.OPERATIONAL, getManagedTeNodeInstanceIdentifier());
        trans.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.debug("Managed Node {} has been deleted in operational datastore ", id);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Cannot delete Managed Node {} from the operational datastore (transaction: {})", id,
                        trans.getIdentifier());
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public String toString() {
        final MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper("ManagedTeNode");
        CodeHelpers.appendValue(helper, "NodeId", id);
        CodeHelpers.appendValue(helper, "NodeState", state);
        CodeHelpers.appendValue(helper, "ManagedTePaths", mngPaths);
        return helper.toString();
    }
}
