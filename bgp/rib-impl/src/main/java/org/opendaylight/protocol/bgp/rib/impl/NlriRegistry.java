package org.opendaylight.protocol.bgp.rib.impl;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;

public interface NlriRegistry {

	public AutoCloseable registerHandler(Class<? extends AddressFamily> afi,
			Class<? extends SubsequentAddressFamily> safi, NlriHandler<?> handler);

	public NlriHandler<?> getHandler(Class<? extends AddressFamily> afi,
			Class<? extends SubsequentAddressFamily> safi);

}