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
import org.opendaylight.protocol.bgp.rib.impl.spi.AbstractImportPolicy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;

/**
 * Policy database attached to a particular RIB instance. Acts as the unified
 * lookup point.
 */
final class PolicyDatabase {
    private final Map<PeerRole, AbstractExportPolicy> exportPolicies = new EnumMap<>(PeerRole.class);
    private final Map<PeerRole, AbstractImportPolicy> importPolicies = new EnumMap<>(PeerRole.class);

    PolicyDatabase(final Long localAs, final Ipv4Address bgpId, final ClusterIdentifier clusterId) {
        this.exportPolicies.put(PeerRole.Ebgp, new ToExternalExportPolicy(localAs));
        this.exportPolicies.put(PeerRole.Ibgp, new ToInternalExportPolicy(bgpId, clusterId));
        this.exportPolicies.put(PeerRole.RrClient, new ToReflectorClientExportPolicy(bgpId, clusterId));
        this.exportPolicies.put(PeerRole.Internal, new ToInternalReflectorClientExportPolicy(bgpId, clusterId));

        this.importPolicies.put(PeerRole.Ebgp, new FromExternalImportPolicy());
        this.importPolicies.put(PeerRole.Ibgp, new FromInternalImportPolicy(bgpId, clusterId));
        this.importPolicies.put(PeerRole.RrClient, new FromReflectorClientImportPolicy(bgpId, clusterId));
        this.importPolicies.put(PeerRole.Internal, new FromInternalReflectorClientImportPolicy(bgpId, clusterId));
    }

    AbstractExportPolicy exportPolicyForRole(final PeerRole peerRole) {
        return this.exportPolicies.get(peerRole);
    }

    AbstractImportPolicy importPolicyForRole(final PeerRole peerRole) {
        /*
         * TODO: this solution does not share equivalent attributes across
         *       multiple peers. If we need to do that, consider carefully:
         *       - whether the interner should be shared with RIBSupportContextImpl.writeRoutes()
         *       - lookup/update contention of both the cache and the interner
         *       - ability to share resulting attributes across import policies
         *       - ability to share resulting attributes with export policies
         */
        return new CachingImportPolicy(this.importPolicies.get(peerRole));
    }
}
