/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.explicit.route.object.ero.Subobject;

/**
 * Explicit Route Object Parser.
 */
public interface EROSubobjectParser {
    Subobject parseSubobject(ByteBuf buffer, boolean loose) throws PCEPDeserializerException;
}
