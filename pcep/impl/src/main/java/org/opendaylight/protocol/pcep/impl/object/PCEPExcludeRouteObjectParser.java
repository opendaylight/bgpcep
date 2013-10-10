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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ExcludeRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ExplicitRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.definition.ExplicitRouteBuilder;

/**
 * Parser for {@link ExcludeRouteObject}
 */
public final class PCEPExcludeRouteObjectParser extends AbstractObjectParser<ExcludeRouterBuilder> {

	public static final int CLASS = 7; //FIXME: to actual value

	public static final int TYPE = 1;
	
	public PCEPExcludeRouteObjectParser(HandlerRegistry registry) {
		super(registry);
	}

	@Override
	public ExcludeRouteObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException,
			PCEPDocumentedException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Byte array is mandatory. Can't be null or empty.");

		final ExcludeRouterBuilder builder = new ExcludeRouterBuilder();

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());
		// FIXME: add subobjects
		return builder.build();
	}

	@Override
	public void addTlv(final ExcludeRouterBuilder builder, final Tlv tlv) {
		// No tlvs defined
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof ExcludeRouteObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass()
					+ ". Needed ExcludeRouteObject.");

		assert !(((ExcludeRouteObject) object).getSubobjects().isEmpty()) : "Empty Excluded Route Object.";

		// return PCEPEROSubobjectParser.put(((ExplicitRouteObject) obj).getSubobjects());

		// FIXME: add subobjects
		return null;
	}

	@Override
	public int getObjectType() {
		return TYPE;
	}

	@Override
	public int getObjectClass() {
		return CLASS;
	}
}
