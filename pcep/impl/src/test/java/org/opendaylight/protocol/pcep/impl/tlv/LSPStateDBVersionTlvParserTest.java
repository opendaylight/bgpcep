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
import org.opendaylight.protocol.pcep.impl.PCEPTlvFactory;
import org.opendaylight.protocol.pcep.tlv.LSPStateDBVersionTlv;
import org.opendaylight.protocol.util.ByteArray;

public class LSPStateDBVersionTlvParserTest {
	@Test
	public void testEquality() throws IOException, PCEPDeserializerException {
		final LSPStateDBVersionTlv objToTest1a = (LSPStateDBVersionTlv) PCEPTlvFactory.parse(
				ByteArray.fileToBytes("src/test/resources/LSPStateDBVersionTlv1.bin")).get(0);
		final LSPStateDBVersionTlv objToTest1b = (LSPStateDBVersionTlv) PCEPTlvFactory.parse(
				ByteArray.fileToBytes("src/test/resources/LSPStateDBVersionTlv1.bin")).get(0);
		final LSPStateDBVersionTlv objToTest2 = (LSPStateDBVersionTlv) PCEPTlvFactory.parse(
				ByteArray.fileToBytes("src/test/resources/LSPStateDBVersionTlv2.bin")).get(0);

		assertTrue(objToTest1a.equals(objToTest1a));
		assertFalse(objToTest1a.equals(objToTest2));
		assertFalse(objToTest1a == objToTest1b);
		assertTrue(objToTest1a.equals(objToTest1b));
	}

	@Test
	public void testSerialization() throws PCEPDeserializerException, IOException {
		final byte[] bytesFromFile = ByteArray.fileToBytes("src/test/resources/LSPStateDBVersionTlv1.bin");

		final LSPStateDBVersionTlv objToTest = (LSPStateDBVersionTlv) PCEPTlvFactory.parse(bytesFromFile).get(0);
		assertEquals(objToTest.getDbVersion(), 128L);

		final byte[] bytesActual = PCEPTlvFactory.put(objToTest);

		assertArrayEquals(bytesFromFile, bytesActual);
	}

	@Test
	public void testConstruction() throws PCEPDeserializerException, IOException {
		final LSPStateDBVersionTlv expected = (LSPStateDBVersionTlv) PCEPTlvFactory.parse(
				ByteArray.fileToBytes("src/test/resources/LSPStateDBVersionTlv1.bin")).get(0);
		final LSPStateDBVersionTlv actual = new LSPStateDBVersionTlv(128L);

		assertEquals(expected, actual);
	}

	@Test(expected = PCEPDeserializerException.class)
	public void testValidityControl() throws Exception {
		/*
		 * Should throw exception
		 */
		PCEPTlvFactory.parse(ByteArray.fileToBytes("src/test/resources/PCEStatefulCapabilityTlvInvalid1.bin"));
	}
}
