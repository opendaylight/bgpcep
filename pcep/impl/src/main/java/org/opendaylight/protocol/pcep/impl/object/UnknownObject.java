/*
* Copyright (c) 2013 Cisco Systems, Inc. and others. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.protocol.pcep.impl.object;

import java.util.Arrays;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.impl.object.PCEPErrorObjectParser.PCEPErrorsMaping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.Errors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public class UnknownObject implements Object {		

	private final Errors error;
	private final PCEPErrors e;

	public UnknownObject(PCEPErrors error) {
		this.e = error;
		PCEPErrorsMaping maping = PCEPErrorObjectParser.PCEPErrorsMaping.getInstance();
		this.error = new ErrorsBuilder().setType(maping.getFromErrorsEnum(error).type).setValue(maping.getFromErrorsEnum(error).value).build();
	}

	public List<Errors> getErrors() {
		return Arrays.asList(this.error);
	}
	
	public PCEPErrors getError() {
		return e;
	}

	@Override
	public Class<? extends DataContainer> getImplementedInterface() {
		return Object.class;
	}

	@Override
	public Boolean isIgnore() {
		return false;
	}

	@Override
	public Boolean isProcessingRule() {
		return false;
	}
}