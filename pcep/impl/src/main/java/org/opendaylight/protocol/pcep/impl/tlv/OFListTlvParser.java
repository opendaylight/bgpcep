/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import io.netty.buffer.ByteBuf;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OfId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OfListTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.tlvs.OfListBuilder;

import com.google.common.collect.Lists;

/**
 * Parser for {@link OfListTlv}
 */
public class OFListTlvParser implements TlvParser, TlvSerializer {

	private static final int OF_CODE_ELEMENT_LENGTH = 2;

	@Override
	public OfListTlv parseTlv(final byte[] valueBytes) throws PCEPDeserializerException {
		if (valueBytes == null || valueBytes.length == 0)
			throw new IllegalArgumentException("Value bytes array is mandatory. Can't be null or empty.");
		if (valueBytes.length % OF_CODE_ELEMENT_LENGTH != 0)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + valueBytes.length + ".");

		final List<OfId> ofCodes = Lists.newArrayList();
		for (int i = 0; i < valueBytes.length; i += OF_CODE_ELEMENT_LENGTH) {
			try {
				ofCodes.add(new OfId(ByteArray.bytesToShort(Arrays.copyOfRange(valueBytes, i, i + OF_CODE_ELEMENT_LENGTH)) & 0xFFFF));
			} catch (final NoSuchElementException nsee) {
				throw new PCEPDeserializerException(nsee, "Unknown OF Code inside OF Code list Tlv.");
			}
		}
		return new OfListBuilder().setCodes(ofCodes).build();
	}

	@Override
	public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
		if (tlv == null)
			throw new IllegalArgumentException("OFListTlv is mandatory.");
		final OfListTlv oft = (OfListTlv) tlv;

		final List<OfId> ofCodes = oft.getCodes();
		final byte[] retBytes = new byte[ofCodes.size() * OF_CODE_ELEMENT_LENGTH];

		final int size = ofCodes.size();
		for (int i = 0; i < size; i++) {
			ByteArray.copyWhole(ByteArray.shortToBytes(ofCodes.get(i).getValue().shortValue()), retBytes, i * OF_CODE_ELEMENT_LENGTH);
		}
		buffer.writeBytes(retBytes);
	}
}
