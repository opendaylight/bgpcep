/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * Import policy invoked on routes which we get from outside of our home AS.
 */
final class FromExternalImportPolicy extends AbstractImportPolicy {
    @Override
    ContainerNode effectiveAttributes(final ContainerNode attributes) {
        // FIXME: filter all non-transitive attributes
        // FIXME: but that may end up hurting our informedness
        return attributes;
    }
}