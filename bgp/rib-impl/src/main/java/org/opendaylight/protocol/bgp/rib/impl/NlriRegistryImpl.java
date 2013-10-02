package org.opendaylight.protocol.bgp.rib.impl;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

import com.google.common.collect.Iterators;

public final class NlriRegistryImpl implements NlriRegistry {
	public static final NlriRegistry INSTANCE;
	private final Map<TablesKey, NlriHandler<?>> handlers = new ConcurrentHashMap<>();

	private static final class Ipv4NlriHandler implements NlriHandler<Ipv4Prefix> {
		@Override
		public Iterator<Ipv4Prefix> getKeys(final MpReachNlri nlri) {
			return Iterators.unmodifiableIterator(((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.mp.reach.nlri.advertized.routes.nlri.Ipv4)nlri.getAdvertizedRoutes().getNlri()).getIpv4Prefixes().iterator());
		}

		@Override
		public Iterator<Ipv4Prefix> getKeys(final MpUnreachNlri nlri) {
			return Iterators.unmodifiableIterator(((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.mp.unreach.nlri.withdrawn.routes.nlri.Ipv4)nlri.getWithdrawnRoutes().getNlri()).getIpv4Prefixes().iterator());
		}
	}

	private static final class Ipv6NlriHandler implements NlriHandler<Ipv6Prefix> {
		@Override
		public Iterator<Ipv6Prefix> getKeys(final MpReachNlri nlri) {
			return Iterators.unmodifiableIterator(((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.mp.reach.nlri.advertized.routes.nlri.Ipv6)nlri.getAdvertizedRoutes().getNlri()).getIpv6Prefixes().iterator());
		}

		@Override
		public Iterator<Ipv6Prefix> getKeys(final MpUnreachNlri nlri) {
			return Iterators.unmodifiableIterator(((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.mp.unreach.nlri.withdrawn.routes.nlri.Ipv6)nlri.getWithdrawnRoutes().getNlri()).getIpv6Prefixes().iterator());
		}
	}

	private static final class LinkstateNlriHandler implements NlriHandler<Object> {
		@Override
		public Iterator<Object> getKeys(final MpReachNlri nlri) {
			final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.
			update.path.attributes.mp.reach.nlri.advertized.routes.nlri.Linkstate ls =
			(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.
					update.path.attributes.mp.reach.nlri.advertized.routes.nlri.Linkstate) nlri.getAdvertizedRoutes().getNlri();

			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Iterator<Object> getKeys(final MpUnreachNlri nlri) {
			final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.
			update.path.attributes.mp.unreach.nlri.withdrawn.routes.nlri.Linkstate ls =
			(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.
					update.path.attributes.mp.unreach.nlri.withdrawn.routes.nlri.Linkstate) nlri.getWithdrawnRoutes().getNlri();

			// TODO Auto-generated method stub
			return null;
		}
	}

	static {
		final NlriRegistry reg = new NlriRegistryImpl();

		reg.registerHandler(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class, new Ipv4NlriHandler());
		reg.registerHandler(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class, new Ipv6NlriHandler());
		reg.registerHandler(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class, new LinkstateNlriHandler());

		INSTANCE = reg;
	}


	private NlriRegistryImpl() {

	}

	/* (non-Javadoc)
	 * @see org.opendaylight.protocol.bgp.rib.impl.NlriRegistry#registerHandler(java.lang.Class, java.lang.Class, org.opendaylight.protocol.bgp.rib.impl.NlriHandler)
	 */
	@Override
	public synchronized AutoCloseable registerHandler(final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi,
			final NlriHandler<?> handler) {
		final TablesKey key = new TablesKey(afi, safi);

		if (handlers.containsKey(key)) {
			throw new RuntimeException("Specified AFI/SAFI combination is already registered");
		}

		handlers.put(key, handler);

		return new AutoCloseable() {
			@Override
			public void close() {
				handlers.remove(key);
			}
		};
	}

	/* (non-Javadoc)
	 * @see org.opendaylight.protocol.bgp.rib.impl.NlriRegistry#getHandler(java.lang.Class, java.lang.Class)
	 */
	@Override
	public NlriHandler<?> getHandler(final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi) {
		return handlers.get(new TablesKey(afi, safi));
	}
}
