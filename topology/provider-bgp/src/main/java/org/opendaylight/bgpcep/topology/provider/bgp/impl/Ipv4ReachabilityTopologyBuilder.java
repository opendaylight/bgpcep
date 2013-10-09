package org.opendaylight.bgpcep.topology.provider.bgp.impl;

import org.opendaylight.bgpcep.topology.provider.bgp.LocRIBListener;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;

final class Ipv4ReachabilityTopologyBuilder implements LocRIBListener {
	@Override
	public void onLocRIBChange(final DataModification<?, ?> trans, final DataChangeEvent<?, ?> event) {
		// TODO Auto-generated method stub
	}
}
