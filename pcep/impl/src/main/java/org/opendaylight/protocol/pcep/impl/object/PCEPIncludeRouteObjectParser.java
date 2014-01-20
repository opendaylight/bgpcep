/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import java.util.List;

import org.opendaylight.protocol.pcep.spi.EROSubobjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.IroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.iro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.iro.SubobjectBuilder;

import com.google.common.collect.Lists;

/**
 * Parser for {@link Iro}
 */
public class PCEPIncludeRouteObjectParser extends AbstractEROWithSubobjectsParser {

	public static final int CLASS = 10;

	public static final int TYPE = 1;

	public PCEPIncludeRouteObjectParser(final EROSubobjectHandlerRegistry subobjReg) {
		super(subobjReg);
	}

	@Override
	public Iro parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("Byte array is mandatory. Can't be null or empty.");
		}

		final IroBuilder builder = new IroBuilder();

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());

		final List<Subobject> subs = Lists.newArrayList();
		for (final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject s : parseSubobjects(bytes)) {
			subs.add(new SubobjectBuilder().setSubobjectType(s.getSubobjectType()).build());
		}
		builder.setSubobject(subs);
		return builder.build();
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof Iro)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed IncludeRouteObject.");
		}
		final Iro iro = ((Iro) object);

		assert !(iro.getSubobject().isEmpty()) : "Empty Include Route Object.";

		final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject> subs = Lists.newArrayList();

		for (final Subobject s : iro.getSubobject()) {
			subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder().setLoose(
					false).setSubobjectType(s.getSubobjectType()).build());
		}
		return ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), serializeSubobject(subs));
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
