/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.impl;

import static com.google.common.base.Verify.verifyNotNull;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.BGPRibRoutingPolicyFactory;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.apply.policy.group.apply.policy.Config;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.ClusterIdentifier;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true)
// FIXME: integrate with DefaultBGPRibRoutingPolicyFactory once we have OSGi R7
public final class OSGiBGPRibRoutingPolicyFactory implements BGPRibRoutingPolicyFactory {
    @Reference
    DataBroker dataBroker;
    @Reference
    StatementRegistryConsumer statementRegistryConsumer;

    private DefaultBGPRibRoutingPolicyFactory delegate;

    @Override
    public BGPRibRoutingPolicy buildBGPRibPolicy(final long localAs, final Ipv4AddressNoZone bgpId,
            final ClusterIdentifier clusterId, final Config policyConfig) {
        return verifyNotNull(delegate).buildBGPRibPolicy(localAs, bgpId, clusterId, policyConfig);
    }

    @Activate
    void activate() {
        delegate = new DefaultBGPRibRoutingPolicyFactory(dataBroker, statementRegistryConsumer);
    }

    @Deactivate
    void deactive() {
        delegate = null;
    }
}
