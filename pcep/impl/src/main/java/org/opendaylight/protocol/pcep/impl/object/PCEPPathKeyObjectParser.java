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
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.object.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.object.PathKeyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.object.path.key.PathKeys;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.object.path.key.PathKeysBuilder;

import com.google.common.collect.Lists;

/**
 * Parser for {@link PathKey}
 */
public class PCEPPathKeyObjectParser extends AbstractEROWithSubobjectsParser {

	public static final int CLASS = 16;

	public static final int TYPE = 1;

	public PCEPPathKeyObjectParser(final EROSubobjectHandlerRegistry subReg) {
		super(subReg);
	}

	@Override
	public PathKey parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException {
		final PathKeyBuilder builder = new PathKeyBuilder();
		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());
		final List<PathKeys> pk = Lists.newArrayList();
		final List<Subobjects> subs = parseSubobjects(bytes);
		for (final Subobjects s : subs) {
			final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobjects.subobject.type.PathKeyCase k = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobjects.subobject.type.PathKeyCase) s.getSubobjectType();
			pk.add(new PathKeysBuilder().setLoose(s.isLoose()).setPceId(k.getPathKey().getPceId()).setPathKey(k.getPathKey().getPathKey()).build());
		}
		builder.setPathKeys(pk);
		return builder.build();
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof PathKey)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed PathKeyObject.");
		}
		final PathKey pkey = (PathKey) object;
		final List<PathKeys> pk = pkey.getPathKeys();
		final List<Subobjects> subs = Lists.newArrayList();
		for (final PathKeys p : pk) {
			subs.add(new SubobjectsBuilder().setLoose(p.isLoose()).setSubobjectType(
					new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobjects.subobject.type.PathKeyCaseBuilder().setPathKey(
							new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobjects.subobject.type.path.key._case.PathKeyBuilder().setPathKey(
									p.getPathKey()).setPceId(p.getPceId()).build()).build()).build());
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
