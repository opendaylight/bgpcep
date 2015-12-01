/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.spi;

import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Comparator servers for comparing of configuration instances.
 * Supplies equals(), since it can produce unwanted false negative
 * results (comparing Collections with same items in different order).
 *
 * @param <T>
 */
public interface OpenConfigComparator<T extends DataObject> {

    /**
     * Compares two non-null objects.
     * @param object1
     * @param object2
     * @return The result of comparison.
     */
    boolean isSame(@Nonnull T object1, @Nonnull T object2);

}
