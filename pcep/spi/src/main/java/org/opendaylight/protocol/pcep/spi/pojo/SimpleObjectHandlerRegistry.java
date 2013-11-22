/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi.pojo;

import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.UnknownObject;
import org.opendaylight.protocol.util.Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yangtools.yang.binding.DataContainer;

import com.google.common.base.Preconditions;

/**
 *
 */
public final class SimpleObjectHandlerRegistry implements ObjectHandlerRegistry {
	private final HandlerRegistry<DataContainer, ObjectParser, ObjectSerializer> handlers = new HandlerRegistry<>();

	private static int createKey(final int objectClass, final int objectType) {
		Preconditions.checkArgument(objectClass >= 0 && objectClass <= Util.UNSIGNED_BYTE_MAX_VALUE);
		Preconditions.checkArgument(objectType >= 0 && objectType <= 15);
		return (objectClass << 4) | objectType;
	}

	public AutoCloseable registerObjectParser(final int objectClass, final int objectType, final ObjectParser parser) {
		Preconditions.checkArgument(objectClass >= 0 && objectClass <= Util.UNSIGNED_BYTE_MAX_VALUE);
		Preconditions.checkArgument(objectType >= 0 && objectType <= 15);
		return this.handlers.registerParser(createKey(objectClass, objectType), parser);
	}

	public AutoCloseable registerObjectSerializer(final Class<? extends Object> objClass, final ObjectSerializer serializer) {
		return this.handlers.registerSerializer(objClass, serializer);
	}

	@Override
	public ObjectParser getObjectParser(final int objectClass, final int objectType) {
		final ObjectParser ret = this.handlers.getParser(createKey(objectClass, objectType));
		if (ret != null) {
			return ret;
		}

		boolean foundClass = false;

		// FIXME: search the parsers, check classes

		if (!foundClass) {
			return new ObjectParser() {
				@Override
				public Object parseObject(final ObjectHeader header, final byte[] buffer) {
					return new UnknownObject(PCEPErrors.UNRECOGNIZED_OBJ_CLASS);
				}
			};
		} else {
			return new ObjectParser() {
				@Override
				public Object parseObject(final ObjectHeader header, final byte[] buffer) {
					return new UnknownObject(PCEPErrors.UNRECOGNIZED_OBJ_TYPE);
				}
			};
		}
	}

	@Override
	public ObjectSerializer getObjectSerializer(final Object object) {
		return this.handlers.getSerializer(object.getImplementedInterface());
	}
}
