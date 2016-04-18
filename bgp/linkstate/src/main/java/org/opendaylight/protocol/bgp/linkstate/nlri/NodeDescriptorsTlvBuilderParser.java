/*
 * Copyright (c) 2016 AT&T Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.AreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.DomainIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.CRouterIdentifier;

public interface NodeDescriptorsTlvBuilderParser {

    void setAsNumBuilder(AsNumber asNum, NlriTlvTypeBuilderContext context);

    void setAreaIdBuilder(AreaIdentifier ai, NlriTlvTypeBuilderContext context);

    void setCRouterIdBuilder(CRouterIdentifier CRouterId, NlriTlvTypeBuilderContext context);

    void setDomainIdBuilder(DomainIdentifier bgpId, NlriTlvTypeBuilderContext context);

    NodeIdentifier buildNodeDescriptors(NlriTlvTypeBuilderContext context);

}
