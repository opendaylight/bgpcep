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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PathKeyObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Subobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.SubobjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.subobjects.subobject.type.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.object.PathKeys;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.object.PathKeysBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.path.key.expansion.PathKeyBuilder;

import com.google.common.collect.Lists;

/**
 * Parser for {@link PathKeyObject}
 */
public class PCEPPathKeyObjectParser extends AbstractEROWithSubobjectsParser {

	public static final int CLASS = 16;

	public static final int TYPE = 1;

	public PCEPPathKeyObjectParser(final EROSubobjectHandlerRegistry subReg) {
		super(subReg);
	}

	@Override
	public PathKeyObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException,
			PCEPDocumentedException {
		final PathKeyBuilder builder = new PathKeyBuilder();
		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());
		final List<PathKeys> pk = Lists.newArrayList();
		final List<Subobjects> subs = parseSubobjects(bytes);
		for (final Subobjects s : subs) {
			final PathKey k = (PathKey) s.getSubobjectType();
			pk.add(new PathKeysBuilder().setLoose(s.isLoose()).setPceId(k.getPathKey().getPceId()).setPathKey(k.getPathKey().getPathKey()).build());
		}
		builder.setPathKeys(pk);
		return builder.build();
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof PathKeyObject)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed PathKeyObject.");
		}
		final PathKeyObject pkey = (PathKeyObject) object;
		final List<PathKeys> pk = pkey.getPathKeys();
		final List<Subobjects> subs = Lists.newArrayList();
		for (final PathKeys p : pk) {
			subs.add(new SubobjectsBuilder().setLoose(p.isLoose()).setSubobjectType(
					new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.subobjects.subobject.type.PathKeyBuilder().setPathKey(
							new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.subobjects.subobject.type.path.key.PathKeyBuilder().setPathKey(
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
