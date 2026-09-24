#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import allure
from collections.abc import Callable
from contextlib import contextmanager
import io
import logging
import os
import pytest
from typing import ContextManager, Generator, Iterator, Callable, List, Optional, Set


from libraries import cluster
from libraries import infra
from libraries.variables import variables

ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP
RESTCONF_PORT = variables.RESTCONF_PORT
KARAF_LOG_LEVEL = variables.KARAF_LOG_LEVEL
CLUSTER_MEMBER_IPS = variables.CLUSTER_MEMBER_IPS
ODL_FEATRUES = [
    "odl-integration-compatible-with-all",
    "odl-infrautils-ready, odl-restconf-all",
    "odl-restconf-nb-rfc8040, odl-jolokia",
    "odl-bgpcep-data-change-counter",
    "odl-bgpcep-bgp",
    "odl-bgpcep-bgp-config-example",
    "odl-bgpcep-pcep",
    "odl-bgpcep-bmp",
    "odl-bgpcep-bmp-config-example",
]

log = logging.getLogger(__name__)


def pytest_addoption(parser):
    """Adds custom command-line options to pytest."""
    parser.addoption(
        "--step-include",
        action="store",
        default=None,
        help="Comma-separated list of step tags to run",
    )
    parser.addoption(
        "--step-exclude",
        action="store",
        default=None,
        help="Comma-separated list of step tags to skip",
    )


@pytest.hookimpl(trylast=True)
def pytest_collection_modifyitems(items):
    """Prevents standalone and cluster tests from running in the same session.

    Both fixtures stage their ODL from the same build output and use the same
    ports, so running them together conflicts. When both are collected,
    standalone takes priority and cluster tests are skipped.

    trylast=True ensures this runs after -m/-k filtering, so explicit
    selections (like `-m cluster`) are not overridden.

    Args:
        items (list[pytest.Item]): Tests collected for this session.

    Returns:
        None
    """
    has_standalone = any(item.get_closest_marker("standalone") for item in items)
    has_cluster = any(item.get_closest_marker("cluster") for item in items)

    if not (has_standalone and has_cluster):
        return

    skip_cluster = pytest.mark.skip(
        reason="Skipped: standalone and cluster tests cannot run in the same "
        "session; standalone takes priority."
    )
    for item in items:
        if item.get_closest_marker("cluster"):
            item.add_marker(skip_cluster)


@pytest.fixture
def allure_step_with_separate_logging(
    request: pytest.FixtureRequest,
) -> Callable[[str], ContextManager[None]]:
    """Provide context manager for Allure steps which separates logging

    This fixture extends standart allure_step context manger with functionality
    to store logs for each step seperately.

    Args:
        request (FixtureRequest): Request fixture for accessing test context.

    Returns:
        Callable: context manager for allure step with seperate logging.
    """

    @contextmanager
    def _log_step(title: str) -> Generator[any, None, None]:
        """Exectue allure step with seperate logging

        Args:
            title (str): Step title.

        Returns:
            Generator[any, None, None]: context manager for allure step
        """
        log_capture_string = io.StringIO()
        handler = logging.StreamHandler(log_capture_string)
        tox_ini_log_fromat = request.config.getini("log_format")
        formatter = logging.Formatter(tox_ini_log_fromat)
        handler.setFormatter(formatter)

        root_logger = logging.getLogger()
        root_logger.addHandler(handler)

        try:
            with allure.step(title) as allure_step:
                infra.log_message_to_karaf(f"Starting step: {title}")
                yield allure_step
        finally:
            infra.log_message_to_karaf(f"End of step: {title}")
            root_logger.removeHandler(handler)
            log_contents = log_capture_string.getvalue()
            if log_contents:
                allure.attach(
                    log_contents,
                    name=f"Logs for '{title}'",
                    attachment_type=allure.attachment_type.TEXT,
                )

    return _log_step


@pytest.fixture
def step_tag_checker(
    request: pytest.FixtureRequest,
) -> Callable[[Optional[List[str]]], bool]:
    """
    Returns a function that checks if a step should run based on tags.
    Reads --step-include and --step-exclude command-line options.

    Logic mimics Robot Framework:
    1. If --step-include is used, the step *must* match one tag.
    2. If --step-exclude is used, the step *must not* match any tag.
    """
    include_str = request.config.getoption("--step-include")
    exclude_str = request.config.getoption("--step-exclude")

    include_tags = set(include_str.split(",")) if include_str else set()
    exclude_tags = set(exclude_str.split(",")) if exclude_str else set()

    def _should_run_step(tags: Optional[str | List[str]]) -> bool:
        step_tags = {tags} if tags else set()
        if not exclude_tags.isdisjoint(step_tags):
            return False
        if include_tags and include_tags.isdisjoint(step_tags):
            return False
        return True

    return _should_run_step


@pytest.fixture(scope="session", autouse=True)
def initialize_workspace():
    """Session-wide setup to ensure clean workspace directories exist.

    Shared by the standalone and cluster setups, so the directories exist
    regardless of which topology the session runs.

    Args:
        None

    Returns:
        None
    """
    infra.shell("rm -rf tmp && mkdir tmp")
    infra.shell("ls results || mkdir results")


@pytest.fixture(scope="session")
def odl_standalone():
    """Fixture for single instance standalone test session setup.

    It handles setting features to be installed, starting karaf, etc.

    Args:
        None

    Returns:
        None
    """
    infra.start_odl_with_features(ODL_FEATRUES)
    infra.wait_for_odl_ready(timeout=580)
    infra.execute_karaf_command(f"log:set {KARAF_LOG_LEVEL}")
    yield
    infra.stop_all_karaf_instances()


@pytest.fixture(scope="session")
def odl_three_node_cluster():
    """Fixture for 3-node ODL cluster session setup.

    Stages one Karaf distribution per entry in CLUSTER_MEMBER_IPS, wires them
    into a single pekko cluster and starts every member, then waits for all
    of them to become ready.

    Args:
        None

    Returns:
        None
    """
    cluster.setup_cluster()
    cluster.start_cluster(ODL_FEATRUES)
    cluster.wait_cluster_ready(timeout=580)
    for member_ip in CLUSTER_MEMBER_IPS:
        infra.execute_karaf_command(f"log:set {KARAF_LOG_LEVEL}", host=member_ip)
    yield
    infra.stop_all_karaf_instances()


@pytest.fixture(scope="class", autouse=True)
def log_test_suite_start_end_to_karaf(request: pytest.FixtureRequest):
    """Fixture to log in karaf test suite start and end markers

    Logs to every node in cluster.active_nodes(): all cluster members for a
    cluster session, or just the single standalone node otherwise.

    Args:
        request (FixtureRequest): Request fixture for accessing test context.

    Returns:
        None
    """
    hosts = cluster.active_nodes()
    for host in hosts:
        infra.log_message_to_karaf(f"Starting suite {request.cls.__name__}", host=host)
    yield
    for host in hosts:
        infra.log_message_to_karaf(f"End of suite {request.cls.__name__}", host=host)


@pytest.fixture(scope="function", autouse=True)
def log_test_case_start_end_to_karaf(request: pytest.FixtureRequest):
    """Fixture to log in karaf test case start and end markers

    Logs to every node in cluster.active_nodes(): all cluster members for a
    cluster session, or just the single standalone node otherwise.

    Args:
        request (FixtureRequest): Request fixture for accessing test context.

    Returns:
        None
    """
    hosts = cluster.active_nodes()
    for host in hosts:
        infra.log_message_to_karaf(
            f"Starting test {request.cls.__name__}.{request.node.name}", host=host
        )
    yield
    for host in hosts:
        infra.log_message_to_karaf(
            f"End of test {request.cls.__name__}.{request.node.name}", host=host
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
    infra.shell(
        (
            "pkill gobgpd || "
            "echo 'No running instance of gobgpd script.'"
        )
    )

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
