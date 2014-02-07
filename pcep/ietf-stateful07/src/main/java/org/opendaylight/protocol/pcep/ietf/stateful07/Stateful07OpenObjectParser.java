/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.ietf.stateful07;

import org.opendaylight.protocol.pcep.impl.object.PCEPOpenObjectParser;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

/**
 * Parser for {@link Open}
 */
public class Stateful07OpenObjectParser extends PCEPOpenObjectParser {
	public Stateful07OpenObjectParser(final TlvHandlerRegistry tlvReg) {
		super(tlvReg);
	}

	@Override
	public void addTlv(final TlvsBuilder tbuilder, final Tlv tlv) {
		super.addTlv(tbuilder, tlv);

		final Tlvs2Builder statefulBuilder = new Tlvs2Builder();
		if (tbuilder.getAugmentation(Tlvs2.class) != null) {
			final Tlvs2 t = tbuilder.getAugmentation(Tlvs2.class);
			if (t.getStateful() != null) {
				statefulBuilder.setStateful(t.getStateful());
			}
		}
		if (tlv instanceof Stateful) {
			statefulBuilder.setStateful((Stateful) tlv);
		}
		tbuilder.addAugmentation(Tlvs2.class, statefulBuilder.build());
	}

	@Override
	public byte[] serializeTlvs(final Tlvs tlvs) {
		if (tlvs == null) {
			return new byte[0];
		}

		byte[] s = super.serializeTlvs(tlvs);
		int finalLength = s.length;

		byte[] statefulBytes = null;
		if (tlvs.getAugmentation(Tlvs2.class) != null) {
			final Tlvs2 statefulTlvs = tlvs.getAugmentation(Tlvs2.class);
			if (statefulTlvs.getStateful() != null) {
				statefulBytes = serializeTlv(statefulTlvs.getStateful());
				finalLength += statefulBytes.length;
			}
		}

		int offset = 0;
		final byte[] result = new byte[finalLength];
		ByteArray.copyWhole(s, result, offset);
		offset += s.length;

		if (statefulBytes != null) {
			ByteArray.copyWhole(statefulBytes, result, offset);
			offset += statefulBytes.length;
		}
		return result;
	}
}
