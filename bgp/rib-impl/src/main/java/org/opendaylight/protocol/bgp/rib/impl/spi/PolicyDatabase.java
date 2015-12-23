/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;

public interface PolicyDatabase {
    ExportPolicy exportPolicyForRole(final PeerRole peerRole);

    ImportPolicy importPolicyForRole(final PeerRole peerRole);
}