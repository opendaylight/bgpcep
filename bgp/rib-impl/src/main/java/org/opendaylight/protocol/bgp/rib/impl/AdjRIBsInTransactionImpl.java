/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;

import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsInTransaction;
import org.opendaylight.protocol.bgp.rib.spi.BGPObjectComparator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.AttributesBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AdjRIBsInTransactionImpl implements AdjRIBsInTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(AdjRIBsInTransactionImpl.class);
    private final DataModificationTransaction trans;
    private final BGPObjectComparator comparator;

    AdjRIBsInTransactionImpl(final BGPObjectComparator comparator, final DataModificationTransaction trans) {
        this.comparator = Preconditions.checkNotNull(comparator);
        this.trans = Preconditions.checkNotNull(trans);
    }

    @Override
    public void setUptodate(final InstanceIdentifier<Tables> basePath, final boolean uptodate) {
        final InstanceIdentifier<Attributes> aid = basePath.child(Attributes.class);
        final Attributes a = (Attributes) trans.readOperationalData(aid);
        Preconditions.checkState(a != null);

        if (uptodate != a.isUptodate()) {
            LOG.debug("Table {} switching uptodate to {}", basePath, uptodate);
            trans.removeOperationalData(aid);
            trans.putOperationalData(aid, new AttributesBuilder().setUptodate(uptodate).build());
        }
    }

    public Future<RpcResult<TransactionStatus>> commit() {
        return trans.commit();
    }

    @Override
    public <T extends DataObject> void advertise(final InstanceIdentifier<T> id, final T obj) {
        trans.putOperationalData(id, obj);
    }

    @Override
    public void withdraw(final InstanceIdentifier<?> id) {
        trans.removeOperationalData(id);
    }

    @Override
    public BGPObjectComparator comparator() {
        return comparator;
    }
}
