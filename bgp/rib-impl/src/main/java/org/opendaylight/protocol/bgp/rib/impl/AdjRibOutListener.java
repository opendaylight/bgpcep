/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import java.util.Collection;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

/**
 * Instantiated for each peer and table, listens on a particular peer's adj-rib-out,
 * performs transcoding to BA form (message) and sends it down the channel.
 */
@NotThreadSafe
final class AdjRibOutListener implements DOMDataTreeChangeListener {
    private final ChannelOutputLimiter session;
    private final RIBSupport ribSupport;

    AdjRibOutListener(final RIBSupport ribSupport, final ChannelOutputLimiter session) {
        this.ribSupport = Preconditions.checkNotNull(ribSupport);
        this.session = Preconditions.checkNotNull(session);
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeCandidate> changes) {
        for (DataTreeCandidate tc : changes) {
            final UpdateBuilder ub = new UpdateBuilder();

            // FIXME: fill the structure

            session.write(ub.build());
        }

        session.flush();
    }

}
