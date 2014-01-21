/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;

/**
 * Common interface for attribute parser implementation.
 */
public interface AttributeParser {
	/**
	 * @param body encoded attribute body
	 * @param builder Path attributes builder. Guaranteed to contain all valid attributes whose type
	 *                is numerically lower than this attribute's type.
	 */
	void parseAttribute(final byte[] body, PathAttributesBuilder builder) throws BGPDocumentedException, BGPParsingException;
}
