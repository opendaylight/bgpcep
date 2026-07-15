#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import pytest

from controller_testlib import infra
from controller_testlib.fixtures import make_preconditions_fixture

from libraries.variables import variables

pytest_plugins = ["controller_testlib.fixtures"]

preconditions = make_preconditions_fixture(
    variables.ODL_FEATURES, karaf_log_level=variables.KARAF_LOG_LEVEL
)


@pytest.fixture(scope="class")
def teardown_kill_all_running_play_script_processes():
    """Fixture to stop play.py script at the end of test class execution

    Args:
        None

    Returns:
        None
    """
    yield
    infra.shell(
        (
            r"pkill -f '^(.*/)?python3?\s+.*play.py' || "
            r"echo 'No running instance of play.py script.'"
        )
    )


@pytest.fixture(scope="class")
def teardown_kill_all_running_bgp_app_peer_script_processes():
    """Fixture to stop bgp_app_peer.py script at the end of test class execution

    Args:
        None

    Returns:
        None
    """
    yield
    infra.shell(
        (
            r"pkill -f '^(.*/)?python3?\s+.*bgp_app_peer.py' || "
            r"echo 'No running instance of bgp_app_peer.py script.'"
        )
    )


@pytest.fixture(scope="class")
def teardown_kill_all_running_exabgp_processes():
    """Fixture to stop exabgp instaces at the end of test class execution

    Args:
        None

    Returns:
        None
    """
    yield
    infra.shell(
        (
            r"pkill -f '^(.*/)?python3?\s+.*bin/exabgp' || "
            r"echo 'No running instance of play.py script.'"
        )
    )


@pytest.fixture(scope="class")
def teardown_kill_all_running_gobgp_processes():
    """Fixture to stop gobpd instaces at the end of test class execution

    Args:
        None

    Returns:
        None
    """
    yield
    infra.shell(("pkill gobgpd || " "echo 'No running instance of gobgpd script.'"))


@pytest.fixture(scope="class")
def teardown_kill_all_running_pcep_pcc_mock_processes():
    """Fixture to stop pcep-pcc-mock.jar instances at the end of test class execution

    Args:
        None

    Returns:
        None
    """
    yield
    infra.shell(
        (
            r"pkill -f '^(.*/)?java\s+.*pcep-pcc-mock\.jar' || "
            r"echo 'No running instance of pcep-pcc-mock.jar.'"
        )
    )
