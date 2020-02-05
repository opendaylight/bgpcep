/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry;

import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;

/**
 * Attribute Container Wrapper, provides information whether any import/export policy condition has been satisfied.
 */
public final class RouteAttributeContainer {
    private final boolean anyConditionSatisfied;
    private final Attributes attributes;

    private RouteAttributeContainer(final Attributes attributes, final boolean anyConditionSatisfied) {
        this.anyConditionSatisfied = anyConditionSatisfied;
        this.attributes = attributes;
    }

    public static RouteAttributeContainer routeAttributeContainerFalse(final Attributes attributes) {
        return new RouteAttributeContainer(attributes, false);
    }

    public static RouteAttributeContainer routeAttributeContainerTrue(final Attributes attributes) {
        return new RouteAttributeContainer(attributes, true);
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
    public @Nullable Attributes getAttributes() {
        return this.attributes;
    }
}
