/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Instruction Scheduler Deployer
 */
public interface IntructionDeployer {
    interface UpdateConfiguration {
        void writeConfiguration();
        void removeConfiguration();
    }

    /**
     * Creates Instruction Scheduler
     * @param instructionId Instruction Scheduler Id
     */
    void createInstruction(@Nonnull String instructionId, @Nullable UpdateConfiguration writeConfiguration);

    /**
     * Remove Instruction Scheduler
     * @param instructionId Instruction Scheduler Id
     */
    void removeInstruction(@Nonnull String instructionId);

    /**
     * Write instruction configuration on DS
     * @param instructionId Instruction Scheduler Id
     */
    void writeConfiguration(String instructionId);

    /**
     * Remove instruction configuration on DS
     * @param instructionId Instruction Scheduler Id
     */
    void removeConfiguration(String instructionId);
}
