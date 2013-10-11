/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.base.Preconditions;

@ThreadSafe
public class HandlerRegistry<CLASS, PARSER, SERIALIZER> {
	private final Map<Class<? extends CLASS>, SERIALIZER> serializers = new ConcurrentHashMap<>();
	private final Map<Integer, PARSER> parsers = new ConcurrentHashMap<>();

	public AutoCloseable registerParser(final int type, final PARSER parser) {
		synchronized (parsers) {
			Preconditions.checkArgument(!parsers.containsKey(type), "Type %s already registered", type);
			parsers.put(type, parser);

			return new AbstractRegistration() {
				@Override
				protected void removeRegistration() {
					synchronized (parsers) {
						parsers.remove(type);
					}
				}
			};
		}
	}

	public PARSER getParser(final int type) {
		return parsers.get(type);
	}

	public AutoCloseable registerSerializer(final Class<? extends CLASS> clazz, final SERIALIZER serializer) {
		synchronized (serializers) {
			Preconditions.checkArgument(!serializers.containsKey(clazz), "Message class %s already registered", clazz);
			serializers.put(clazz, serializer);

			return new AbstractRegistration() {
				@Override
				protected void removeRegistration() {
					synchronized (serializers) {
						serializers.remove(clazz);
					}
				}
			};
		}
	}

	public SERIALIZER getSerializer(final Class<? extends CLASS> clazz) {
		return serializers.get(clazz);
	}
}
