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
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instantiated for each peer and table, listens on a particular peer's adj-rib-out,
 * performs transcoding to BA form (message) and sends it down the channel.
 */
@NotThreadSafe
final class AdjRibOutListener implements DOMDataTreeChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(AdjRibOutListener.class);

    private final ChannelOutputLimiter session;
    private final RIBSupport ribSupport;

    AdjRibOutListener(final RIBSupport ribSupport, final ChannelOutputLimiter session) {
        this.ribSupport = Preconditions.checkNotNull(ribSupport);
        this.session = Preconditions.checkNotNull(session);
    }

    static AdjRibOutListener create(final RIBSupport ribSupport, final ChannelOutputLimiter session) {
        return new AdjRibOutListener(ribSupport, session);
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeCandidate> changes) {
        LOG.debug("Data change received for AdjRibOut {}", changes);
        for (final DataTreeCandidate tc : changes) {
            for (final DataTreeCandidateNode child : tc.getRootNode().getChildNodes()) {
                for (final DataTreeCandidateNode route : this.ribSupport.changedRoutes(child)) {
                    LOG.debug("AdjRibOut parsing route {}", route);
                    final UpdateBuilder ub = new UpdateBuilder();

                    // FIXME: fill the structure (use codecs)

                    this.session.write(ub.build());
                }
            }
        }
        this.session.flush();
    }
}
