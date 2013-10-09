package org.opendaylight.bgpcep.topology.provider.bgp.impl;

import org.opendaylight.bgpcep.topology.provider.bgp.LocRIBListener;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;

public class LinkstateTopologyBuilder implements LocRIBListener {

	@Override
	public void onLocRIBChange(final DataModification<?, ?> trans, final DataChangeEvent<?, ?> event)
			throws Exception {
		// TODO Auto-generated method stub

	}

}
