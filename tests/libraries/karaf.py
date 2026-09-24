#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import logging

from libraries import excepts
from libraries import cluster
from libraries import infra

log = logging.getLogger(__name__)


def fail_if_exception_found_during_test(step_name: str):
    """Create a failure if an Exception is found in the karaf.log

    It checks the log messages for concrete step.

     Args:
        step_name (str): Name of the step for which to check the exceptions.

    Returns:
        None
    """
    for log_dir in cluster.active_karaf_log_dirs():
        _fail_if_exception_in_log(step_name, f"{log_dir}/data/log/karaf.log")


def _fail_if_exception_in_log(step_name: str, log_path: str):
    """Fails if an Exception appears after a step marker in one karaf log.

    Args:
        step_name (str): Name of the step for which to check the exceptions.
        log_path (str): Karaf log to inspect.

    Returns:
        None
    """
    infra.wait_for_string_in_file(
        5,
        0.2,
        f"ROBOT MESSAGE: Starting step: {step_name}",
        log_path,
    )
    rc, stdout = infra.shell(
        (
            f"sed -n '/ROBOT MESSAGE: Starting step: {step_name}/,$ p' "
            f"{log_path}"
        )
    )
    assert rc == 0 and stdout, "Failed to get logs for current test step"
    exlist, matchlist = excepts.verify_exceptions(stdout)
    excepts.write_exceptions_map_to_file(step_name, "tmp/exceptions.txt")
    assert (
        len(exlist) == 0
    ), f"Not expecting to find any excpetion in karaf.log file, but found {exlist}"
