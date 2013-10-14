/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.opendaylight.protocol.concepts.AddressFamily;
import org.opendaylight.protocol.concepts.NetworkAddress;
import org.opendaylight.protocol.pcep.PCEPErrorMapping;
import org.opendaylight.protocol.pcep.PCEPErrorMapping.PCEPErrorIdentifier;
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OpenObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.Errors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.SessionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.session.Open;

/**
 * Utilities used in pcep-impl
 */
public final class Util {

	private Util() {
	}

	public static class BiParsersMap<K, KV, V> {
		private final HashMap<K, KV> kToKv = new HashMap<K, KV>();

		private final HashMap<KV, V> kvToV = new HashMap<KV, V>();

		public void put(final K key, final KV keyValue, final V value) {
			this.kToKv.put(key, keyValue);
			this.kvToV.put(keyValue, value);
		}

		public KV getKeyValueFromKey(final K key) {
			return this.kToKv.get(key);
		}

		public V getValueFromKeyValue(final KV keyValue) {
			return this.kvToV.get(keyValue);
		}
	}

	public static <T extends NetworkAddress<T>> List<T> parseAddresses(final byte[] bytes, int offset, final AddressFamily<T> family,
			final int addrLen) {
		final List<T> addresses = new ArrayList<T>();

		while (bytes.length > offset) {
			addresses.add(family.addressForBytes(ByteArray.subByte(bytes, offset, addrLen)));
			offset += addrLen;
		}

		return addresses;
	}

	public static <T extends NetworkAddress<T>> void putAddresses(final byte[] destBytes, int offset, final List<T> addresses,
			final int addrLen) {
		for (final T address : addresses) {
			System.arraycopy(address.getAddress(), 0, destBytes, offset, addrLen);
			offset += addrLen;
		}
	}

	public static int getPadding(final int length, final int padding) {
		return (padding - (length % padding)) % padding;
	}

	public static Message createErrorMessage(final PCEPErrors e, final OpenObject t) {
		final PcerrBuilder errMessageBuilder = new PcerrBuilder();
		final PCEPErrorMapping mapping = PCEPErrorMapping.getInstance();
		final PCEPErrorIdentifier id = mapping.getFromErrorsEnum(e);
		final Errors err = new ErrorsBuilder().setType(id.type).setValue(id.value).build();
		if (t == null)
			return errMessageBuilder.setPcerrMessage(new PcerrMessageBuilder().setErrors(Arrays.asList(err)).build()).build();
		else {
			final ErrorType type = new SessionBuilder().setOpen((Open) t).build();
			return errMessageBuilder.setPcerrMessage(new PcerrMessageBuilder().setErrors(Arrays.asList(err)).setErrorType(type).build()).build();
		}
	}
}
