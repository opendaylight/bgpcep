/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf;

import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.ietf.stateful02.PCEPOpenObjectParser;
import org.opendaylight.protocol.pcep.ietf.stateful02.PCEPLspaObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectHeaderImpl;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.lsp.cleanup.tlv.LspCleanupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Tlvs2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Tlvs2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AttributeFilter;

public class PCEPObjectParserTest {

	private TlvHandlerRegistry tlvRegistry;

	@Before
	public void setUp() throws Exception {
		this.tlvRegistry = ServiceLoaderPCEPExtensionProviderContext.create().getTlvHandlerRegistry();
	}

	@Test
	public void testOpenObjectWithTLV() throws PCEPDeserializerException, IOException {
		final PCEPOpenObjectParser parser = new PCEPOpenObjectParser(this.tlvRegistry);
		final byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPOpenObject1.bin");

		final OpenBuilder builder = new OpenBuilder();
		builder.setProcessingRule(false);
		builder.setIgnore(false);
		builder.setVersion(new ProtocolVersion((short) 1));
		builder.setKeepalive((short) 30);
		builder.setDeadTimer((short) 120);
		builder.setSessionId((short) 1);

		final Stateful tlv1 = new StatefulBuilder().setLspUpdateCapability(Boolean.TRUE).setIncludeDbVersion(Boolean.FALSE).addAugmentation(
				Stateful1.class, new Stateful1Builder().setInitiation(true).build()).build();

		final Tlvs2Builder statBuilder = new Tlvs2Builder();
		statBuilder.setStateful(tlv1);

		final Tlvs1Builder cleanupBuilder = new Tlvs1Builder();
		cleanupBuilder.setLspCleanup(new LspCleanupBuilder().setTimeout(180L).build());

		builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder().addAugmentation(
				Tlvs2.class, statBuilder.build()).addAugmentation(Tlvs1.class, cleanupBuilder.build()).build());

		// FIXME: enable once the registry is rewritten
		// assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false),
		// ByteArray.cutBytes(result, 4)));
		// assertArrayEquals(result, parser.serializeObject(builder.build()));
	}

	@Test
	public void testLspaObjectWithTlv() throws IOException, PCEPDeserializerException {
		final PCEPLspaObjectParser parser = new PCEPLspaObjectParser(this.tlvRegistry);
		final byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPLspaObject1LowerBounds.bin");

		final LspaBuilder builder = new LspaBuilder();
		builder.setIgnore(false);
		builder.setProcessingRule(false);
		builder.setIncludeAny(new AttributeFilter(0l));
		builder.setExcludeAny(new AttributeFilter(0l));
		builder.setIncludeAll(new AttributeFilter(0l));
		builder.setSetupPriority((short) 0);
		builder.setHoldPriority((short) 0);
		builder.setLocalProtectionDesired(false);

		final SymbolicPathName tlv = new SymbolicPathNameBuilder().setPathName(
				new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.SymbolicPathName(new byte[] {
						(byte) 0x4d, (byte) 0x65, (byte) 0x64, (byte) 0x20, (byte) 0x74, (byte) 0x65, (byte) 0x73, (byte) 0x74,
						(byte) 0x20, (byte) 0x6f, (byte) 0x66, (byte) 0x20, (byte) 0x73, (byte) 0x79, (byte) 0x6d, (byte) 0x62,
						(byte) 0x6f, (byte) 0x6c, (byte) 0x69, (byte) 0x63, (byte) 0x20, (byte) 0x6e, (byte) 0x61, (byte) 0x6d, (byte) 0x65 })).build();

		builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.lspa.TlvsBuilder().addAugmentation(
				org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Tlvs2.class,
				new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Tlvs2Builder().setSymbolicPathName(
						tlv).build()).build());
		// Tlvs container does not contain toString
		// assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false),
		// ByteArray.cutBytes(result, 4)));
		assertArrayEquals(result, parser.serializeObject(builder.build()));
	}

	@Test
	public void testLspObjectWithTLV() throws IOException, PCEPDeserializerException {
		// final PCEPLspObjectParser parser = new PCEPLspObjectParser(this.tlvRegistry);
		// final byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPLspObject1WithTLV.bin");
		//
		// final LspBuilder builder = new LspBuilder();
		// builder.setProcessingRule(true);
		// builder.setIgnore(true);
		// builder.setDelegate(false);
		// builder.setRemove(true);
		// builder.setSync(false);
		//
		// final LspErrorCode tlv1 = new LspErrorCodeBuilder().setErrorCode(627610883L).build();
		// final SymbolicPathName tlv2 = new SymbolicPathNameBuilder().setPathName(
		// new
		// org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.SymbolicPathName("Med".getBytes())).build();
		// builder.setTlvs(new
		// org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.TlvsBuilder().setLspErrorCode(
		// tlv1).setSymbolicPathName(tlv2).build());
		// assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), ByteArray.cutBytes(result,
		// 4)));
		// assertArrayEquals(result, parser.serializeObject(builder.build()));
	}
}
