/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import java.util.Arrays;
import java.util.BitSet;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.impl.Util.BiParsersMap;
import org.opendaylight.protocol.pcep.subobject.EROGeneralizedLabelSubobject;
import org.opendaylight.protocol.pcep.subobject.EROLabelSubobject;
import org.opendaylight.protocol.pcep.subobject.EROType1LabelSubobject;
import org.opendaylight.protocol.pcep.subobject.EROWavebandSwitchingLabelSubobject;
import org.opendaylight.protocol.util.ByteArray;

public class EROLabelSubobjectParser {

    public static final int RES_F_LENGTH = 1;

    public static final int C_TYPE_F_LENGTH = 1;

    public static final int RES_F_OFFSET = 0;

    public static final int C_TYPE_F_OFFSET = RES_F_OFFSET + RES_F_LENGTH;

    public static final int HEADER_LENGTH = C_TYPE_F_OFFSET + C_TYPE_F_LENGTH;

    public static final int U_FLAG_OFFSET = 0;

    private static class MapOfParsers extends BiParsersMap<Class<? extends EROLabelSubobject>, Integer, EROLabelParser> {
	private final static MapOfParsers instance = new MapOfParsers();

	private MapOfParsers() {
	    this.fillInMap();
	}

	private void fillInMap() {
	    this.put(EROType1LabelSubobject.class, 1, new EROType1LabelSubobjectParser());
	    this.put(EROGeneralizedLabelSubobject.class, 2, new EROGeneralizedLabelSubobjectParser());
	    this.put(EROWavebandSwitchingLabelSubobject.class, 3, new EROWavebandSwitchingLabelSubobjectParser());
	}

	public static MapOfParsers getInstance() {
	    return instance;
	}
    }

    public static EROLabelSubobject parse(byte[] soContentsBytes, boolean loose) throws PCEPDeserializerException {
	if (soContentsBytes == null || soContentsBytes.length == 0)
	    throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
	if (soContentsBytes.length < HEADER_LENGTH)
	    throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + soContentsBytes.length + "; Expected: >" + HEADER_LENGTH + ".");

	final BitSet reserved = ByteArray.bytesToBitSet(Arrays.copyOfRange(soContentsBytes, RES_F_OFFSET, RES_F_LENGTH));

	final int c_type = soContentsBytes[C_TYPE_F_OFFSET] & 0xFF;

	final EROLabelParser parser = MapOfParsers.getInstance().getValueFromKeyValue(c_type);

	if (parser == null) {
	    throw new PCEPDeserializerException("Unknown C-TYPE for ero label subobject. Passed: " + c_type);
	}

	return parser.parse(ByteArray.cutBytes(soContentsBytes, HEADER_LENGTH), reserved.get(U_FLAG_OFFSET), loose);
    }

    public static byte[] put(EROLabelSubobject objToSerialize) {
	final Integer c_type = MapOfParsers.getInstance().getKeyValueFromKey(objToSerialize.getClass());
	final EROLabelParser parser = MapOfParsers.getInstance().getValueFromKeyValue(c_type);

	if (c_type == null || parser == null)
	    throw new IllegalArgumentException("Unknown EROLabelSubobject instance. Passed " + objToSerialize.getClass());

	final byte[] labelbytes = parser.put(objToSerialize);

	final byte[] retBytes = new byte[labelbytes.length + HEADER_LENGTH];

	System.arraycopy(labelbytes, 0, retBytes, HEADER_LENGTH, labelbytes.length);

	final BitSet reserved = new BitSet();
	reserved.set(U_FLAG_OFFSET, objToSerialize.isUpStream());
	System.arraycopy(ByteArray.bitSetToBytes(reserved, RES_F_LENGTH), 0, retBytes, RES_F_OFFSET, RES_F_LENGTH);

	retBytes[C_TYPE_F_OFFSET] = (byte) c_type.intValue();

	return retBytes;
    }
}
