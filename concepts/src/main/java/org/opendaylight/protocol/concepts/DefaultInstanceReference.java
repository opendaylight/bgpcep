/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import static java.util.Objects.requireNonNull;

import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

public class DefaultInstanceReference<T extends DataObject> implements InstanceReference<T> {
    private final DataObjectIdentifier<T> instanceIdentifier;

    public DefaultInstanceReference(final DataObjectIdentifier<T> instanceIdentifier) {
        this.instanceIdentifier = requireNonNull(instanceIdentifier);
    }

    @Override
    public final DataObjectIdentifier<T> getInstanceIdentifier() {
        return instanceIdentifier;
    }
}
