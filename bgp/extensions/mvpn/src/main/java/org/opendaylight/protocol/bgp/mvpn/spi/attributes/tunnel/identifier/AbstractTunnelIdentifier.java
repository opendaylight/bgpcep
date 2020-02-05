/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mvpn.spi.attributes.tunnel.identifier;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel.pmsi.tunnel.TunnelIdentifier;

/**
 * Common Abstract Tunnel Identifier.
 * @author Claudio D. Gasparini
 */
public abstract class AbstractTunnelIdentifier<T extends TunnelIdentifier>
        implements TunnelIdentifierSerializer<T>, TunnelIdentifierParser<T> {
}
