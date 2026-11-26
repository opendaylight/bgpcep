/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.spi;

import com.google.common.base.MoreObjects;
import io.netty.buffer.ByteBuf;
import org.opendaylight.yangtools.yang.common.Uint16;

/**
 * A utility wrapper
 */
public final class TlvBuffer {
    private final ByteBuf buf;
    private final ByteBuf output;
    private final int type;

    public TlvBuffer(final ByteBuf output, final int type) {
        buf = output.slice(output.writerIndex(), Uint16.MAX_VALUE.toJava());
        this.output = output;
        this.type = type;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("buf", buf).toString();
    }
}
