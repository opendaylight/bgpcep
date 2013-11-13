/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import java.util.BitSet;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPTlv;
import org.opendaylight.protocol.pcep.impl.PCEPTlvParser;
import org.opendaylight.protocol.pcep.tlv.PCEStatefulCapabilityTlv;
import org.opendaylight.protocol.util.ByteArray;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.tlv.PCEStatefulCapabilityTlv
 * PCEStatefulCapabilityTlv}
 * 
 * @see <a
 *      href="http://www.ietf.org/id/draft-crabbe-pce-pce-initiated-lsp-00.txt#section-4.1">
 *      Stateful PCE Capability TLV</a>
 */
public final class PCEStatefulCapabilityTlvParser implements PCEPTlvParser {
	
	public static final int TYPE = 16;
    /*
     * Flags field length in Bytes
     */
    public static final int FLAGS_F_LENGTH = 4;

    /*
     * Offsets inside flags field in bits;
     */
    public static final int I_FLAG_OFFSET = 29;
    public static final int S_FLAG_OFFSET = 30;
    public static final int U_FLAG_OFFSET = 31;

    public PCEStatefulCapabilityTlv parse(byte[] valueBytes) throws PCEPDeserializerException {
	if (valueBytes == null || valueBytes.length == 0)
	    throw new IllegalArgumentException("Value bytes array is mandatory. Can't be null or empty.");
	if (valueBytes.length < FLAGS_F_LENGTH)
	    throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + valueBytes.length + "; Expected: >= " + FLAGS_F_LENGTH + ".");

	final BitSet flags = ByteArray.bytesToBitSet(ByteArray.subByte(valueBytes, 0, FLAGS_F_LENGTH));
	return new PCEStatefulCapabilityTlv(flags.get(I_FLAG_OFFSET), flags.get(U_FLAG_OFFSET), flags.get(S_FLAG_OFFSET));
    }

    public byte[] put(PCEPTlv objToSerialize) {
	if (objToSerialize == null)
	    throw new IllegalArgumentException("PCEStatefulCapabilityTlv is mandatory.");
	
	PCEStatefulCapabilityTlv tlv = (PCEStatefulCapabilityTlv) objToSerialize;

	final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
	flags.set(I_FLAG_OFFSET, tlv.isInstantiated());
	flags.set(U_FLAG_OFFSET, tlv.isUpdate());
	flags.set(S_FLAG_OFFSET, tlv.isVersioned());

	return ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH);
    }
}
