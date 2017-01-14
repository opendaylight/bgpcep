/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.TagType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;

/**
 * Provides access to defined Openconfig Routing policies
 */
public interface OpenconfigPolicyConsumer {
    /**
     * Provides List of statements Condition/Action for specific policy
     * @param policyName policy Name
     * @return List of statements
     */
    @Nullable List<Statement> getPolicy(@Nonnull String policyName);

    /**
     * Check if prefix is defined under specific PrefixSet
     *
     * @param prefixSetName prefix Set Name
     * @param prefix IpPrefix
     * @return true if prefix matches
     */
    boolean matchPrefix(@Nonnull String prefixSetName, @Nonnull IpPrefix prefix);

    /**
     * Check if ipAddress is defined under specific neighborSet
     *
     * @param neighborSetName neighbor Set Name
     * @param ipAddress IpAddress
     * @return true if ipAddress matches
     */
    boolean matchNeighbor(@Nonnull String neighborSetName, @Nonnull IpAddress ipAddress);

    /**
     * Check if prefix is defined under specific PrefixSet
     *
     * @param tagSetName tag Set Name
     * @param tagType tagType
     * @return true if tagType matches
     */
    boolean matchTag(@Nonnull String tagSetName, @Nonnull TagType tagType);
}
