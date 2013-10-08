package org.opendaylight.protocol.bgp.rib.impl;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;

public final class Activator extends AbstractBindingAwareProvider {
	private RIBImpl rib;

	@Override
	public void onSessionInitialized(final ConsumerContext session) {
		// Nothing to do right now
	}

	@Override
	public void onSessionInitiated(final ProviderContext session) {
		rib = new RIBImpl(session.getSALService(DataProviderService.class));
	}
}
