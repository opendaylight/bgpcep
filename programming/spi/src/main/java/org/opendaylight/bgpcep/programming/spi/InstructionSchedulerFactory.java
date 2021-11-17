/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.spi;

/**
 * Factory for InstructionScheduler.
 */
public interface InstructionSchedulerFactory {
    /**
     * Creates a unique InstructionScheduler.
     *
     * @param instructionId Unique Identifier, also to be used as part of Cluster Singleton Service Group Identifier
     *                      as "instructionId"-service-group
     * @return InstructionScheduler
     */
    // FIXME: this has weird lifecycle implications. We really should be exposing some sort of an ObjectRegistration
    //        object, which exposes the SGI and allows unregistration, so that InstructionScheduler itself is in no way
    //        tied to its lifecycle and/or Cluster Singleton Service
    InstructionScheduler createInstructionScheduler(String instructionId);
}
