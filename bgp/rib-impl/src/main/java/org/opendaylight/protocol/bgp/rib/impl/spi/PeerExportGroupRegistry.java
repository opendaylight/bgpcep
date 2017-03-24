/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import com.google.common.annotations.Beta;
import org.opendaylight.protocol.bgp.rib.spi.PeerExportGroup;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;

/**
 * PeerExportGroup Registry. Register should be maintained in blocking mode to avoid race between
 * updating the routes and notify to a peer which doesn't longer exist. BUG-7676
 */
@Beta
public interface PeerExportGroupRegistry extends PeerExportGroup {
    AbstractRegistration registerPeer(PeerId peerId, PeerExporTuple peerExporTuple);

    boolean isEmpty();
}
