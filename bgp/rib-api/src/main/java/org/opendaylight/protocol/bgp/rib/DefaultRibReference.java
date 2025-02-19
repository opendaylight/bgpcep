/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib;

import static java.util.Objects.requireNonNull;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.RibKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

public class DefaultRibReference implements RibReference {
    private final DataObjectIdentifier.WithKey<Rib, RibKey> instanceIdentifier;

    public DefaultRibReference(final DataObjectIdentifier.WithKey<Rib, RibKey> instanceIdentifier) {
        this.instanceIdentifier = requireNonNull(instanceIdentifier);
    }

    @Override
    public final DataObjectIdentifier.WithKey<Rib, RibKey> getInstanceIdentifier() {
        return instanceIdentifier;
    }
}
