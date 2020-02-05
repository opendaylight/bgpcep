/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;

public interface ParameterSerializer {
    /**
     * Serialize parameter using RFC4271 encoding.
     *
     * @param parameter Parameter to serialize
     * @param output Output buffer
     * @throws NullPointerException if any argument is null
     * @throws ParameterLengthOverflowException when the parameter does not fit into 255 bytes
     */
    void serializeParameter(BgpParameters parameter, ByteBuf output) throws ParameterLengthOverflowException;

    /**
     * Serialize parameter using
     * <a href="https://tools.ietf.org/html/draft-ietf-idr-ext-opt-param-05">Extended Optional Parameters Length</a>
     * encoding.
     *
     * @param parameter Parameter to serialize
     * @param output Output buffer
     * @throws NullPointerException if any argument is null
     */
    void serializeExtendedParameter(BgpParameters parameter, ByteBuf output);
}
