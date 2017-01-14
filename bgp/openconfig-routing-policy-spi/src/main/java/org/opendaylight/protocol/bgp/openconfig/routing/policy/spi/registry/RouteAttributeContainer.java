/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry;

import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

public final class RouteAttributeContainer {
    private final boolean anyConditionSatisfied;
    private final ContainerNode attributes;

    public RouteAttributeContainer(final ContainerNode attributes, final boolean anyConditionSatisfied) {
        this.anyConditionSatisfied = anyConditionSatisfied;
        this.attributes = attributes;
    }

    /**
     * Indicates whether any condition was satisfied per any condition
     * @return true if any Condition has been satisfied
     */
    public boolean anyConditionSastified() {
        return this.anyConditionSatisfied;
    }

    /**
     *
     * @return
     */
    public ContainerNode getAttributes() {
        return this.attributes;
    }
}
