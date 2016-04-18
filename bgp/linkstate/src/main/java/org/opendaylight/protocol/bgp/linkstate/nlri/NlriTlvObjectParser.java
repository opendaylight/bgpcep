/*
 * Copyright (c) 2016 AT&T Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;

/**
 * Common interface for encoding TLVs of different NLRI types.
 */
public interface NlriTlvObjectParser {

    void parseNlriTlvObject(ByteBuf value, NlriTlvTypeBuilderContext context, NodeDescriptorsTlvBuilderParser builderparser, NlriType nlriType) throws BGPParsingException;

}
