/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.subobject;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.AbstractEROPathKeySubobjectParser;
import org.opendaylight.protocol.util.ByteArray;

public final class EROPathKey128SubobjectParser extends AbstractEROPathKeySubobjectParser {

    @Override
    protected byte[] readPceId(final ByteBuf buffer) {
        return ByteArray.readBytes(buffer, PCE128_ID_F_LENGTH);
    }

    @Override
    protected void checkContentLength(final int length) throws PCEPDeserializerException {
        if (length != CONTENT128_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + length + "; Expected: >"
                + CONTENT128_LENGTH + ".");
        }
    }
}
