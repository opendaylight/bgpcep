/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry;

import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

public final class RouteAttributeImportPolicyContainer {
    private final boolean anyConditionSatisfied;
    private final ContainerNode attributes;

    private RouteAttributeImportPolicyContainer(final ContainerNode attributes, final boolean anyConditionSatisfied) {
        this.anyConditionSatisfied = anyConditionSatisfied;
        this.attributes = attributes;
    }

    public static RouteAttributeImportPolicyContainer routeAttributeContainerFalse(final ContainerNode attributes) {
        return new RouteAttributeImportPolicyContainer(attributes, false);
    }

    public static RouteAttributeImportPolicyContainer routeAttributeContainerTrue(final ContainerNode attributes) {
        return new RouteAttributeImportPolicyContainer(attributes, true);
    }

    /**
     * Indicates whether any import/export condition was satisfied per any condition.
     *
     * @return true if any Condition has been satisfied
     */
    public boolean anyConditionSatisfied() {
        return this.anyConditionSatisfied;
    }

    /**
     * Returns container with route entry attributes.
     *
     * @return Attributes
     */
    @Nullable
    public ContainerNode getAttributes() {
        return this.attributes;
    }
}
