/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import com.google.common.base.Preconditions;

import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;

final class SimpleSubsequentAddressFamilyRegistry extends AbstractFamilyRegistry<SubsequentAddressFamily, Integer> implements
        SubsequentAddressFamilyRegistry {
    AutoCloseable registerSubsequentAddressFamily(final Class<? extends SubsequentAddressFamily> clazz, final int number) {
        Preconditions.checkArgument(number >= 0 && number <= Values.UNSIGNED_BYTE_MAX_VALUE);
        return super.registerFamily(clazz, number);
    }

    @Override
    public Class<? extends SubsequentAddressFamily> classForFamily(final int number) {
        return super.classForFamily(number);
    }

    @Override
    public Integer numberForClass(final Class<? extends SubsequentAddressFamily> clazz) {
        return super.numberForClass(clazz);
    }
}
