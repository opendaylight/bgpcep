/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.EntryObject;
import org.opendaylight.yangtools.binding.Key;
import org.opendaylight.yangtools.binding.KeyStep;

public class DefaultInstanceReference<T extends DataObject> implements InstanceReference<T> {
    private final DataObjectIdentifier<T> instanceIdentifier;

    public DefaultInstanceReference(final DataObjectIdentifier<T> instanceIdentifier) {
        this.instanceIdentifier = requireNonNull(instanceIdentifier);
    }

    @Override
    public final DataObjectIdentifier<T> getInstanceIdentifier() {
        return instanceIdentifier;
    }

    public <N extends EntryObject<N, K>, K extends Key<N>> @Nullable K firstKeyOf(Class<@NonNull N> listItem) {
        for (var step : instanceIdentifier.steps()) {
            if (step instanceof KeyStep<?, ?> keyPredicate && listItem.equals(step.type())) {
                @SuppressWarnings("unchecked")
                final var ret = (K) keyPredicate.key();
                return ret;
            }
        }
        return null;
    }
}
