/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.common.base.Optional;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;

public class PeerRoleUtilTest {
    @Test(expected = UnsupportedOperationException.class)
    public void testPrivateConstructor() throws Throwable {
        final Constructor<PeerRoleUtil> c = PeerRoleUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test
    public void roleForChange() {
        assertNull(PeerRoleUtil.roleForChange(Optional.fromNullable(null)));
        assertEquals(PeerRole.Ebgp, PeerRoleUtil.roleForChange(Optional.of(new ImmutableLeafNodeBuilder<>()
            .withNodeIdentifier(PeerRoleUtil.PEER_ROLE_NID).withValue("ebgp").build())));
    }

    @Test
    public void roleForString() {
        assertEquals("ebgp",PeerRoleUtil.roleForString(PeerRole.Ebgp));
        assertEquals("ibgp",PeerRoleUtil.roleForString(PeerRole.Ibgp));
        assertEquals("rr-client",PeerRoleUtil.roleForString(PeerRole.RrClient));
        assertEquals("internal",PeerRoleUtil.roleForString(PeerRole.Internal));
    }
}