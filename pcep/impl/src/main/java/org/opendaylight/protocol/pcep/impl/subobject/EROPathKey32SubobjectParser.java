/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.spi.AbstractEROPathKeySubobjectParser;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;

public class EROPathKey32SubobjectParser extends AbstractEROPathKeySubobjectParser {

    @Override
    protected final byte[] readPceId(final ByteBuf buffer) {
        return ByteArray.readBytes(buffer, PCE_ID_F_LENGTH);
    }

    @Override
    protected final void checkContentLenght(final int lenght) throws PCEPDeserializerException {
        if (lenght != CONTENT_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + lenght + "; Expected: >"
                + CONTENT_LENGTH + ".");
        }
    }
}
