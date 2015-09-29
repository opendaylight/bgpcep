/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.EnumMap;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;

/**
 * Policy database attached to a particular RIB instance. Acts as the unified
 * lookup point.
 */
final class PolicyDatabase {
    private final Map<PeerRole, AbstractExportPolicy> exportPolicies = new EnumMap<>(PeerRole.class);
    private final Map<PeerRole, AbstractImportPolicy> importPolicies = new EnumMap<>(PeerRole.class);

    PolicyDatabase(final Long localAs, final Ipv4Address bgpId, final ClusterIdentifier clusterId) {
        exportPolicies.put(PeerRole.Ebgp, new ToExternalExportPolicy(localAs));
        exportPolicies.put(PeerRole.Ibgp, new ToInternalExportPolicy(bgpId, clusterId));
        exportPolicies.put(PeerRole.RrClient, new ToReflectorClientExportPolicy(bgpId, clusterId));
        exportPolicies.put(PeerRole.Internal, new ToInternalReflectorClientExportPolicy(bgpId, clusterId));

        importPolicies.put(PeerRole.Ebgp, new FromExternalImportPolicy());
        importPolicies.put(PeerRole.Ibgp, new FromInternalImportPolicy(bgpId, clusterId));
        importPolicies.put(PeerRole.RrClient, new FromReflectorClientImportPolicy(bgpId, clusterId));
        importPolicies.put(PeerRole.Internal, new FromInternalReflectorClientImportPolicy(bgpId, clusterId));
    }

    AbstractExportPolicy exportPolicyForRole(final PeerRole peerRole) {
        return exportPolicies.get(peerRole);
    }

    AbstractImportPolicy importPolicyForRole(final PeerRole peerRole) {
        return importPolicies.get(peerRole);
    }
}
