package org.opendaylight.bgpcep.topology.provider.bgp;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public final class LocRIBListeners {
	public interface Subscribtion {
		public Class<? extends AddressFamily> getAfi();
		public Class<? extends SubsequentAddressFamily> getSafi();
		public LocRIBListener getLocRIBListener();
	}

	private LocRIBListeners() {

	}

	public static ServiceRegistration<Subscribtion> subscribe(final BundleContext context,
			final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi, final LocRIBListener listener) {
		return context.registerService(Subscribtion.class,
				new Subscribtion() {
			@Override
			public Class<? extends AddressFamily> getAfi() {
				return afi;
			}

			@Override
			public Class<? extends SubsequentAddressFamily> getSafi() {
				return safi;
			}

			@Override
			public LocRIBListener getLocRIBListener() {
				return listener;
			}
		}, null);
	}
}
