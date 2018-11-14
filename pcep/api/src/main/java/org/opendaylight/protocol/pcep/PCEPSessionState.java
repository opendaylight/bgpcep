/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.Messages;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.PeerPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.Open;

/**
 * Exposes Session state.
 */
public interface PCEPSessionState {
    /**
     * The statistics of PCEP received/sent messages from the PCE point of view.
     *
     * @return messages
     */
    @Nonnull Messages getMessages();

    /**
     * The local (PCE) preferences.
     *
     * @return local preferences
     */
    @Nonnull LocalPref getLocalPref();

    /**
     * The remote peer (PCC) preferences.
     *
     * @return peer preferences
     */
    @Nonnull PeerPref getPeerPref();

    /**
     * The local (PCE) Open Message.
     *
     * @return Open
     */
    @Nonnull Open getLocalOpen();
}
