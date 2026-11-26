/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.spi;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float128;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float64;
import org.opendaylight.yangtools.binding.EnumTypeObject;
import org.opendaylight.yangtools.binding.ScalarTypeObject;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;

public sealed interface TlvBody extends BinaryOutput permits TlvWriter {
    @Override
    TlvBody write1(int value);

    @Override
    TlvBody write1(Uint8 value);

    @Override
    TlvBody write1(ScalarTypeObject<Uint8> value);

    @Override
    TlvBody write2(int value);

    @Override
    TlvBody write2(Uint16 value);

    @Override
    TlvBody write2(ScalarTypeObject<Uint16> value);

    @Override
    TlvBody write2(EnumTypeObject value);

    @Override
    TlvBody write4(int value);

    @Override
    TlvBody write4(Uint32 value);

    @Override
    TlvBody write4(ScalarTypeObject<Uint32> value);

    @Override
    TlvBody write4(EnumTypeObject value);

    @NonNullByDefault
    TlvBody write4(Ipv4AddressNoZone value);

    @NonNullByDefault
    TlvBody write4(Float32 value);

    @Override
    TlvBody write8(long value);

    @Override
    TlvBody write8(Uint64 value);

    @Override
    TlvBody write8(ScalarTypeObject<Uint64> value);

    @NonNullByDefault
    TlvBody write8(Float64 value);

    @NonNullByDefault
    TlvBody write16(Ipv6AddressNoZone value);

    @NonNullByDefault
    TlvBody write16(Float128 value);
}
