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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.PathType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.pcc.configured.lsp.ConfiguredLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.pcc.configured.lsp.ConfiguredLspKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.CodeHelpers;
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
    private ConcurrentMap<ConfiguredLspKey, ManagedTePath> mngPaths =
            new ConcurrentHashMap<ConfiguredLspKey, ManagedTePath>();
    private final TransactionChain chain;
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

    public ConcurrentMap<ConfiguredLspKey, ManagedTePath> getTePaths() {
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

    /**
     * Disable this Managed TE Node and removed associated Managed Node from the Data Store. TE Path which are not
     * delegated are removed from the path list other ones are mark as disabled.
     */
    public void disable() {
        /* Remove associated TE Paths that are not managed by the PCE i.e. TE Path which are not delegated */
        for (ManagedTePath mngPath: mngPaths.values()) {
            final ConfiguredLsp lsp = mngPath.getLsp();
            if (mngPath.getType() == PathType.Stateless || mngPath.getType() == PathType.Pcc) {
                mngPaths.remove(lsp.key());
            } else {
                mngPath.disabled();
            }
        }

        state = NodeState.Disabled;
        LOG.debug("Managed TE Node {} has been disabled. Keep configuration {}", id, this);
    }

    public void addManagedTePath(final ManagedTePath mngPath) {
        mngPaths.put(mngPath.getLsp().key(), mngPath);
    }

    public ManagedTePath removeManagedTePath(final ConfiguredLspKey key) {
        checkArgument(key != null, "Provided Configured LSP Key is a null object");

        /* Get corresponding Managed TE Path */
        final ManagedTePath mngPath = mngPaths.remove(key);
        if (mngPath == null) {
            return null;
        }

        /* Remove corresponding Managed Path from the Data Store */
        mngPath.removeFromDataStore();

        return mngPath;
    }

    public ManagedTePath getManagedTePath(final ConfiguredLspKey key) {
        checkArgument(key != null, "Provided Configured LSP Key is a null object");

        return mngPaths.get(key);
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
