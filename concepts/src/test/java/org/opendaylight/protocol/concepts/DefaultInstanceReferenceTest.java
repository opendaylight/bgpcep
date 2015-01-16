/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.concepts;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DefaultInstanceReferenceTest {

    private static final InstanceIdentifier<NetworkTopology> IID = InstanceIdentifier.builder(NetworkTopology.class).build();

    @Test
    public void testDefaultInstanceReference() {
        final DefaultInstanceReference<NetworkTopology> defaultIID = new DefaultInstanceReference<>(IID);
        Assert.assertEquals(IID, defaultIID.getInstanceIdentifier());
    }

    @Test(expected = NullPointerException.class)
    public void testNullReference() {
        new DefaultInstanceReference<>(null);
    }

}
