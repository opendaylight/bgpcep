/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.protocol.bgp.concepts.BGPObject;
import org.opendaylight.protocol.bgp.parser.BGPNode;
import org.opendaylight.protocol.bgp.parser.BGPUpdateMessage;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPUpdateMessageParser;
import org.opendaylight.protocol.bgp.util.HexDumpBGPFileParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Update;

public class BGPUpdateMessageParserTest {

	@Test
	@Ignore
	public void testNodeParsing() throws Exception {
		final List<byte[]> result = HexDumpBGPFileParser.parseMessages(new File(this.getClass().getResource("/bgp-update-nodes.txt").getFile()));
		assertEquals(1, result.size());
		final byte[] body = ByteArray.cutBytes(result.get(0), BGPMessageFactoryImpl.COMMON_HEADER_LENGTH);
		final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(result.get(0), BGPMessageFactoryImpl.MARKER_LENGTH,
				BGPMessageFactoryImpl.LENGTH_FIELD_LENGTH));
		final Update event = BGPUpdateMessageParser.parse(body, messageLength);
		final BGPUpdateMessage updateMessage = (BGPUpdateMessage) event;
		final Set<BGPObject> addedObjects = updateMessage.getAddedObjects();
		assertEquals(14, addedObjects.size());
		assertEquals(0, updateMessage.getRemovedObjects().size());
		for (final BGPObject bgpObject : addedObjects) {
			assertTrue(bgpObject instanceof BGPNode);
		}
	}
}
