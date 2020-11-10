/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.impl;

import static java.util.Objects.requireNonNull;

import java.util.ServiceLoader;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.kohsuke.MetaInfServices;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.BGPRibRoutingPolicyFactory;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.apply.policy.group.apply.policy.Config;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.ClusterIdentifier;

@Singleton
@MetaInfServices
public final class DefaultBGPRibRoutingPolicyFactory implements BGPRibRoutingPolicyFactory {
    private final StatementRegistryConsumer statementRegistryConsumer;
    private final DataBroker databroker;

    public DefaultBGPRibRoutingPolicyFactory() {
        this(loadService(DataBroker.class), loadService(StatementRegistryConsumer.class));
    }

    @Inject
    public DefaultBGPRibRoutingPolicyFactory(final DataBroker databroker,
            final StatementRegistryConsumer statementRegistryConsumer) {
        this.databroker = requireNonNull(databroker);
        this.statementRegistryConsumer = requireNonNull(statementRegistryConsumer);
    }

    @Override
    public BGPRibRoutingPolicy buildBGPRibPolicy(final long localAs, final Ipv4AddressNoZone bgpId,
            final ClusterIdentifier clusterId, final Config policyConfig) {
        return new BGPRibPolicyImpl(databroker, statementRegistryConsumer, localAs, bgpId, clusterId,
            requireNonNull(policyConfig));
    }

    private static <T> @NonNull T loadService(final Class<T> service) {
        return ServiceLoader.load(service).findFirst()
            .orElseThrow(() -> new IllegalStateException("No " + service.getSimpleName() + " found"));
    }
}
