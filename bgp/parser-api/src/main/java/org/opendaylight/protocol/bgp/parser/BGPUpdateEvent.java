/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

/**
 * Marker interface for events resulting from parsing of an BGP UPDATE message. An unfortunate twist in BGP spec makes
 * use of a specially-crafted message to indicate that a per-AFI RIB has been completely synchronized.
 * 
 * Extends ProtocolMessage to allow parsing of BGP Update Messages in BGP listener.
 */
public interface BGPUpdateEvent extends BGPMessage {

}
