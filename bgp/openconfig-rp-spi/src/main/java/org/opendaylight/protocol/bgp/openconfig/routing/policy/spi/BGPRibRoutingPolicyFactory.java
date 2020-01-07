/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.apply.policy.group.apply.policy.Config;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.ClusterIdentifier;

/**
 * Factory per RIB Routing Policies .
 */
@NonNullByDefault
public interface BGPRibRoutingPolicyFactory {
    /**
     * Creates RIB Routing Policies from BGP Openconfig policy Configuration.
     *
     * @param localAs      RIB AS
     * @param bgpId        BGP ID
     * @param clusterId    Cluster Identifier
     * @param policyConfig BGP Openconfig policy Configuration
     * @return BGPRIBRoutingPolicy
     */
    BGPRibRoutingPolicy buildBGPRibPolicy(long localAs, Ipv4AddressNoZone bgpId, ClusterIdentifier clusterId,
            Config policyConfig);
}
