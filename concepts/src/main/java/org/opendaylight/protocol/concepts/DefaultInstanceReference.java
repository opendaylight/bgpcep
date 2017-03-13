/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import com.google.common.base.Preconditions;

import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 *
 */
public class DefaultInstanceReference<T extends DataObject> implements InstanceReference<T> {
    private final InstanceIdentifier<T> instanceIdentifier;

    public DefaultInstanceReference(final InstanceIdentifier<T> instanceIdentifier) {
        this.instanceIdentifier = Preconditions.checkNotNull(instanceIdentifier);
    }

    @Override
    public final InstanceIdentifier<T> getInstanceIdentifier() {
        return this.instanceIdentifier;
    }
}
