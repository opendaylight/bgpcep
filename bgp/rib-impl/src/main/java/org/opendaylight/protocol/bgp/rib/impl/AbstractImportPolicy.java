/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * Defines the internal hooks invoked when a new route appears.
 */
abstract class AbstractImportPolicy {
    /**
     * Transform incoming attributes according to policy.
     *
     * @param attributes received attributes
     * @return Filtered attributes, or null if the advertisement should be ignored.
     */
    @Nullable abstract ContainerNode effectiveAttributes(@Nullable ContainerNode attributes);

    /**
     * AIGP_SESSION indicates whether the attribute is enabled or disabled for use on that session
     *
     * @return true if AIGP attribute is enabled, false if it's disabled
     */
    abstract boolean isAigpSession();
}