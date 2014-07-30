/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsInTransaction;
import org.opendaylight.protocol.bgp.rib.spi.BGPObjectComparator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.AttributesBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AdjRIBsInTransactionImpl implements AdjRIBsInTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(AdjRIBsInTransactionImpl.class);
    private final BGPObjectComparator comparator;
    private final WriteTransaction trans;

    AdjRIBsInTransactionImpl(final BGPObjectComparator comparator, final WriteTransaction writeTransaction) {
        this.comparator = Preconditions.checkNotNull(comparator);
        this.trans = Preconditions.checkNotNull(writeTransaction);
    }

    @Override
    public void setUptodate(final InstanceIdentifier<Tables> basePath, final boolean uptodate) {
        final InstanceIdentifier<Attributes> aid = basePath.child(Attributes.class);
        trans.merge(LogicalDatastoreType.OPERATIONAL, aid, new AttributesBuilder().setUptodate(uptodate).build());
        LOG.debug("Table {} switching uptodate to {}", basePath, uptodate);
    }

    public CheckedFuture<Void, TransactionCommitFailedException> commit() {
        return trans.submit();
    }

    @Override
    public <T extends DataObject> void advertise(final InstanceIdentifier<T> id, final T obj) {
        trans.put(LogicalDatastoreType.OPERATIONAL, id, obj, true);
    }

    @Override
    public void withdraw(final InstanceIdentifier<?> id) {
        trans.delete(LogicalDatastoreType.OPERATIONAL, id);
    }

    @Override
    public BGPObjectComparator comparator() {
        return comparator;
    }
}
