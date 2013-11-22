/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OfId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.list.tlv.OfList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.list.tlv.OfListBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * Parser for {@link OfList}
 */
public class OFListTlvParser implements TlvParser, TlvSerializer {

	public static final int TYPE = 4;

	private static final int OF_CODE_ELEMENT_LENGTH = 2;
	private static final Logger LOG = LoggerFactory.getLogger(OFListTlvParser.class);

	@Override
	public OfList parseTlv(final byte[] valueBytes) throws PCEPDeserializerException {
		if (valueBytes == null || valueBytes.length == 0) {
			throw new IllegalArgumentException("Value bytes array is mandatory. Can't be null or empty.");
		}
		if (valueBytes.length % OF_CODE_ELEMENT_LENGTH != 0) {
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + valueBytes.length + ".");
		}
		final List<OfId> ofCodes = Lists.newArrayList();
		for (int i = 0; i < valueBytes.length; i += OF_CODE_ELEMENT_LENGTH) {
			try {
				ofCodes.add(new OfId(ByteArray.bytesToShort(Arrays.copyOfRange(valueBytes, i, i + OF_CODE_ELEMENT_LENGTH)) & 0xFFFF));
			} catch (final NoSuchElementException nsee) {
				LOG.debug("Unknown Objective Function encountered", nsee);
				throw new PCEPDeserializerException("Unknown OF Code inside OF Code list Tlv.", nsee);
			}
		}
		return new OfListBuilder().setCodes(ofCodes).build();
	}

	@Override
	public byte[] serializeTlv(final Tlv tlv) {
		if (tlv == null) {
			throw new IllegalArgumentException("OFListTlv is mandatory.");
		}
		final OfList oft = (OfList) tlv;

		final List<OfId> ofCodes = oft.getCodes();
		final byte[] retBytes = new byte[ofCodes.size() * OF_CODE_ELEMENT_LENGTH];

		final int size = ofCodes.size();
		for (int i = 0; i < size; i++) {
			ByteArray.copyWhole(ByteArray.shortToBytes(ofCodes.get(i).getValue().shortValue()), retBytes, i * OF_CODE_ELEMENT_LENGTH);
		}
		return retBytes;
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
