/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import java.util.List;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPEROSubobjectParser;
import org.opendaylight.protocol.pcep.object.PCEPExplicitRouteObject;
import org.opendaylight.protocol.pcep.spi.AbstractObjectParser;
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
import org.opendaylight.protocol.pcep.subobject.ExplicitRouteSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ExplicitRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.definition.ExplicitRouteBuilder;

/**
 * Parser for {@link ExplicitRouteObject}
 */
public class PCEPExplicitRouteObjectParser extends AbstractObjectParser<ExplicitRouteBuilder> {

	public PCEPExplicitRouteObjectParser(final HandlerRegistry registry) {
		super(registry);
	}

	@Override
	public ExplicitRouteObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException,
			PCEPDocumentedException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Byte array is mandatory. Can't be null or empty.");

		final ExplicitRouteBuilder builder = new ExplicitRouteBuilder();

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());
		// FIXME: add subobjects
		return builder.build();
	}

	@Override
	public void addTlv(final ExplicitRouteBuilder builder, final Tlv tlv) {
		// No tlvs defined
	}

	public PCEPObject parse(final byte[] bytes, final boolean processed, final boolean ignored) throws PCEPDeserializerException {

		final List<ExplicitRouteSubobject> subobjects = PCEPEROSubobjectParser.parse(bytes);
		if (subobjects.isEmpty())
			throw new PCEPDeserializerException("Empty Explicit Route Object.");

		return new PCEPExplicitRouteObject(subobjects, ignored);
	}

	public byte[] put(final PCEPObject obj) {
		if (!(obj instanceof PCEPExplicitRouteObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass()
					+ ". Needed PCEPExplicitRouteObject.");

		assert !(((PCEPExplicitRouteObject) obj).getSubobjects().isEmpty()) : "Empty Explicit Route Object.";

		return PCEPEROSubobjectParser.put(((PCEPExplicitRouteObject) obj).getSubobjects());
	}
}
