/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.programming.spi;

/**
 * Factory for InstructionScheduler
 */
public interface InstructionSchedulerFactory {
    /**
     * Creates a unique InstructionScheduler
     * @return
     */
    InstructionScheduler createInstructionScheduler(final String instructionId);
}
