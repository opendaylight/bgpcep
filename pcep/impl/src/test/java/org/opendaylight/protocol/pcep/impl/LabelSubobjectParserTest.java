/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.impl.subobject.GeneralizedLabelParser;
import org.opendaylight.protocol.pcep.impl.subobject.Type1LabelParser;
import org.opendaylight.protocol.pcep.impl.subobject.WavebandSwitchingLabelParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.GeneralizedLabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.Type1LabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.WavebandSwitchingLabelBuilder;

public class LabelSubobjectParserTest {

	private static final byte[] generalizedLabelBytes = { (byte) 0x12, (byte) 0x00, (byte) 0x25, (byte) 0xFF };

	private static final byte[] wavebandLabelBytes = { (byte) 0x00, (byte) 0x00, (byte) 0x12, (byte) 0x34, (byte) 0x00, (byte) 0x00,
			(byte) 0x99, (byte) 0x99, (byte) 0x00, (byte) 0x00, (byte) 0x11, (byte) 0x11 };

	@Test
	public void testGeneralizedLabel() throws PCEPDeserializerException {
		final GeneralizedLabelParser parser = new GeneralizedLabelParser();
		final GeneralizedLabelBuilder builder = new GeneralizedLabelBuilder();
		builder.setGeneralizedLabel(generalizedLabelBytes);
		assertEquals(builder.build(), parser.parseLabel(generalizedLabelBytes));
		assertArrayEquals(generalizedLabelBytes, parser.serializeLabel(builder.build()));
	}

	@Test
	public void testWavebandLabel() throws PCEPDeserializerException {
		final WavebandSwitchingLabelParser parser = new WavebandSwitchingLabelParser();
		final WavebandSwitchingLabelBuilder builder = new WavebandSwitchingLabelBuilder();
		builder.setWavebandId(0x1234L);
		builder.setStartLabel(0x9999L);
		builder.setEndLabel(0x1111L);
		assertEquals(builder.build(), parser.parseLabel(wavebandLabelBytes));
		assertArrayEquals(wavebandLabelBytes, parser.serializeLabel(builder.build()));
	}

	@Test
	public void testTypeOneLabel() throws PCEPDeserializerException {
		final Type1LabelParser parser = new Type1LabelParser();
		final Type1LabelBuilder builder = new Type1LabelBuilder();
		builder.setType1Label(0x120025ffL);
		assertEquals(builder.build(), parser.parseLabel(generalizedLabelBytes));
		assertArrayEquals(generalizedLabelBytes, parser.serializeLabel(builder.build()));
	}
}
