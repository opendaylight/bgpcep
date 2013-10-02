package org.opendaylight.protocol.bgp.rib.impl;

import java.util.Iterator;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpUnreachNlri;


public interface NlriHandler<KEY> {
	public Iterator<KEY> getKeys(MpReachNlri nlri);
	public Iterator<KEY> getKeys(MpUnreachNlri nlri);
}
