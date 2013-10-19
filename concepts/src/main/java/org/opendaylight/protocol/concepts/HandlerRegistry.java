/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class HandlerRegistry<CLASS, PARSER, SERIALIZER> {
	private final MultiRegistry<Class<? extends CLASS>, SERIALIZER> serializers = new MultiRegistry<>();
	private final MultiRegistry<Integer, PARSER> parsers = new MultiRegistry<>();

	public AbstractRegistration registerParser(final int type, final PARSER parser) {
		return parsers.register(type, parser);
	}

	public PARSER getParser(final int type) {
		return parsers.get(type);
	}

	public AbstractRegistration registerSerializer(final Class<? extends CLASS> clazz, final SERIALIZER serializer) {
		return serializers.register(clazz, serializer);
	}

	public SERIALIZER getSerializer(final Class<? extends CLASS> clazz) {
		return serializers.get(clazz);
	}
}
