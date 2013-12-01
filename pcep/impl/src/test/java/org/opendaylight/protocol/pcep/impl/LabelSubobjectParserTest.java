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
import org.opendaylight.protocol.pcep.impl.subobject.GeneralizedLabelParser;
import org.opendaylight.protocol.pcep.impl.subobject.Type1LabelParser;
import org.opendaylight.protocol.pcep.impl.subobject.WavebandSwitchingLabelParser;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.GeneralizedLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.Type1LabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.WavebandSwitchingLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.generalized.label._case.GeneralizedLabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.type1.label._case.Type1LabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.waveband.switching.label._case.WavebandSwitchingLabelBuilder;

public class LabelSubobjectParserTest {

	private static final byte[] generalizedLabelBytes = { (byte) 0x12, (byte) 0x00, (byte) 0x25, (byte) 0xFF };

	private static final byte[] wavebandLabelBytes = { (byte) 0x00, (byte) 0x00, (byte) 0x12, (byte) 0x34, (byte) 0x00, (byte) 0x00,
			(byte) 0x99, (byte) 0x99, (byte) 0x00, (byte) 0x00, (byte) 0x11, (byte) 0x11 };

	@Test
	public void testGeneralizedLabel() throws PCEPDeserializerException {
		final GeneralizedLabelParser parser = new GeneralizedLabelParser();
		final GeneralizedLabelBuilder iBuilder = new GeneralizedLabelBuilder();
		iBuilder.setGeneralizedLabel(generalizedLabelBytes);
		final GeneralizedLabelCaseBuilder builder = new GeneralizedLabelCaseBuilder().setGeneralizedLabel(iBuilder.build());
		assertEquals(builder.build(), parser.parseLabel(generalizedLabelBytes));
		assertArrayEquals(generalizedLabelBytes, parser.serializeLabel(builder.build()));
	}

	@Test
	public void testWavebandLabel() throws PCEPDeserializerException {
		final WavebandSwitchingLabelParser parser = new WavebandSwitchingLabelParser();
		final WavebandSwitchingLabelBuilder iBuilder = new WavebandSwitchingLabelBuilder();
		iBuilder.setWavebandId(0x1234L);
		iBuilder.setStartLabel(0x9999L);
		iBuilder.setEndLabel(0x1111L);
		final WavebandSwitchingLabelCaseBuilder builder = new WavebandSwitchingLabelCaseBuilder().setWavebandSwitchingLabel(iBuilder.build());
		assertEquals(builder.build(), parser.parseLabel(wavebandLabelBytes));
		assertArrayEquals(wavebandLabelBytes, parser.serializeLabel(builder.build()));
	}

	@Test
	public void testTypeOneLabel() throws PCEPDeserializerException {
		final Type1LabelParser parser = new Type1LabelParser();
		final Type1LabelBuilder iBuilder = new Type1LabelBuilder();
		iBuilder.setType1Label(0x120025ffL);
		final Type1LabelCaseBuilder builder = new Type1LabelCaseBuilder().setType1Label(iBuilder.build());
		assertEquals(builder.build(), parser.parseLabel(generalizedLabelBytes));
		assertArrayEquals(generalizedLabelBytes, parser.serializeLabel(builder.build()));
	}
}
