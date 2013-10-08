/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.CParameters;

public interface CapabilityRegistry {
	public AutoCloseable registerCapabilityParser(int capabilityType, CapabilityParser parser);
	public CapabilityParser getCapabilityParser(int capabilityType);

	public AutoCloseable registerCapabilitySerializer(Class<? extends CParameters> capabilityClass, CapabilitySerializer serializer);
	public CapabilitySerializer getCapabilitySerializer(CParameters capability);
}
