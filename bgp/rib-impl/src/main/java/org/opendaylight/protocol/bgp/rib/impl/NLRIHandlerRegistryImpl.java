/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.protocol.bgp.rib.spi.NLRIHandler;
import org.opendaylight.protocol.bgp.rib.spi.NLRIHandlerRegistry;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.loc.rib.tables.routes.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.loc.rib.tables.routes.ipv4.routes.Ipv4RoutesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.loc.rib.tables.routes.ipv6.routes.Ipv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.loc.rib.tables.routes.ipv6.routes.Ipv6RoutesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;

import com.google.common.collect.Iterators;

public final class NLRIHandlerRegistryImpl implements NLRIHandlerRegistry {
	public static final NLRIHandlerRegistry INSTANCE;
	private final Map<TablesKey, NLRIHandler<?>> handlers = new ConcurrentHashMap<>();

	private static final class Ipv4NlriHandler implements NLRIHandler<Ipv4Prefix> {
		@Override
		public Iterator<Ipv4Prefix> getKeys(final MpReachNlri nlri) {
			return Iterators.unmodifiableIterator(((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.mp.reach.nlri.advertized.routes.nlri.Ipv4)nlri.getAdvertizedRoutes().getNlri()).getIpv4Prefixes().iterator());
		}

		@Override
		public Iterator<Ipv4Prefix> getKeys(final MpUnreachNlri nlri) {
			return Iterators.unmodifiableIterator(((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.mp.unreach.nlri.withdrawn.routes.nlri.Ipv4)nlri.getWithdrawnRoutes().getNlri()).getIpv4Prefixes().iterator());
		}

		@Override
		public InstanceIdentifier identifierForKey(final InstanceIdentifier basePath,
				final Ipv4Prefix key) {
			final InstanceIdentifierBuilder builder = InstanceIdentifier.builder(basePath);

			builder.node(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.loc.rib.tables.routes.Ipv4Routes.class);
			builder.node(Ipv4Routes.class, new Ipv4RoutesKey(key));

			return builder.toInstance();
		}
	}

	private static final class Ipv6NlriHandler implements NLRIHandler<Ipv6Prefix> {
		@Override
		public Iterator<Ipv6Prefix> getKeys(final MpReachNlri nlri) {
			return Iterators.unmodifiableIterator(((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.mp.reach.nlri.advertized.routes.nlri.Ipv6)nlri.getAdvertizedRoutes().getNlri()).getIpv6Prefixes().iterator());
		}

		@Override
		public Iterator<Ipv6Prefix> getKeys(final MpUnreachNlri nlri) {
			return Iterators.unmodifiableIterator(((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.mp.unreach.nlri.withdrawn.routes.nlri.Ipv6)nlri.getWithdrawnRoutes().getNlri()).getIpv6Prefixes().iterator());
		}

		@Override
		public InstanceIdentifier identifierForKey(final InstanceIdentifier basePath,
				final Ipv6Prefix key) {
			final InstanceIdentifierBuilder builder = InstanceIdentifier.builder(basePath);

			builder.node(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.loc.rib.tables.routes.Ipv6Routes.class);
			builder.node(Ipv6Routes.class, new Ipv6RoutesKey(key));

			return builder.toInstance();
		}
	}

	private static final class LinkstateNlriHandler implements NLRIHandler<Object> {
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

		@Override
		public InstanceIdentifier identifierForKey(final InstanceIdentifier basePath,
				final Object key) {
			final InstanceIdentifierBuilder builder = InstanceIdentifier.builder(basePath);

			// FIXME: finish this
			//			} else if (id instanceof LinkIdentifier) {
			//				this.links.remove(l, peer, (LinkIdentifier) id);
			//			} else if (id instanceof NodeIdentifier) {
			//				this.nodes.remove(n, peer, (NodeIdentifier) id);
			//			} else if (id instanceof PrefixIdentifier<?>) {
			//				this.prefixes.remove(p, peer, (PrefixIdentifier<?>) id);

			return builder.toInstance();
		}
	}

	static {
		final NLRIHandlerRegistry reg = new NLRIHandlerRegistryImpl();

		reg.registerHandler(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class, new Ipv4NlriHandler());
		reg.registerHandler(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class, new Ipv6NlriHandler());
		reg.registerHandler(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class, new LinkstateNlriHandler());

		INSTANCE = reg;
	}


	private NLRIHandlerRegistryImpl() {

	}

	/* (non-Javadoc)
	 * @see org.opendaylight.protocol.bgp.rib.impl.NlriRegistry#registerHandler(java.lang.Class, java.lang.Class, org.opendaylight.protocol.bgp.rib.impl.NlriHandler)
	 */
	@Override
	public synchronized AutoCloseable registerHandler(final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi,
			final NLRIHandler<?> handler) {
		final TablesKey key = new TablesKey(afi, safi);

		if (handlers.containsKey(key)) {
			throw new RuntimeException("Specified AFI/SAFI combination is already registered");
		}

		handlers.put(key, handler);

		final Object lock = this;
		return new AbstractRegistration() {
			@Override
			protected void removeRegistration() {
				synchronized (lock) {
					handlers.remove(key);
				}
			}
		};
	}

	/* (non-Javadoc)
	 * @see org.opendaylight.protocol.bgp.rib.impl.NlriRegistry#getHandler(java.lang.Class, java.lang.Class)
	 */
	@Override
	public NLRIHandler<?> getHandler(final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi) {
		return handlers.get(new TablesKey(afi, safi));
	}
}
