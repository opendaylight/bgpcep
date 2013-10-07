/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPEROSubobjectParser;
import org.opendaylight.protocol.pcep.object.PCEPIncludeRouteObject;
import org.opendaylight.protocol.pcep.spi.AbstractObjectParser;
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.IncludeRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.IncludeRouteBuilder;

/**
 * Parser for {@link IncludeRouteObject}
 */
public class PCEPIncludeRouteObjectParser extends AbstractObjectParser<IncludeRouteBuilder> {

	public PCEPIncludeRouteObjectParser(final HandlerRegistry registry) {
		super(registry);
	}

	@Override
	public IncludeRouteObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException,
			PCEPDocumentedException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Byte array is mandatory. Can't be null or empty.");

		final IncludeRouteBuilder builder = new IncludeRouteBuilder();

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());
		// FIXME: add subobjects
		return builder.build();
	}

	@Override
	public void addTlv(final IncludeRouteBuilder builder, final Tlv tlv) {
		// No tlvs defined
	}

	public byte[] put(final PCEPObject obj) {
		if (!(obj instanceof PCEPIncludeRouteObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass()
					+ ". Needed PCEPIncludeRouteObject.");

		assert !(((PCEPIncludeRouteObject) obj).getSubobjects().isEmpty()) : "Empty Include Route Object.";

		return PCEPEROSubobjectParser.put(((PCEPIncludeRouteObject) obj).getSubobjects());
	}
}
