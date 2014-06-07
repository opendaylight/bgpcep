/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * A reference to an object identified by an InstanceIdentifier.
 */
public interface InstanceReference<T extends DataObject> {
    /**
     * Returns the InstanceIdentifier of the object.
     *
     * @return instance identifier
     */
    InstanceIdentifier<T> getInstanceIdentifier();
}
