package org.opendaylight.bgpcep.pcep.tunnel.provider;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public final class BundleActivator extends AbstractBindingAwareProvider {
	private static final Logger LOG = LoggerFactory.getLogger(BundleActivator.class);

	@Override
	public void onSessionInitiated(final ProviderContext session) {
		final DataProviderService dps = Preconditions.checkNotNull(session.getSALService(DataProviderService.class));

		// FIXME: migrate to config subsystem
		final TunnelTopologyExporter tte = new TunnelTopologyExporter(dps, null);
		tte.addTargetTopology(null);
	}
}
