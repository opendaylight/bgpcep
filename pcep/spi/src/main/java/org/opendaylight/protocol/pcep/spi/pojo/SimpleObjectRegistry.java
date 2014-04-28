/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi.pojo;

import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.UnknownObject;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yangtools.yang.binding.DataContainer;

import com.google.common.base.Preconditions;

/**
 *
 */
public final class SimpleObjectRegistry implements ObjectRegistry {
	private final HandlerRegistry<DataContainer, ObjectParser, ObjectSerializer> handlers = new HandlerRegistry<>();

	private static int createKey(final int objectClass, final int objectType) {
		Preconditions.checkArgument(objectClass >= 0 && objectClass <= Values.UNSIGNED_BYTE_MAX_VALUE);
		Preconditions.checkArgument(objectType >= 0 && objectType <= 15);
		return (objectClass << 4) | objectType;
	}

	public AutoCloseable registerObjectParser(final int objectClass, final int objectType, final ObjectParser parser) {
		Preconditions.checkArgument(objectClass >= 0 && objectClass <= Values.UNSIGNED_BYTE_MAX_VALUE, "Illagal object class %s", objectClass);
		Preconditions.checkArgument(objectType >= 0 && objectType <= 15, "Illegal object type %s", objectType);
		return this.handlers.registerParser(createKey(objectClass, objectType), parser);
	}

	public AutoCloseable registerObjectSerializer(final Class<? extends Object> objClass, final ObjectSerializer serializer) {
		return this.handlers.registerSerializer(objClass, serializer);
	}

	@Override
	public Object parseObject(int objectClass, int objectType, ObjectHeader header, byte[] buffer) throws PCEPDeserializerException {
		Preconditions.checkArgument(objectType >= 0 && objectType <= Values.UNSIGNED_SHORT_MAX_VALUE);
		final ObjectParser parser = this.handlers.getParser(createKey(objectClass, objectType));

		if (parser == null) {
		    if(!header.isProcessingRule()) {
		        return null;
		    }

			final boolean foundClass = false;

			// FIXME: BUG-187: search the parsers, check classes

			if (!foundClass) {
				return new UnknownObject(PCEPErrors.UNRECOGNIZED_OBJ_CLASS);
			} else {
				return new UnknownObject(PCEPErrors.UNRECOGNIZED_OBJ_TYPE);
			}
		}
		return parser.parseObject(header, buffer);
	}

	@Override
	public byte[] serializeObject(Object object) {
		final ObjectSerializer serializer = this.handlers.getSerializer(object.getImplementedInterface());
		if (serializer == null) {
			return null;
		}
		return serializer.serializeObject(object);
	}
}
