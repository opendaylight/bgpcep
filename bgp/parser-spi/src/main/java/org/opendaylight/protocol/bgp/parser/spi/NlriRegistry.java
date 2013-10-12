/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;

public interface NlriRegistry {
	public AutoCloseable registerNlriParser(Class<? extends AddressFamily> afi,
			Class<? extends SubsequentAddressFamily> safi, NlriParser parser);
	//	public AutoCloseable registerNlriSerializer(Class<? extends DataObject> nlriClass, NlriSerializer serializer);

	public MpUnreachNlri parseMpUnreach(final byte[] bytes) throws BGPParsingException;
	public MpReachNlri parseMpReach(final byte[] bytes) throws BGPParsingException;
	//	public byte[] serializeNlri(DataObject attribute);
}
