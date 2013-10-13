/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

public interface ProviderContext {
	public AddressFamilyRegistry getAddressFamilyRegistry();
	public AttributeRegistry getAttributeRegistry();
	public CapabilityRegistry getCapabilityRegistry();
	public MessageRegistry getMessageRegistry();
	public NlriRegistry getNlriRegistry();
	public ParameterRegistry getParameterRegistry();
	public SubsequentAddressFamilyRegistry getSubsequentAddressFamilyRegistry();
}
