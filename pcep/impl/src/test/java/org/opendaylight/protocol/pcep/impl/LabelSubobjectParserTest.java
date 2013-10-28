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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.GeneralizedLabelBuilder;

public class LabelSubobjectParserTest {

	private final byte[] generalizedLabelBytes = { (byte) 0x12, (byte) 0x00, (byte) 0x25, (byte) 0xFF };

	@Test
	public void testGeneralizedLabel() throws PCEPDeserializerException {
		final GeneralizedLabelParser parser = new GeneralizedLabelParser();
		final GeneralizedLabelBuilder builder = new GeneralizedLabelBuilder();
		builder.setGeneralizedLabel(this.generalizedLabelBytes);
		assertEquals(builder.build(), parser.parseLabel(this.generalizedLabelBytes));
		assertArrayEquals(this.generalizedLabelBytes, parser.serializeLabel(builder.build()));
	}
}
