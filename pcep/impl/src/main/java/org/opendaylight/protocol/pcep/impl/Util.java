/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.pcep.spi.PCEPErrorMapping;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.PCEPErrorMapping.PCEPErrorIdentifier;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.SessionBuilder;

import com.google.common.collect.Lists;

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

	public static List<IpAddress> parseAddresses(final byte[] bytes, int offset, final AddressFamily family, final int addrLen) {
		final List<IpAddress> addresses = Lists.newArrayList();

		while (bytes.length > offset) {
			if (family instanceof Ipv4) {
				addresses.add(new IpAddress(Ipv4Util.addressForBytes(ByteArray.subByte(bytes, offset, addrLen))));
			} else {
				addresses.add(new IpAddress(Ipv6Util.addressForBytes(ByteArray.subByte(bytes, offset, addrLen))));
			}
			offset += addrLen;
		}

		return addresses;
	}

	public static void putAddresses(final byte[] destBytes, int offset, final List<IpAddress> addresses, final int addrLen) {
		for (final IpAddress address : addresses) {
			if (address.getIpv4Address() != null) {
				System.arraycopy(address.getIpv4Address().getValue().getBytes(), 0, destBytes, offset, addrLen);
			} else {
				System.arraycopy(address.getIpv6Address().getValue().getBytes(), 0, destBytes, offset, addrLen);
			}
			offset += addrLen;
		}
	}

	public static Message createErrorMessage(final PCEPErrors e, final Open t) {
		final PcerrBuilder errMessageBuilder = new PcerrBuilder();
		final PCEPErrorMapping mapping = PCEPErrorMapping.getInstance();
		final PCEPErrorIdentifier id = mapping.getFromErrorsEnum(e);
		final ErrorObject err = new ErrorObjectBuilder().setType(id.type).setValue(id.value).build();
		if (t == null) {
			return errMessageBuilder.setPcerrMessage(
					new PcerrMessageBuilder().setErrors(Arrays.asList(new ErrorsBuilder().setErrorObject(err).build())).build()).build();
		} else {
			final ErrorType type = new SessionBuilder().setOpen(t).build();
			return errMessageBuilder.setPcerrMessage(
					new PcerrMessageBuilder().setErrors(Arrays.asList(new ErrorsBuilder().setErrorObject(err).build())).setErrorType(type).build()).build();
		}
	}
}
