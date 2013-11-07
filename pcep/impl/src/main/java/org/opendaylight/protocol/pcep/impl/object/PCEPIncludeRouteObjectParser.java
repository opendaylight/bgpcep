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
import org.opendaylight.protocol.pcep.spi.EROSubobjectHandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.IncludeRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.Subobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.SubobjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.IncludeRouteBuilder;

import com.google.common.collect.Lists;

/**
 * Parser for {@link IncludeRouteObject}
 */
public class PCEPIncludeRouteObjectParser extends AbstractEROWithSubobjectsParser {

	public static final int CLASS = 10;

	public static final int TYPE = 1;

	public PCEPIncludeRouteObjectParser(final EROSubobjectHandlerRegistry subobjReg) {
		super(subobjReg);
	}

	@Override
	public IncludeRouteObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException,
			PCEPDocumentedException {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("Byte array is mandatory. Can't be null or empty.");
		}

		final IncludeRouteBuilder builder = new IncludeRouteBuilder();

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());

		final List<Subobjects> subs = Lists.newArrayList();
		for (final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobjects s : parseSubobjects(bytes)) {
			subs.add(new SubobjectsBuilder().setSubobjectType(s.getSubobjectType()).build());
		}
		builder.setSubobjects(subs);
		return builder.build();
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof IncludeRouteObject)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed IncludeRouteObject.");
		}
		final IncludeRouteObject iro = ((IncludeRouteObject) object);

		assert !(iro.getSubobjects().isEmpty()) : "Empty Include Route Object.";

		final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobjects> subs = Lists.newArrayList();

		for (final Subobjects s : iro.getSubobjects()) {
			subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectsBuilder().setLoose(
					false).setSubobjectType(s.getSubobjectType()).build());
		}

		return serializeSubobject(subs);
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
