/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.OpenObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev250930.pcep.session.state.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev250930.pcep.session.state.Messages;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev250930.pcep.session.state.PeerPref;

/**
 * Exposes Session state.
 */
@NonNullByDefault
public interface PCEPSessionState {
    /**
     * The statistics of PCEP received/sent messages from the PCE point of view.
     *
     * @return messages
     */
    Messages getMessages();

    /**
     * The local (PCE) preferences.
     *
     * @return local preferences
     */
    LocalPref getLocalPref();

    /**
     * The remote peer (PCC) preferences.
     *
     * @return peer preferences
     */
    PeerPref getPeerPref();

    /**
     * The local (PCE) Open Message's Open Object.
     *
     * @return Open
     */
    OpenObject getLocalOpen();
}
