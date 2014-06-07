/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.bgp.topology.provider;

import java.util.ArrayList;

import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.route.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHop;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.IgpNodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.Prefix;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.PrefixKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
abstract class AbstractReachabilityTopologyBuilder<T extends Route> extends AbstractTopologyBuilder<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractReachabilityTopologyBuilder.class);

    protected AbstractReachabilityTopologyBuilder(final DataProviderService dataProvider, final RibReference locRibReference,
            final TopologyId topologyId, final Class<T> idClass) {
        super(dataProvider, locRibReference, topologyId, new TopologyTypesBuilder().build(), idClass);
    }

    private NodeId advertizingNode(final Attributes attrs) {
        final CNextHop nh = attrs.getCNextHop();
        if (nh instanceof Ipv4NextHopCase) {
            final Ipv4NextHop ipv4 = ((Ipv4NextHopCase) nh).getIpv4NextHop();

            return new NodeId(ipv4.getGlobal().getValue());
        } else if (nh instanceof Ipv6NextHopCase) {
            final Ipv6NextHop ipv6 = ((Ipv6NextHopCase) nh).getIpv6NextHop();

            return new NodeId(ipv6.getGlobal().getValue());
        } else {
            LOG.warn("Unhandled next hop class {}", nh.getImplementedInterface());
            return null;
        }
    }

    private InstanceIdentifier<Node1> nodeInstanceId(final NodeId ni) {
        return getInstanceIdentifier().builder().child(Node.class, new NodeKey(ni)).augmentation(Node1.class).toInstance();
    }

    private InstanceIdentifier<Node1> ensureNodePresent(final DataModification<InstanceIdentifier<?>, DataObject> trans, final NodeId ni) {
        final InstanceIdentifier<Node1> nii = nodeInstanceId(ni);
        LOG.debug("Looking for pre-existing node at {}", nii);

        if (trans.readOperationalData(nii) == null) {
            LOG.debug("Create a new node at {}", nii);
            trans.putOperationalData(nii, new Node1Builder().setIgpNodeAttributes(
                    new IgpNodeAttributesBuilder().setPrefix(new ArrayList<Prefix>()).build()).build());
        }

        return nii;
    }

    private void removeEmptyNode(final DataModification<InstanceIdentifier<?>, DataObject> trans, final InstanceIdentifier<Node1> nii) {
        final Node1 node = (Node1) trans.readOperationalData(nii);
        if (node != null && node.getIgpNodeAttributes().getPrefix().isEmpty()) {
            trans.removeOperationalData(nii);
        }
    }

    protected abstract Attributes getAttributes(final T value);

    protected abstract IpPrefix getPrefix(final T value);

    @Override
    protected final void createObject(final DataModification<InstanceIdentifier<?>, DataObject> trans, final InstanceIdentifier<T> id,
            final T value) {
        final NodeId ni = advertizingNode(getAttributes(value));
        final InstanceIdentifier<Node1> nii = ensureNodePresent(trans, ni);

        final IpPrefix prefix = getPrefix(value);
        final PrefixKey pk = new PrefixKey(prefix);

        trans.putOperationalData(
                nii.builder().child(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.IgpNodeAttributes.class).child(
                        Prefix.class, pk).toInstance(), new PrefixBuilder().setKey(pk).setPrefix(prefix).build());
    }

    @Override
    protected final void removeObject(final DataModification<InstanceIdentifier<?>, DataObject> trans, final InstanceIdentifier<T> id,
            final T value) {
        final NodeId ni = advertizingNode(getAttributes(value));
        final InstanceIdentifier<Node1> nii = nodeInstanceId(ni);

        final IpPrefix prefix = getPrefix(value);
        final PrefixKey pk = new PrefixKey(prefix);

        trans.removeOperationalData(nii.builder().child(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.IgpNodeAttributes.class).child(
                Prefix.class, pk).toInstance());

        removeEmptyNode(trans, nii);
    }

}
