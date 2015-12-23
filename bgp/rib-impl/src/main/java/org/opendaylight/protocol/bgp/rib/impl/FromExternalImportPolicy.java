/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import org.opendaylight.protocol.bgp.rib.impl.spi.ImportPolicy;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * Import policy invoked on routes which we get from outside of our home AS.
 */
final class FromExternalImportPolicy implements ImportPolicy {
    @Override
    public ContainerNode effectiveAttributes(final ContainerNode attributes) {
        /*
         * Filter out non-transitive attributes, so they do not cross inter-AS
         * boundaries.
         *
         * FIXME: to be completely flexible, we need to allow for retaining
         *        the MED attribute. @see https://tools.ietf.org/html/rfc4271#section-5.1.4.
         */
        return AttributeOperations.getInstance(attributes).transitiveAttributes(attributes);
    }
}