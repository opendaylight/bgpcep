package org.opendaylight.protocol.bgp.rib.impl;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;

public final class Activator extends AbstractBindingAwareProvider {
	@SuppressWarnings("unused")
	private RIBImpl rib;

	@Override
	public void onSessionInitiated(final ProviderContext session) {
		this.rib = new RIBImpl(session.getSALService(DataProviderService.class));
	}
}
