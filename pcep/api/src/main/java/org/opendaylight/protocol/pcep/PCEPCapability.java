package org.opendaylight.protocol.pcep;

import java.net.InetSocketAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

public interface PCEPCapability {

    void setCapabilityProposal(InetSocketAddress address, TlvsBuilder builder);

}
