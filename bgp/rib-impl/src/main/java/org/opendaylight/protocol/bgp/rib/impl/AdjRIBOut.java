/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;

import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Notification;

class AdjRIBOut extends AbstractAdjRIBOut<Object, DataObject> {
    private final BGPSessionImpl session;

    AdjRIBOut(final BGPSessionImpl session) {
        this.session = Preconditions.checkNotNull(session);
    }

    @Override
    protected void wantWrite() {
        // TODO Auto-generated method stub
    }

    @Override
    protected boolean writePDU(final Object key, final DataObject value) {
        final Notification ntf = null; // value;

        session.sendMessage(ntf);
        return session.isWritable();
    }
}
