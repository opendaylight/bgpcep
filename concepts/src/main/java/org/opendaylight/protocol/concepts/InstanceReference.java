/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.EntryObject;
import org.opendaylight.yangtools.binding.Key;

/**
 * A reference to an object identified by an {@link DataObjectIdentifier}.
 */
public interface InstanceReference<T extends DataObject> {
    /**
     * Returns the InstanceIdentifier of the object.
     *
     * @return instance identifier
     */
    DataObjectIdentifier<T> getInstanceIdentifier();

    /**
     * Return the key associated with the first instance identifier.
     *
     * @param listItem component type
     * @return key associated with the component, or null if the component type
     *         is not present.
     */
    <N extends EntryObject<N, K>, K extends Key<N>> @Nullable K firstKeyOf(Class<@NonNull N> listItem);
}
