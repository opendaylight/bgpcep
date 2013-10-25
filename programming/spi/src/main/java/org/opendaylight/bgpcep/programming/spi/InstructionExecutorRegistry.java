/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionType;
import org.opendaylight.yangtools.concepts.Registration;

public interface InstructionExecutorRegistry {
	Registration<InstructionExecutor> registerInstructionExecutor(Class<? extends InstructionType> type, InstructionExecutor executor);
}
