/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.impl.tlv;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.impl.PCEPTlvParser;
import org.opendaylight.protocol.pcep.tlv.PCEStatefulCapabilityTlv;
import org.opendaylight.protocol.util.ByteArray;

public class PCEStatefulCapabilityTlvParserTest {
    @Test
    public void testEquality() throws IOException, PCEPDeserializerException {
	final PCEStatefulCapabilityTlv objToTest1a = (PCEStatefulCapabilityTlv) PCEPTlvParser.parseTlv(
		ByteArray.fileToBytes("src/test/resources/PCEStatefulCapabilityTlv1.bin")).get(0);
	final PCEStatefulCapabilityTlv objToTest1b = (PCEStatefulCapabilityTlv) PCEPTlvParser.parseTlv(
		ByteArray.fileToBytes("src/test/resources/PCEStatefulCapabilityTlv1.bin")).get(0);
	final PCEStatefulCapabilityTlv objToTest2 = (PCEStatefulCapabilityTlv) PCEPTlvParser.parseTlv(
		ByteArray.fileToBytes("src/test/resources/PCEStatefulCapabilityTlv2.bin")).get(0);

	assertTrue(objToTest1a.equals(objToTest1a));
	assertFalse(objToTest1a.equals(objToTest2));
	assertFalse(objToTest1a == objToTest1b);
	assertTrue(objToTest1a.equals(objToTest1b));
    }

    @Test
    public void testSerialization() throws PCEPDeserializerException, IOException {
	final byte[] bytesFromFile = ByteArray.fileToBytes("src/test/resources/PCEStatefulCapabilityTlv1.bin");

	final PCEStatefulCapabilityTlv objToTest = (PCEStatefulCapabilityTlv) PCEPTlvParser.parseTlv(bytesFromFile).get(0);
	assertTrue(objToTest.isUpdate());
	assertTrue(objToTest.isVersioned());

	final byte[] bytesActual = PCEPTlvParser.put(objToTest);

	assertArrayEquals(bytesFromFile, bytesActual);
    }

    @Test
    public void testConstruction() throws PCEPDeserializerException, IOException {
	final PCEStatefulCapabilityTlv expected = (PCEStatefulCapabilityTlv) PCEPTlvParser.parseTlv(
		ByteArray.fileToBytes("src/test/resources/PCEStatefulCapabilityTlv1.bin")).get(0);

	final PCEStatefulCapabilityTlv actual = new PCEStatefulCapabilityTlv(false, true, true);

	assertEquals(expected, actual);
    }

    @Test(expected = PCEPDeserializerException.class)
    public void testValidityControl() throws Exception {
	/*
	 * Should throw exception
	 */
	PCEPTlvParser.parseTlv(ByteArray.fileToBytes("src/test/resources/PCEStatefulCapabilityTlvInvalid1.bin"));
    }

}
