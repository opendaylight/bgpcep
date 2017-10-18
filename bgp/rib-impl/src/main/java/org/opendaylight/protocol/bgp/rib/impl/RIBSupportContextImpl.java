/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Verify;
import java.util.Map.Entry;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.rib.impl.spi.Codecs;
import org.opendaylight.protocol.bgp.rib.impl.spi.CodecsRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;

class RIBSupportContextImpl extends RIBSupportContext {

    private static final ContainerNode EMPTY_TABLE_ATTRIBUTES = ImmutableNodes.containerNode(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Attributes.QNAME);

    private final RIBSupport ribSupport;
    private final Codecs codecs;

    public RIBSupportContextImpl(final RIBSupport ribSupport, final CodecsRegistry codecs) {
        this.ribSupport = requireNonNull(ribSupport);
        this.codecs = codecs.getCodecs(this.ribSupport);
    }

    @Override
    public void writeRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tableId, final MpReachNlri nlri,
            final Attributes attributes) {
        final ContainerNode domNlri = this.codecs.serializeReachNlri(nlri);
        final ContainerNode routeAttributes = this.codecs.serializeAttributes(attributes);
        this.ribSupport.putRoutes(tx, tableId, domNlri, routeAttributes);
    }

    @Override
    public void createEmptyTableStructure(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tableId) {
        final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> tb = ImmutableNodes.mapEntryBuilder();
        tb.withNodeIdentifier((NodeIdentifierWithPredicates)tableId.getLastPathArgument());
        tb.withChild(EMPTY_TABLE_ATTRIBUTES);

        // tableId is keyed, but that fact is not directly visible from YangInstanceIdentifier, see BUG-2796
        final NodeIdentifierWithPredicates tableKey = (NodeIdentifierWithPredicates) tableId.getLastPathArgument();
        for (final Entry<QName, Object> e : tableKey.getKeyValues().entrySet()) {
            tb.withChild(ImmutableNodes.leafNode(e.getKey(), e.getValue()));
        }

        final ChoiceNode routes = this.ribSupport.emptyRoutes();
        Verify.verifyNotNull(routes, "Null empty routes in %s", this.ribSupport);
        Verify.verify(Routes.QNAME.equals(routes.getNodeType()), "Empty routes have unexpected identifier %s, expected %s", routes.getNodeType(), Routes.QNAME);

        tx.put(LogicalDatastoreType.OPERATIONAL, tableId, tb.withChild(routes).build());
    }

    @Override
    public void deleteRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tableId, final MpUnreachNlri nlri) {
        this.ribSupport.deleteRoutes(tx, tableId, this.codecs.serializeUnreachNlri(nlri));
    }

    @Override
    public RIBSupport getRibSupport() {
        return this.ribSupport;
    }
}
