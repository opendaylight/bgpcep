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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.MvpnChoice;

final class SimpleMvpnNlriRegistryTest {
    @Test
    void registryParseTest() {
        assertThrows(IllegalArgumentException.class,
            () -> SimpleMvpnNlriRegistry.getInstance().parseMvpn(NlriType.InterAsIPmsiAD, null));
    }

    @Test
    void registryNullTest() {
        final ByteBuf body = Unpooled.buffer();
        SimpleMvpnNlriRegistry.getInstance().serializeMvpn(new NotRegistered());
        assertEquals(0, body.readableBytes());
    }

    private static final class NotRegistered implements MvpnChoice {
        @Override
        public Class<NotRegistered> implementedInterface() {
            return NotRegistered.class;
        }
    }

}
