/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mvpn.spi.pojo.nlri;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.Mvpn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.MvpnChoice;
import org.opendaylight.yangtools.binding.CaseObject;
import org.opendaylight.yangtools.binding.lib.AbstractAugmentable;

class SimpleMvpnNlriRegistryTest {
    @Test
    void registryParseTest() {
        assertThrows(IllegalArgumentException.class,
            () -> SimpleMvpnNlriRegistry.getInstance().parseMvpn(NlriType.InterAsIPmsiAD, null));
    }

    @Test
    void registryNullTest() {
        final var body = Unpooled.buffer();
        SimpleMvpnNlriRegistry.getInstance().serializeMvpn(new NotRegistered());
        assertEquals(0, body.readableBytes());
    }

    private static final class NotRegistered extends AbstractAugmentable<NotRegistered>
            implements CaseObject<Mvpn, MvpnChoice, NotRegistered>,  MvpnChoice {
        @Override
        public Class<NotRegistered> implementedInterface() {
            return NotRegistered.class;
        }

        @Override
        public int javaHC() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean javaEQ(final NotRegistered obj) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String javaTS() {
            throw new UnsupportedOperationException();
        }
    }
}
