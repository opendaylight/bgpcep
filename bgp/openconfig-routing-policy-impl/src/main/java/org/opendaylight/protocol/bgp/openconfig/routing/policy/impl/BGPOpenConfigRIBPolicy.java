/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.impl;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.List;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.BGPOpenconfigRIBRoutingPolicyConsumer;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.OpenconfigPolicyConsumer;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRIBRoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.DefaultPolicyType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.apply.policy.group.apply.policy.Config;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;

public final class BGPOpenConfigRIBPolicy implements BGPOpenconfigRIBRoutingPolicyConsumer {
    private static final List<String> EMPTY_POLICY = Collections.emptyList();
    private final OpenconfigPolicyConsumer policyProvider;
    private final StatementRegistryConsumer statementRegistryConsumer;

    public BGPOpenConfigRIBPolicy(final OpenconfigPolicyConsumer policyProvider,
        final StatementRegistryConsumer statementRegistryConsumer) {
        this.policyProvider = Preconditions.checkNotNull(policyProvider);
        this.statementRegistryConsumer = Preconditions.checkNotNull(statementRegistryConsumer);
    }

    @Override
    public BGPRIBRoutingPolicy buildBGPRIBPolicy(final long localAs, final Ipv4Address bgpId,
        final ClusterIdentifier clusterId, final Config policyConfig) {
        Preconditions.checkNotNull(policyConfig);
        final DefaultPolicyType dfImportPolicy = policyConfig.getDefaultImportPolicy();
        final DefaultPolicyType dfExportPolicy = policyConfig.getDefaultExportPolicy();
        List<String> exportPolicy = policyConfig.getExportPolicy();
        if (exportPolicy == null) {
            exportPolicy = EMPTY_POLICY;
        }
        List<String> importPolicy = policyConfig.getImportPolicy();
        if (importPolicy == null) {
            importPolicy = EMPTY_POLICY;
        }
        final BGPRIBPolicyImpl policy = new BGPRIBPolicyImpl(this.policyProvider, this.statementRegistryConsumer,
            localAs, bgpId, clusterId, this.policyProvider, dfImportPolicy, dfExportPolicy, exportPolicy, importPolicy);
        return policy;
    }
}
