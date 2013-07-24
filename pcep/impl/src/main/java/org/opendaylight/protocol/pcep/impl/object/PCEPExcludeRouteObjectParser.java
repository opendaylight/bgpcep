/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPObjectParser;
import org.opendaylight.protocol.pcep.impl.PCEPXROSubobjectParser;
import org.opendaylight.protocol.pcep.object.PCEPExcludeRouteObject;
import org.opendaylight.protocol.pcep.subobject.ExcludeRouteSubobject;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.object.PCEPExcludeRouteObject
 * PCEPExcludeRouteObject}
 */
public class PCEPExcludeRouteObjectParser implements PCEPObjectParser {

	/*
	 * lengths of fields in bytes
	 */
	public final int FLAGS_F_LENGTH = 2;

	/*
	 * offsets of fields in bytes
	 */
	public final int FLAGS_F_OFFSET = 2; // added reserved 2 bytes
	public final int SO_F_OFFSET = this.FLAGS_F_OFFSET + this.FLAGS_F_LENGTH;

	/*
	 * Flag offsets inside flags field in bits
	 */
	public final int F_FLAG_OFFSET = 15;

	@Override
	public PCEPObject parse(byte[] bytes, boolean processed, boolean ignored) throws PCEPDeserializerException, PCEPDocumentedException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Byte array is mandatory. Can't be null or empty.");

		final BitSet flags = ByteArray.bytesToBitSet(Arrays.copyOfRange(bytes, this.FLAGS_F_OFFSET, this.FLAGS_F_LENGTH));

		final List<ExcludeRouteSubobject> subobjects = PCEPXROSubobjectParser.parse(ByteArray.cutBytes(bytes, this.SO_F_OFFSET));
		if (subobjects.isEmpty())
			throw new PCEPDeserializerException("Empty Exclude Route Object.");

		return new PCEPExcludeRouteObject(subobjects, flags.get(this.F_FLAG_OFFSET), processed, ignored);
	}

	@Override
	public byte[] put(PCEPObject obj) {
		if (!(obj instanceof PCEPExcludeRouteObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass() + ". Needed PCEPExcludeRouteObject.");

		assert !(((PCEPExcludeRouteObject) obj).getSubobjects().isEmpty()) : "Empty Exclude Route Object.";

		final byte[] subObjsBytes = PCEPXROSubobjectParser.put(((PCEPExcludeRouteObject) obj).getSubobjects());
		final byte[] retBytes = new byte[this.SO_F_OFFSET + subObjsBytes.length];
		final BitSet flags = new BitSet(this.FLAGS_F_LENGTH * Byte.SIZE);
		flags.set(this.F_FLAG_OFFSET, ((PCEPExcludeRouteObject) obj).isFail());
		ByteArray.copyWhole(ByteArray.bitSetToBytes(flags, this.FLAGS_F_LENGTH), retBytes, this.FLAGS_F_OFFSET);
		ByteArray.copyWhole(subObjsBytes, retBytes, this.SO_F_OFFSET);

		return retBytes;
	}

}
