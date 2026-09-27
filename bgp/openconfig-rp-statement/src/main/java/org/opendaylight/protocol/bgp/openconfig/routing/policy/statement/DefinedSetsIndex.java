/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement;

import java.util.List;
import java.util.Set;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.as.path.sets.AsPathSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.cluster.id.set.ClusterIdSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.originator.id.set.OriginatorIdSet;

/**
 * 
 */
public interface DefinedSetsIndex {

    @Nullable
    AsPathSet lookupAsPathSet(String asPathSetRef);

    // FIXME: @Nullable
    List<Communities> lookupCommunitySet(String communitySetRef);

    // FIXME: @Nullable
    List<ExtendedCommunities> lookupExtCommunitySet(String extCommunitySetRef);

    @Nullable
    ClusterIdSet lookupClusterIdSet(String clusterIdSetRef);

    // FIXME: @Nullable
    List<PeerId> lookupNeighborSet(String neighborSetRef);

    @Nullable
    OriginatorIdSet lookupOriginatorIdSet(String originatorIdSetRef);

    Set<PeerRole> lookupRoleSets(String roleSetRef);

}