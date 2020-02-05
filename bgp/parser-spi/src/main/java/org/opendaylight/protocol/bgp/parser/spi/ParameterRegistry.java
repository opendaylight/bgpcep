/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import java.util.Optional;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;

public interface ParameterRegistry {
    /**
     * Find a parser for specified parameter type.
     *
     * @param parameterType Parameter type
     * @return Parser, if registered
     */
    Optional<ParameterParser> findParser(int parameterType);

    /**
     * Find a serializer for specified parameter.
     *
     * @param parameter Parameter to serialize
     * @return Serializer, if registered
     * @throws NullPointerException if any argument is null
     */
    Optional<ParameterSerializer> findSerializer(BgpParameters parameter);
}
