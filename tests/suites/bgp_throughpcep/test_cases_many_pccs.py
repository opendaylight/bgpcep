#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# This is a suite which has both scale and performance aspects. Given scale
# target, suite reports failures if functional error is detected, or if
# various time limits expire. For passing test cases, their duration is
# the performance metric.
#
# ODL acts as a translation layer between PCEP capable devices and users
# employing RESTCONF. Performance measurement focuses on two different workflows.
#
# The first workflow is initial synchronization, when ODL learns the state of
# PCEP topology as devices connect to it, while RESTCONF user reads the state
# repeatedly. The second workflow is mass update, when RESTCONF users issue
# RPCs to updale Layer Switched Paths on Path Computation Clients.
# This suite uses pcc-mock (pre-build) to simulate PCCs. It needs
# segment of bindable IP addresses, one for each simulated PCC; so running
# pcc-mock from remote machine is only viable when just single PCC is
# simulated. Testing with multiple PCCs works best when pcc-mock runs on the
# same VM as ODL, so 127.0.0.0/8 subnet can be used.
# Library AuthStandalone is used directly for RESTCONF reads in the first
# workflow. That library transparently handles several http authentication
# methods, based on credentials and other arguments.
#
# In the second workflow, updater.py utility is used for issuing rapid
# RESTCONF requests. It can use multiple worker threads, as http requests are
# blocking. Due to CPython interpreter itself being single threaded,
# amounts of threads above 8-16 are actually slightly slower (which may
# roughly correspond to network traffic being more limiting factor than CPU).
# This suite starts updater utility bound to single CPU, as this setup was
# the most performant in other tests.
# In case of failed step, whole test fails to finish test run sooner.
# Having a tool co-located with ODL reduces network latency, but puts more pressure
# on CPU and memory on Controller VM.
# In some environments, issues with TIME-WAIT prevent high restconf rates,
# so TCP reuse is temporarily allowed during the suite run, if possible
# (and if not disabled by UPDATERVM_ENABLE_TCP_RW_REUSE option value).
# See http://vincent.bernat.im/en/blog/2014-tcp-time-wait-state-linux.html
# This suite ignores possible failures when changing reuse.
# Similarly, in some environments, handling of requests.Session object matters
# try changing RESTCONF_REUSE value to see if it helps.


# Variables to override (only if needed):
# LOG_NAME: Filename (without path) to save pcc-mock output into.
# LSPS: Number of LSPs per PCC to simulate and test.
# ODL_IP: Numeric IP address of VM where ODL runs.
# ODL_USER: Username for ssh login to ODL VM.
# ODL_PASSWD: Ssh password, empty means public keys are used instead.
# PCCS: Number of PCCs to simulate and test.
# PCEP_READY_VERIFY_TIMEOUT: Grace period for pcep-topology to appear.
# Lower if ODL is ready.
# TOOLS_IP: Numeric IP address of VM to run pcc-mock and updater from by default.
# TOOLS_PASSWD: Linux password to go with the username (empty means keys).
# TOOLS_USER: Linux username to SSH to on TOOLS_SYSTEM VM.
# UPDATER_REFRESH: Main updater thread may sleep this long. Balance precision
# with overhead.
# UPDATER_TIMEOUT: If updater stops itself if running more than this time.
# (Set this limit according to your performance target.)
# ENABLE_TCP_TW_REUSE: Set to false if changing Linux configuration is not desired.


import logging
import pytest

from suites.base_test_cases import BaseTestCases


LSPS = 100
PCCS = 150
PCEP_READY_VERIFY_TIMEOUT = 180
UPDATER_TIMEOUT = 900
ENABLE_TCP_TW_REUSE = True
RESTCONF_REUSE = True

log = logging.getLogger(__name__)


@pytest.mark.usefixtures("preconditions")
@pytest.mark.usefixtures("log_test_suite_start_end_to_karaf")
@pytest.mark.usefixtures("log_test_case_start_end_to_karaf")
@pytest.mark.usefixtures("teardown_kill_all_running_play_script_processes")
@pytest.mark.parametrize(
    "lsps, pccs, pcep_ready_verify_timeout, updater_timeout, enable_tcp_tw_reuse, restconf_reuse",
    [
        (
            LSPS,
            PCCS,
            PCEP_READY_VERIFY_TIMEOUT,
            UPDATER_TIMEOUT,
            ENABLE_TCP_TW_REUSE,
            RESTCONF_REUSE,
        )
    ],
)
@pytest.mark.run(order=13)
class TestCasesManyPccs(BaseTestCases):
    pass
