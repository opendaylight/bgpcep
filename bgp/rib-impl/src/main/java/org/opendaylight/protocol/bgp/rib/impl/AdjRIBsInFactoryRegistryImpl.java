/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.controller.sal.binding.api.data.DataModification;
import org.opendaylight.protocol.bgp.rib.spi.AbstractAdjRIBsIn;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsIn;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsInFactory;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsInFactoryRegistry;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv4.routes.Ipv4RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv4.routes.Ipv4RoutesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv6.routes.Ipv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv6.routes.Ipv6RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv6.routes.Ipv6RoutesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;

public final class AdjRIBsInFactoryRegistryImpl implements AdjRIBsInFactoryRegistry {
	public static final AdjRIBsInFactoryRegistry INSTANCE;

	private final Map<TablesKey, AdjRIBsInFactory> factories = new ConcurrentHashMap<>();

	private static final class Ipv4AdjRIBsIn extends AbstractAdjRIBsIn<Ipv4Prefix, Ipv4Routes> {
		private Ipv4AdjRIBsIn(final Comparator<PathAttributes> comparator, final TablesKey key) {
			super(comparator, key);
		}

		@Override
		public InstanceIdentifier identifierForKey(final InstanceIdentifier basePath,
				final Ipv4Prefix key) {
			final InstanceIdentifierBuilder builder = InstanceIdentifier.builder(basePath);

			builder.node(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.loc.rib.tables.routes.Ipv4Routes.class);
			builder.node(Ipv4Routes.class, new Ipv4RoutesKey(key));

			return builder.toInstance();
		}

		@Override
		public void addRoutes(
				final DataModification trans,
				final Peer peer,
				final MpReachNlri nlri,
				final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributes attributes) {
			final RIBEntryData data = new RIBEntryData(attributes) {
				@Override
				protected Ipv4Routes getDataObject(final Ipv4Prefix key) {
					// FIXME: key we reuse the key object here?
					// FIXME: we have PathAttributes, how can we copy?
					return new Ipv4RoutesBuilder().setKey(new Ipv4RoutesKey(key)).setAttributes(null).build();
				}
			};

			for (Ipv4Prefix id : ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.mp.unreach.nlri.withdrawn.routes.nlri.Ipv4)nlri.getAdvertizedRoutes().getNlri()).getIpv4Prefixes()) {
				super.add(trans, peer, id, data);
			}
		}

		@Override
		public void removeRoutes(final DataModification trans, final Peer peer,
				final MpUnreachNlri nlri) {
			for (Ipv4Prefix id : ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.mp.unreach.nlri.withdrawn.routes.nlri.Ipv4)nlri.getWithdrawnRoutes().getNlri()).getIpv4Prefixes()) {
				super.remove(trans, peer, id);
			}
		}
	}

	private static final class Ipv6AdjRIBsIn extends AbstractAdjRIBsIn<Ipv6Prefix, Ipv6Routes> {
		Ipv6AdjRIBsIn(final Comparator<PathAttributes> comparator, final TablesKey key) {
			super(comparator, key);
		}

		@Override
		public InstanceIdentifier identifierForKey(final InstanceIdentifier basePath,
				final Ipv6Prefix key) {
			final InstanceIdentifierBuilder builder = InstanceIdentifier.builder(basePath);

			builder.node(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.loc.rib.tables.routes.Ipv6Routes.class);
			builder.node(Ipv6Routes.class, new Ipv6RoutesKey(key));

			return builder.toInstance();
		}

		@Override
		public void addRoutes(
				final DataModification trans,
				final Peer peer,
				final MpReachNlri nlri,
				final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributes attributes) {
			final RIBEntryData data = new RIBEntryData(attributes) {
				@Override
				protected Ipv6Routes getDataObject(final Ipv6Prefix key) {
					// FIXME: key we reuse the key object here?
					// FIXME: we have PathAttributes, how can we copy?
					return new Ipv6RoutesBuilder().setKey(new Ipv6RoutesKey(key)).setAttributes(null).build();
				}
			};

			for (Ipv6Prefix id : ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.mp.reach.nlri.advertized.routes.nlri.Ipv6)nlri.getAdvertizedRoutes().getNlri()).getIpv6Prefixes()) {
				super.add(trans, peer, id, data);
			}
		}

		@Override
		public void removeRoutes(final DataModification trans, final Peer peer,
				final MpUnreachNlri nlri) {
			for (Ipv6Prefix id : ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.mp.unreach.nlri.withdrawn.routes.nlri.Ipv6)nlri.getWithdrawnRoutes().getNlri()).getIpv6Prefixes()) {
				super.remove(trans, peer, id);
			}
		}
	}

	private static final class LinkstateAdjRIBsIn extends AbstractAdjRIBsIn<CLinkstateDestination, DataObject> {
		LinkstateAdjRIBsIn(final Comparator<PathAttributes> comparator, final TablesKey key) {
			super(comparator, key);
		}

		@Override
		public InstanceIdentifier identifierForKey(final InstanceIdentifier basePath,
				final CLinkstateDestination key) {
			final InstanceIdentifierBuilder builder = InstanceIdentifier.builder(basePath);

			switch (key.getNlriType()) {
			case Ipv4Prefix:
				// FIXME: finish this
				return builder.toInstance();
			case Ipv6Prefix:
				// FIXME: finish this
				return builder.toInstance();
			case Link:
				// FIXME: finish this
				return builder.toInstance();
			case Node:
				// FIXME: finish this
				return builder.toInstance();
			}

			throw new IllegalStateException("Unhandled link-state NLRI type " + key.getNlriType());
		}

		@Override
		public void addRoutes(
				final DataModification trans,
				final Peer peer,
				final MpReachNlri nlri,
				final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributes attributes) {
			final CLinkstateDestination key =
					((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.
							update.path.attributes.mp.reach.nlri.advertized.routes.nlri.Linkstate) nlri.getAdvertizedRoutes().getNlri()).getCLinkstateDestination();

			RIBEntryData data = null;
			switch (key.getNlriType()) {
			case Ipv4Prefix:
				data = new RIBEntryData(attributes) {
					@Override
					protected DataObject getDataObject(final CLinkstateDestination key) {
						// TODO Auto-generated method stub
						return null;
					}
				};
				break;
			case Ipv6Prefix:
				data = new RIBEntryData(attributes) {
					@Override
					protected DataObject getDataObject(final CLinkstateDestination key) {
						// TODO Auto-generated method stub
						return null;
					}
				};
				break;
			case Link:
				data = new RIBEntryData(attributes) {
					@Override
					protected DataObject getDataObject(final CLinkstateDestination key) {
						// TODO Auto-generated method stub
						return null;
					}
				};
				break;
			case Node:
				data = new RIBEntryData(attributes) {
					@Override
					protected DataObject getDataObject(final CLinkstateDestination key) {
						// TODO Auto-generated method stub
						return null;
					}
				};
				break;
			}

			if (data == null) {
				throw new IllegalStateException("Unhandled link-state NLRI type " + key.getNlriType());
			}
			super.add(trans, peer, key, data);
		}

		@Override
		public void removeRoutes(final DataModification trans, final Peer peer,
				final MpUnreachNlri nlri) {
			final CLinkstateDestination key =
					((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.
							update.path.attributes.mp.unreach.nlri.withdrawn.routes.nlri.Linkstate) nlri.getWithdrawnRoutes().getNlri()).getCLinkstateDestination();

			super.remove(trans, peer, key);
		}
	}

	static {
		final AdjRIBsInFactoryRegistry reg = new AdjRIBsInFactoryRegistryImpl();

		reg.registerAdjRIBsInFactory(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class,
				new AdjRIBsInFactory() {
			@Override
			public AdjRIBsIn createAdjRIBsIn(final Comparator<PathAttributes> comparator, final TablesKey key) {
				return new Ipv4AdjRIBsIn(comparator, key);
			}
		});
		reg.registerAdjRIBsInFactory(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class,
				new AdjRIBsInFactory() {
			@Override
			public AdjRIBsIn createAdjRIBsIn(final Comparator<PathAttributes> comparator, final TablesKey key) {
				return new Ipv6AdjRIBsIn(comparator, key);
			}
		});
		reg.registerAdjRIBsInFactory(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class,
				new AdjRIBsInFactory() {
			@Override
			public AdjRIBsIn createAdjRIBsIn(final Comparator<PathAttributes> comparator, final TablesKey key) {
				return new LinkstateAdjRIBsIn(comparator, key);
			}
		});

		INSTANCE = reg;
	}

	private AdjRIBsInFactoryRegistryImpl() {

	}

	@Override
	public synchronized AutoCloseable registerAdjRIBsInFactory(final Class<? extends AddressFamily> afi,
			final Class<? extends SubsequentAddressFamily> safi, final AdjRIBsInFactory factory) {
		final TablesKey key = new TablesKey(afi, safi);

		if (factories.containsKey(key)) {
			throw new RuntimeException("Specified AFI/SAFI combination is already registered");
		}

		factories.put(key, factory);

		final Object lock = this;
		return new AbstractRegistration() {
			@Override
			protected void removeRegistration() {
				synchronized (lock) {
					factories.remove(key);
				}
			}
		};
	}

	@Override
	public synchronized AdjRIBsInFactory getAdjRIBsInFactory(final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi) {
		return factories.get(new TablesKey(afi, safi));
	}
}
