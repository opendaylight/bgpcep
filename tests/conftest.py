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
    """Keeps a session down to one ODL, whatever shape it has.

    Every ODL fixture here stages its distribution from the same build
    output into the same directories and binds the same ports, and a
    session fixture is torn down only once the session ends. A second one
    in the same session is therefore set up on top of the first rather than
    after it.

    Two things are kept apart. Standalone and cluster tests cannot share a
    session, and standalone takes priority. Neither can two cluster
    layouts: `pytest -m cluster` collects suites wanting the shard layout
    the distribution ships and suites wanting an unreplicated RIB, and the
    layout of whichever runs first takes priority.

    Keyed on the fixtures rather than the markers, so a suite that requests
    a cluster fixture without marking itself is still caught. Any fixture
    whose name starts with odl_three_node_cluster counts, so a new cluster
    variant is covered the day it is added, and two tests are taken to
    share a cluster when they have one of those fixtures in common: a
    fixture layered on another, such as odl_three_node_cluster_bgp, names
    the one it builds on as well, while a separate layout names none of
    them.

    trylast=True ensures this runs after -m/-k filtering and after the
    ordering plugin, so explicit selections (like `-m cluster`) are not
    overridden and the layout kept is the one that would have run first.

    Args:
        items (list[pytest.Item]): Tests collected for this session.

    Returns:
        None
    """

    def uses(item: pytest.Item, fixture: str) -> bool:
        return fixture in getattr(item, "fixturenames", ())

    def cluster_fixtures(item: pytest.Item) -> frozenset[str]:
        return frozenset(
            name
            for name in getattr(item, "fixturenames", ())
            if name.startswith("odl_three_node_cluster")
        )

    has_standalone = any(uses(item, "odl_standalone") for item in items)
    has_cluster = any(cluster_fixtures(item) for item in items)

    if has_standalone and has_cluster:
        skip_cluster = pytest.mark.skip(
            reason="Skipped: standalone and cluster tests cannot run in the "
            "same session; standalone takes priority."
        )
        for item in items:
            if cluster_fixtures(item):
                item.add_marker(skip_cluster)
        return

    kept: frozenset[str] = frozenset()
    kept_name = ""
    for item in items:
        used = cluster_fixtures(item)
        if not used:
            continue
        if not kept:
            kept, kept_name = used, min(used)
            continue
        if used & kept:
            kept |= used
            continue
        item.add_marker(
            pytest.mark.skip(
                reason="Skipped: a session can run only one cluster layout; "
                f"the one {kept_name} sets up takes priority."
            )
        )


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

        hosts = cluster.active_nodes()
        try:
            with allure.step(title) as allure_step:
                for host in hosts:
                    infra.log_message_to_karaf(f"Starting step: {title}", host=host)
                yield allure_step
        finally:
            for host in hosts:
                infra.log_message_to_karaf(f"End of step: {title}", host=host)
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
    try:
        infra.start_odl_with_features(ODL_FEATRUES)
        infra.wait_for_odl_ready(timeout=580)
        infra.execute_karaf_command(f"log:set {KARAF_LOG_LEVEL}")
        yield
    finally:
        infra.stop_all_karaf_instances()


def _run_three_node_cluster(
    modules: dict[str, str] | None = None,
    local_only_shards: tuple[str, ...] = (),
    align_bgp: bool = False,
    align_topology: bool = False,
) -> Iterator[None]:
    """Runs a 3-node cluster for a session, in the given shard layout.

    Stages one Karaf distribution per entry in CLUSTER_MEMBER_IPS, wires them
    into a single pekko cluster and starts every member, then waits for all
    of them to become ready.

    Args:
        modules (dict[str, str] | None): Modules to give a shard of their
            own, as name -> yang namespace.
        local_only_shards (tuple[str, ...]): Shards left unreplicated, each
            member keeping its own. Naming any is checked against the
            running cluster, since a suite reading a single node passes
            either way and would not notice the layout failing to apply.
        align_bgp (bool): Whether to move the BGP listener onto the member
            owning the BGP singleton, which suites driving a speaker need.
        align_topology (bool): Whether to move the ipv4 topology builder
            onto that member too, which suites reading a topology built
            from an unreplicated RIB need.

    Yields:
        None
    """
    try:
        cluster.setup_cluster(modules=modules, local_only_shards=local_only_shards)
        cluster.start_cluster(ODL_FEATRUES)
        cluster.wait_cluster_ready(timeout=580)
        if local_only_shards:
            cluster.verify_shards_are_local_only(local_only_shards)
        cluster.log_shared_port_owners()
        if align_bgp:
            cluster.align_bgp_listener()
        if align_topology:
            cluster.align_topology_builder()
        for member_ip in CLUSTER_MEMBER_IPS:
            infra.execute_karaf_command(f"log:set {KARAF_LOG_LEVEL}", host=member_ip)
        yield
    finally:
        infra.stop_all_karaf_instances()


@pytest.fixture(scope="session")
def odl_three_node_cluster():
    """Fixture for 3-node ODL cluster session setup.

    Every module shares the default shard, which is how the distribution
    ships. Suites needing a module in a shard of its own ask for a fixture
    that says so, such as odl_three_node_cluster_nonreplicated_rib.

    Args:
        None

    Returns:
        None
    """
    yield from _run_three_node_cluster()


@pytest.fixture(scope="session")
def odl_three_node_cluster_bgp(odl_three_node_cluster):
    """Fixture for a 3-node cluster a BGP speaker can actually connect to.

    All three members bind the shared BGP port and one wins at random, but
    peers are registered only by the member owning the BGP singleton, which
    is a separate race. A speaker is therefore refused unless one member
    happens to have won both, so the listener is moved onto the singleton
    owner before any test runs. See libraries/cluster.py for why the port
    cannot simply be given a per-member address.

    Kept apart from odl_three_node_cluster so suites that never touch BGP do
    not pay for a BGP-specific workaround, and so the plain fixture stays
    usable by the PCEP and BMP clustering suites, whose listeners are bound
    inside their singleton and so need no alignment.

    Args:
        None

    Returns:
        None
    """
    cluster.align_bgp_listener()
    yield


@pytest.fixture(scope="session")
def odl_three_node_cluster_nonreplicated_rib():
    """Fixture for a 3-node cluster whose BGP RIB is not replicated.

    Each member keeps its own bgp_rib shard instead of sharing one, which is
    what the nonreplicated_rib Robot suites set up before they run. Routes
    therefore reach only the member holding the session. The topology they
    are exported into is replicated as usual, so every member can still
    serve it; those suites read the topology shard leader because the Robot
    originals do, not because the others have nothing to show.

    bgp_rib and topology are given shards of their own here: a module cannot
    be left unreplicated while it shares the default shard, and the topology
    shard whose leader the suites look up does not otherwise exist.

    The BGP listener is aligned as in odl_three_node_cluster_bgp, since these
    suites drive a speaker too, and the ipv4 topology builder is moved onto
    that same member. The builder is a singleton elected separately from the
    RIB's, so it can land on a member whose own bgp_rib shard stays empty,
    and then no member has a topology to read.

    Args:
        None

    Returns:
        None
    """
    yield from _run_three_node_cluster(
        modules=cluster.NONREPLICATED_RIB_MODULES,
        local_only_shards=("bgp_rib",),
        align_bgp=True,
        align_topology=True,
    )


def _test_context_name(request: pytest.FixtureRequest) -> str:
    """Names the suite a test belongs to, for the karaf markers.

    Tests defined directly in a module have no class, so fall back to the
    module name rather than failing on a missing one.

    Args:
        request (FixtureRequest): Request fixture for accessing test context.

    Returns:
        str: Class name of the test, or its module name if it has no class.
    """
    return request.cls.__name__ if request.cls else request.module.__name__


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
    suite_name = _test_context_name(request)
    hosts = cluster.active_nodes()
    for host in hosts:
        infra.log_message_to_karaf(f"Starting suite {suite_name}", host=host)
    yield
    for host in hosts:
        infra.log_message_to_karaf(f"End of suite {suite_name}", host=host)


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
    test_name = f"{_test_context_name(request)}.{request.node.name}"
    hosts = cluster.active_nodes()
    for host in hosts:
        infra.log_message_to_karaf(f"Starting test {test_name}", host=host)
    yield
    for host in hosts:
        infra.log_message_to_karaf(f"End of test {test_name}", host=host)


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
