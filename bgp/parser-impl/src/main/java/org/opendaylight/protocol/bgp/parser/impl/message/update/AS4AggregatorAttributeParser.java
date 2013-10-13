/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributesBuilder;

public final class AS4AggregatorAttributeParser implements AttributeParser {
	public static final int TYPE = 17;

	@Override
	public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) {
		// AS4 Aggregator is ignored
	}
}