#
# Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
"""Brings up a multi-node ODL cluster on a single test host.

Each member gets its own copy of the built distribution, its own loopback
address and its own pekko remote transport, so all members can run side by
side without changing any of the single-node ports.

Known limitation -- BGP, PCEP and BMP listeners are NOT per-member. Their
bind addresses live in the CONFIGURATION datastore (the topology and BMP
config loaders merge their files into it, and the BGP peer acceptor is an
odl:clustered-app-config), and a cluster replicates that datastore, so all
members necessarily share one value. They all default to 0.0.0.0, so on a
single host only the member that starts first can bind ports 1790, 4189 and
12345. Real CSIT does not hit this because it runs each member on its own
VM. RESTCONF and the Karaf shell are unaffected -- those are configured per
member from plain files -- so verifying datastore state on every member
works as expected.
"""

import logging

from libraries import infra
from libraries.variables import variables

CLUSTER_MEMBER_IPS = variables.CLUSTER_MEMBER_IPS
ODL_IP = variables.ODL_IP

# Vendored copy of ODL's own cluster configuration script. It is staged into
# each member's bin/ before running, because the script locates the
# distribution relative to its own path.
CONFIGURE_CLUSTER_SCRIPT = "tools/configure_cluster.sh"

log = logging.getLogger(__name__)

# Whether the current pytest session is running the cluster topology.
# Set once via set_is_cluster_run(), from conftest's cluster setup fixture.
_is_cluster_run = False

# Config files whose bind address defaults to every interface (0.0.0.0),
# which only one member per host can hold. Value is the key as it appears in
# the file, whether currently active or commented out. Unlike the BGP, PCEP
# and BMP listeners, these are plain per-member files and so can differ
# between members.
_MEMBER_BIND_ADDRESS_SETTINGS = (
    ("etc/org.apache.karaf.shell.cfg", "sshHost"),
    ("etc/org.opendaylight.restconf.nb.rfc8040.cfg", "bind-address"),
    ("etc/system.properties", "jetty.host"),
    ("etc/org.ops4j.pax.web.cfg", "org.ops4j.pax.web.listening.addresses"),
)


def set_is_cluster_run(value: bool):
    """Records whether this pytest session is running the cluster topology.

    Args:
        value (bool): True if this session is running cluster-marked tests.

    Returns:
        None
    """
    global _is_cluster_run
    _is_cluster_run = value


def is_cluster_run() -> bool:
    """Whether this pytest session is running the cluster topology.

    Args:
        None

    Returns:
        bool: True for a cluster session, False for standalone.
    """
    return _is_cluster_run


def active_nodes() -> list[str]:
    """Every ODL node address in play for this pytest session.

    Generic building block for anything that needs to act on "all nodes
    currently under test". Single-node code paths still work unmodified,
    since standalone sessions get a one-element list.

    Args:
        None

    Returns:
        list[str]: CLUSTER_MEMBER_IPS for a cluster session, otherwise a
            single-element list containing just ODL_IP.
    """
    return CLUSTER_MEMBER_IPS if is_cluster_run() else [ODL_IP]


def get_member_dir(index: int) -> str:
    """Distribution directory for cluster member at `index`.

    Args:
        index (int): Zero-based member position.

    Returns:
        str: Path to the member's Karaf distribution, relative to cwd.
    """
    return f"opendaylight-member-{index + 1}"


def get_cluster_dirs() -> list[str]:
    """Returns distribution directories for every configured cluster member.

    Args:
        None

    Returns:
        list[str]: Path to each member's Karaf distribution, in member order.
    """
    return [get_member_dir(index) for index in range(len(CLUSTER_MEMBER_IPS))]


def _prepare_member_directories() -> list[str]:
    """Stages one fresh Karaf distribution copy per cluster member.

    Member 1 is staged from the `opendaylight` directory the build produced;
    every other member gets a clean copy of it. Any member directory left
    over from a previous run is removed first, so re-runs on the same
    workspace start clean -- the build only refreshes `opendaylight`, not the
    per-member copies.

    Args:
        None

    Returns:
        list[str]: The prepared member directories, in member order.
    """
    dirs = get_cluster_dirs()
    # Guard for the rm -rf calls below; every entry must be a plain,
    # non-empty, relative member directory name. The assert makes the
    # invariant explicit to prevent a recursive delete on an empty, absolute
    # or parent path in any future refactor.
    assert all(
        name and name.startswith("opendaylight-member-") and "/" not in name
        for name in dirs
    ), f"unexpected cluster member directory name in {dirs}"
    infra.shell(f"rm -rf {dirs[0]}")
    infra.shell(f"mv opendaylight {dirs[0]}")
    for target_dir in dirs[1:]:
        infra.shell(f"rm -rf {target_dir}")
        infra.copy_dir(dirs[0], target_dir)
    return dirs


def _configure_member_network(cwd: str, member_ip: str):
    """Rebinds a member's wildcard (0.0.0.0) endpoints to its own address.

    Karaf SSH, RESTCONF and the web container all default to binding every
    interface, which only one process per host can do. Pinning each member to
    its own loopback alias lets every member keep the single-node ports
    unchanged. Pekko's own remote transport address is not set here; ODL's
    cluster configuration script sets it alongside the seed nodes.

    Args:
        cwd (str): Member's distribution directory.
        member_ip (str): Address to bind this member's endpoints to.

    Returns:
        None
    """
    for file_path, key in _MEMBER_BIND_ADDRESS_SETTINGS:
        infra.shell(
            f"touch {file_path} && "
            f"sed -i -E 's/^#?{key}[ ]*=.*/{key} = {member_ip}/' {file_path} && "
            f"grep -q '^{key}[ ]*=' {file_path} || "
            f"echo '{key} = {member_ip}' >> {file_path}",
            cwd=cwd,
        )


def _configure_member_cluster(cwd: str, index: int, member_ips: list[str]):
    """Wires a member into the pekko cluster using ODL's own script.

    The script seeds configuration/initial from the shipped templates if it is
    absent, points this member's seed-nodes at every member, names its role
    and widens every shard's replica list to the full membership.

    It is copied into the member's bin/ first, because it derives the
    distribution directory from its own location.

    Args:
        cwd (str): Member's distribution directory.
        index (int): This member's 1-based position in member_ips.
        member_ips (list[str]): Address of every cluster member, in order.

    Returns:
        None
    """
    infra.shell(f"cp ../{CONFIGURE_CLUSTER_SCRIPT} bin/", cwd=cwd)
    infra.shell(
        f"./bin/configure_cluster.sh {index} {','.join(member_ips)}", cwd=cwd
    )


def setup_cluster():
    """Configures every cluster member, without starting them.

    Copies one Karaf distribution per entry in CLUSTER_MEMBER_IPS, rebinds
    each member's endpoints to its own address, and wires every member into
    the pekko cluster.

    Args:
        None

    Returns:
        None
    """
    cluster_dirs = _prepare_member_directories()
    for index, (cwd, member_ip) in enumerate(
        zip(cluster_dirs, CLUSTER_MEMBER_IPS), start=1
    ):
        _configure_member_network(cwd, member_ip)
        _configure_member_cluster(cwd, index, CLUSTER_MEMBER_IPS)
    set_is_cluster_run(True)


def start_cluster(features: list[str]):
    """Starts every ODL cluster member with the given set of features.

    All members are launched before any of them is awaited: each member's
    pekko actor system needs its peers reachable to finish joining the
    cluster and log "System ready", so starting members one at a time and
    waiting on each in turn would deadlock -- the first member would sit
    retrying its seed nodes forever, since nothing else has been started yet.

    Args:
        features (list[str]): Karaf features to boot on every member.

    Returns:
        None
    """
    for cwd in get_cluster_dirs():
        infra.start_odl_with_features(features, cwd=cwd)


def wait_cluster_ready(timeout: int = 600):
    """Blocks until every member logs "System ready".

    Args:
        timeout (int): Seconds to wait per member before failing.

    Returns:
        None
    """
    for cwd in get_cluster_dirs():
        infra.wait_for_odl_ready(cwd, timeout=timeout)
