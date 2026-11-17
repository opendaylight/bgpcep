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
single host only one member wins each of ports 1790, 4189 and 12345. PCEP
and BMP bind theirs inside their singletons, so the winner is whichever
member the singleton election picked; only 1790 is bound independently of
its singleton and is therefore a genuine race between members starting
concurrently. Real CSIT does not hit this because it runs each member on
its own VM.

For BGP this has a further consequence. Peers are added to the peer
registry by BGPClusterSingletonService, which runs only on the owner of
the BGP singleton, so a speaker is accepted only when the member that won
port 1790 is also that owner -- and since the two are decided by unrelated
races, usually it is not. align_bgp_listener brings them together by
restarting the acceptor bundle on the owner alone, which is why BGP
cluster suites ask for the odl_three_node_cluster_bgp fixture rather than
the plain one. Any future listener with the same shape can be moved the
same way through restart_listener_on.

RESTCONF and the Karaf shell are unaffected -- those are configured per
member from plain files -- so verifying datastore state on every member
works as expected, which is what the clustering suites are for.
"""

import logging
import re
import time

from libraries import infra
from libraries import templated_requests
from libraries.variables import variables

CLUSTER_MEMBER_IPS = variables.CLUSTER_MEMBER_IPS
CLUSTER_MEMBER_MAX_MEM = variables.CLUSTER_MEMBER_MAX_MEM
ODL_IP = variables.ODL_IP

# Vendored copy of ODL's own cluster configuration script. It is staged into
# each member's bin/ before running, because the script locates the
# distribution relative to its own path.
CONFIGURE_CLUSTER_SCRIPT = "tools/configure_cluster.sh"
# Distribution the build stages, used until the cluster fixture
# moves it into per-member copies.
STANDALONE_DIR = "opendaylight"
# Ports the members share because their bind addresses come from the
# replicated configuration datastore. Logged after startup so a failing
# run shows which member won each race.
# PCEP and BMP create their server inside the singleton, so the member
# holding the port is the singleton owner. BGP's acceptor binds independently
# of the BGP singleton, so its holder need not own it -- both are reported.
SINGLETON_SHARED_PORTS = ((4189, "PCEP"), (12345, "BMP"))
BGP_SHARED_PORT = 1790
# Bundle whose blueprint builds BGPPeerAcceptorImpl. It exports a package
# nothing else in the tree imports and no other class constructs the impl,
# so it is a leaf and can be stopped on one member without disturbing
# anything else running there. That is what makes the port movable.
BGP_ACCEPTOR_BUNDLE = "org.opendaylight.bgpcep.bgp-peer-acceptor"
# Bundle whose components build the BGP topology exporters. Each member
# registers a candidate for every topology while it runs, so stopping it
# withdraws that member from the election and lets the exporter be put on
# a chosen member.
BGP_TOPOLOGY_BUNDLE = "org.opendaylight.bgpcep.bgp-topology-provider"
# Topology the prefix counting suites read, and the name of the singleton
# service group that builds it.
IPV4_TOPOLOGY = "example-ipv4-topology"

# Modules the non-replicated RIB setup gives a shard of their own, as
# name -> yang namespace. bgp_rib is the point of the exercise: a module
# only gets its own shard, and so can only be left unreplicated, once it is
# listed here. topology comes along because the suites read the leader of
# the topology shard, which does not otherwise exist -- the distribution
# ships every module commented out, so all data lands in the default shard.
NONREPLICATED_RIB_MODULES = {
    "bgp_rib": "urn:opendaylight:params:xml:ns:yang:bgp-rib",
    "topology": "urn:TBD:params:xml:ns:yang:network-topology",
}
# File ODL's configuration script reads the module list from. It ships in
# every distribution's bin/ with each module commented out.
CUSTOM_SHARD_CONFIG = "bin/custom_shard_config.txt"
MODULE_SHARDS_CONF = "configuration/initial/module-shards.conf"
# Jolokia MBean holding a shard's raft state on the member serving it.
SHARD_MBEAN_URI = (
    "jolokia/read/org.opendaylight.controller:Category=Shards,"
    "name=member-{index}-shard-{shard}-{datastore},type=Distributed{Type}Datastore"
)

log = logging.getLogger(__name__)


def _checked_shell(command: str, cwd: str | None = None) -> str:
    """Runs a setup command and fails immediately if it did not succeed.

    Cluster setup is a chain where a silent failure is expensive: a skipped
    copy or a script that exits non-zero leaves members unconfigured, and
    the first symptom is a "System ready" timeout minutes later, or members
    that never join each other. infra.shell reports the return code rather
    than raising, so every step here is checked.

    Args:
        command (str): Shell command to run.
        cwd (str | None): Directory to run it in.

    Returns:
        str: Standard output of the command.
    """
    return_code, output = infra.shell(command, cwd=cwd)
    assert return_code == 0, (
        f"Cluster setup step failed with return code {return_code}: "
        f"{command} (cwd={cwd or '.'})\n{output}"
    )
    return output

# Whether the current pytest session is running the cluster topology.
# Set once via set_is_cluster_run(), from conftest's cluster setup fixture.
_is_cluster_run = False

# Host settings that would otherwise put every member on the same address:
# the first four default to 0.0.0.0, the management pair to 127.0.0.1.
# Value is the key as it appears in the file, whether currently active or
# commented out. Unlike the BGP, PCEP and BMP listeners, these are plain
# per-member files and so can differ between members.
_MEMBER_BIND_ADDRESS_SETTINGS = (
    ("etc/org.apache.karaf.shell.cfg", "sshHost"),
    ("etc/org.opendaylight.restconf.nb.rfc8040.cfg", "bind-address"),
    ("etc/system.properties", "jetty.host"),
    ("etc/org.ops4j.pax.web.cfg", "org.ops4j.pax.web.listening.addresses"),
    ("etc/org.apache.karaf.management.cfg", "rmiRegistryHost"),
    ("etc/org.apache.karaf.management.cfg", "rmiServerHost"),
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


def active_karaf_log_dirs() -> list[str]:
    """Distribution directories whose karaf log belongs to this session.

    The cluster fixture moves the single-node distribution into per-member
    directories, so anything reading a karaf log has to follow.

    Args:
        None

    Returns:
        list[str]: Every member directory for a cluster session, otherwise
            the single standalone distribution.
    """
    return get_cluster_dirs() if is_cluster_run() else [STANDALONE_DIR]


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

    Every member gets its own copy of the `opendaylight` directory the build
    produced, which is left in place so a later standalone run still finds
    it. Any member directory left over from a previous run is removed first,
    and `data/` is dropped from each copy -- it may carry a karaf.log from an
    earlier run whose "System ready" would satisfy wait_cluster_ready
    immediately.

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
    for target_dir in dirs:
        _checked_shell(f"rm -rf {target_dir}")
        _checked_shell(f"cp -r opendaylight {target_dir}")
        _checked_shell(f"rm -rf {target_dir}/data")
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
        _checked_shell(
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
    _checked_shell(f"cp ../{CONFIGURE_CLUSTER_SCRIPT} bin/", cwd=cwd)
    _checked_shell(
        f"./bin/configure_cluster.sh {index} {','.join(member_ips)}", cwd=cwd
    )


def setup_cluster(
    modules: dict[str, str] | None = None,
    local_only_shards: tuple[str, ...] = (),
):
    """Configures every cluster member, without starting them.

    Copies one Karaf distribution per entry in CLUSTER_MEMBER_IPS, rebinds
    each member's endpoints to its own address, and wires every member into
    the pekko cluster.

    Args:
        modules (dict[str, str] | None): Modules to give a shard of their
            own, as name -> yang namespace. Without this every module shares
            the default shard, which is the stock layout.
        local_only_shards (tuple[str, ...]): Shards each member should keep
            to itself, leaving them unreplicated across the cluster. Naming
            a shard here requires its module in `modules`.

    Returns:
        None
    """
    cluster_dirs = _prepare_member_directories()
    for index, (cwd, member_ip) in enumerate(
        zip(cluster_dirs, CLUSTER_MEMBER_IPS), start=1
    ):
        _configure_member_network(cwd, member_ip)
        if modules:
            _write_custom_shard_config(cwd, modules)
        _configure_member_cluster(cwd, index, CLUSTER_MEMBER_IPS)
        for shard in local_only_shards:
            _restrict_shard_to_member(cwd, shard, f"member-{index}")
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
        infra.start_odl_with_features(features, cwd=cwd, max_mem=CLUSTER_MEMBER_MAX_MEM)


def wait_cluster_ready(timeout: int = 600):
    """Blocks until every member logs "System ready".

    Args:
        timeout (int): Seconds to wait per member before failing.

    Returns:
        None
    """
    for cwd in get_cluster_dirs():
        infra.wait_for_odl_ready(cwd, timeout=timeout)


def log_shared_port_owners():
    """Logs which member currently holds each shared protocol port.

    The members race for these ports, so recording the winner makes an
    otherwise puzzling connection failure readable after the fact. For BGP
    the singleton owner is logged too, since a speaker is accepted only when
    one member is both.

    Args:
        None

    Returns:
        dict[int, str]: Member directory holding each port, where known.
    """
    return_code, _ = infra.shell("command -v ss")
    if return_code != 0:
        log.warning("ss is not available, cannot determine port owners")
        return {}

    owners = {}
    for port, protocol in ((BGP_SHARED_PORT, "BGP"), *SINGLETON_SHARED_PORTS):
        # ss prints the listening socket's pid; /proc gives the member
        # directory it was started from.
        _, pids = infra.shell(
            f"ss -ltnp 'sport = :{port}' 2>/dev/null | "
            "grep -oP 'pid=\\K[0-9]+' | head -1"
        )
        pid = (pids or "").strip()
        if not pid:
            log.warning(f"{protocol} port {port} is not held by any member")
            continue
        _, cwd = infra.shell(f"readlink /proc/{pid}/cwd || true")
        member = (cwd or "").strip().rstrip("/").split("/")[-1] or "unknown"
        owners[port] = member
        log.info(f"{protocol} port {port} held by {member} (pid {pid})")

    singleton_owner = bgp_singleton_owner()
    if singleton_owner is None:
        log.warning("no running member owns the BGP singleton")
    else:
        log.info(f"BGP singleton instantiated on {singleton_owner}")
        port_owner = owners.get(BGP_SHARED_PORT)
        if port_owner is not None and port_owner != singleton_owner:
            log.warning(
                f"BGP port {BGP_SHARED_PORT} is held by {port_owner} but the "
                f"singleton runs on {singleton_owner}; speakers are refused"
            )
    return owners


def is_member_running(member_dir: str) -> bool:
    """Whether the member in `member_dir` still answers on its Karaf shell.

    Lets callers skip a member a suite has deliberately killed, instead of
    sending it a console command and waiting out the connection timeout.

    Args:
        member_dir (str): Member distribution directory.

    Returns:
        bool: True if the member's Karaf SSH port is listening.
    """
    member_ip = member_ip_for_dir(member_dir)
    _, listening = infra.shell(
        f"ss -ltn 'src {member_ip}:{infra.KARAF_SSH_PORT}' | grep -c LISTEN"
    )
    return (listening or "").strip() not in ("", "0")


def running_members() -> list[str]:
    """Member directories whose Karaf is currently up.

    Args:
        None

    Returns:
        list[str]: Member directories still answering on their Karaf shell,
            in member order.
    """
    return [cwd for cwd in get_cluster_dirs() if is_member_running(cwd)]


def _singleton_owner(pattern: str) -> str | None:
    """Member whose log last reported taking the singleton `pattern` matches.

    A member logs a singleton being instantiated when it takes ownership
    and logs it closing when it gives it up, so ownership is decided by
    whichever of the two came last rather than by the presence of the first.
    A member that has already handed ownership over would otherwise still
    look like the owner for the rest of the run. Members that are no longer
    running are skipped for the same reason: a killed owner never gets to
    log the close, so its log keeps claiming ownership forever.

    Args:
        pattern (str): Expression matching both log lines of the singleton,
            the one taking ownership and the one giving it up.

    Returns:
        str | None: Member directory, or None if no running member owns it.
    """
    for cwd in get_cluster_dirs():
        if not is_member_running(cwd):
            continue
        _, last_event = infra.shell(
            f"grep -E '{pattern}' {cwd}/data/log/karaf.log 2>/dev/null "
            "| tail -1 || true"
        )
        if (last_event or "").strip().endswith("instantiated"):
            return cwd
    return None


def bgp_singleton_owner() -> str | None:
    """Member currently running the BGP cluster singleton.

    Args:
        None

    Returns:
        str | None: Member directory, or None if no running member owns it.
    """
    return _singleton_owner(
        "BGPClusterSingletonService .* (instantiated|close service instance)"
    )


def topology_singleton_owner(topology: str = IPV4_TOPOLOGY) -> str | None:
    """Member currently building `topology`.

    Args:
        topology (str): Name of the topology, which is also the name of its
            singleton service group.

    Returns:
        str | None: Member directory, or None if no running member owns it.
    """
    return _singleton_owner(
        f"Topology Singleton Service ServiceGroupIdentifier\\[value={topology}\\]"
    )


def bgp_singleton_owner_host() -> str:
    """Address of the member running the BGP cluster singleton.

    The singleton owner is the only member with a session, and so the only
    one whose RIB holds the peer when the RIB is not replicated. Fails
    rather than returning nothing, since a caller asking for it has no
    sensible fallback.

    Args:
        None

    Returns:
        str: Address of the member owning the BGP singleton.
    """
    owner_dir = bgp_singleton_owner()
    assert owner_dir is not None, (
        "No running member owns the BGP singleton, so none of them holds a "
        "session. Check that odl-bgpcep-bgp came up on every member."
    )
    return member_ip_for_dir(owner_dir)


def _port_owner(port: int) -> tuple[str, str] | None:
    """Member directory and pid currently listening on `port`, if any.

    Args:
        port (int): TCP port to look up.

    Returns:
        tuple[str, str] | None: (member directory, pid), or None if free.
    """
    _, pids = infra.shell(
        f"ss -ltnp 'sport = :{port}' 2>/dev/null | grep -oP 'pid=\\K[0-9]+' | head -1"
    )
    pid = (pids or "").strip()
    if not pid:
        return None
    _, cwd = infra.shell(f"readlink /proc/{pid}/cwd || true")
    return (cwd or "").strip().rstrip("/").split("/")[-1] or "unknown", pid


def member_ip_for_dir(member_dir: str) -> str:
    """Loopback address of the member running from `member_dir`.

    Args:
        member_dir (str): Member distribution directory.

    Returns:
        str: The member's configured IP.
    """
    return CLUSTER_MEMBER_IPS[get_cluster_dirs().index(member_dir)]


def restart_listener_on(
    bundle: str, port: int, target_dir: str, timeout: int = 60
) -> str | None:
    """Leaves exactly one member listening on a shared protocol port.

    A listener whose bind address comes from the replicated configuration
    datastore cannot be given a per-member address (see the module
    docstring), so on a single host every member tries to bind the same
    wildcard address and the winner is arbitrary. Stopping the bundle that
    builds the listener frees the port on every member, and starting it
    again on one member leaves that member as the only candidate; the
    blueprint container is rebuilt on start, so the listener is constructed
    afresh.

    Members that are not running are skipped, so this still works after a
    suite has killed one.

    Args:
        bundle (str): Symbolic name of the bundle that builds the listener.
        port (int): Port the listener binds, used to confirm the move.
        target_dir (str): Member distribution directory to end up with it.
        timeout (int): Seconds to wait for the port to be freed and rebound.

    Returns:
        str | None: Member directory now holding the port, or None if
            nothing bound it within the timeout.
    """
    live_members = running_members()
    assert target_dir in live_members, (
        f"cannot move {bundle} onto {target_dir}, which is not running"
    )
    target_ip = member_ip_for_dir(target_dir)

    for cwd in live_members:
        member_ip = member_ip_for_dir(cwd)
        log.info(f"Stopping {bundle} on {cwd} ({member_ip})")
        infra.execute_karaf_command(f"bundle:stop {bundle}", host=member_ip)

    for _ in range(timeout):
        if _port_owner(port) is None:
            break
        time.sleep(1)
    else:
        log.warning(f"port {port} was not released within {timeout}s")

    log.info(f"Starting {bundle} on {target_dir} ({target_ip}) only")
    infra.execute_karaf_command(f"bundle:start {bundle}", host=target_ip)

    for _ in range(timeout):
        holder = _port_owner(port)
        if holder is not None:
            log.info(f"port {port} is now held by {holder[0]} (pid {holder[1]})")
            return holder[0]
        time.sleep(1)

    log.warning(f"nothing is listening on {port} after {timeout}s")
    return None


def align_bgp_listener(timeout: int = 60) -> str:
    """Moves the BGP listener onto the member that owns the BGP singleton.

    Peers are registered only by BGPClusterSingletonService, which runs on
    one member, so a speaker reaching any other member's acceptor is
    refused. The member that won port 1790 is unrelated to the member that
    won the singleton, so the two have to be brought together before any
    suite can drive a speaker against the cluster.

    Fails loudly rather than returning quietly, because every BGP cluster
    suite depends on this: reporting it here is the one chance to name the
    real cause, instead of letting each suite time out later on a topology
    that never fills.

    Args:
        timeout (int): Seconds to wait for the port to be freed and rebound.

    Returns:
        str: Member directory now holding the BGP port.
    """
    owner_dir = bgp_singleton_owner()
    assert owner_dir is not None, (
        "No running member owns the BGP singleton, so none of them can accept "
        "a BGP speaker. Check that odl-bgpcep-bgp came up on every member."
    )
    holder = restart_listener_on(
        BGP_ACCEPTOR_BUNDLE, BGP_SHARED_PORT, owner_dir, timeout=timeout
    )
    assert holder == owner_dir, (
        f"Could not move the BGP listener onto {owner_dir}, which owns the BGP "
        f"singleton: port {BGP_SHARED_PORT} is held by {holder}. Restarting "
        f"bundle {BGP_ACCEPTOR_BUNDLE} is what moves the listener, so check "
        f"that the bundle still carries that symbolic name and still builds "
        f"BGPPeerAcceptorImpl from its blueprint."
    )
    return holder


def align_topology_builder(
    topology: str = IPV4_TOPOLOGY, timeout: int = 60
) -> str:
    """Moves a BGP topology builder onto the member that owns the RIB.

    The builder exports a topology by listening to the loc-rib of the BGP
    RIB in the operational datastore, and it is a cluster singleton of its
    own, elected separately from the RIB's. While the RIB is replicated the
    two can sit on different members and nothing shows it, because the
    builder reads the same replicated data wherever it runs. Once each
    member keeps its own RIB, a builder anywhere but on the RIB owner reads
    an empty shard and exports nothing at all, on any member, which looks
    exactly like a speaker that never connected.

    Stopping the bundle withdraws the member from the election, so stopping
    it everywhere and starting it on the RIB owner leaves that member as
    the only candidate. The other members keep serving the topology from
    the datastore, which is all the suites read them for.

    Args:
        topology (str): Topology whose builder should be moved.
        timeout (int): Seconds to wait for the singleton to change hands.

    Returns:
        str: Member directory now building the topology.
    """
    owner_dir = bgp_singleton_owner()
    assert owner_dir is not None, (
        "No running member owns the BGP singleton, so there is no RIB for a "
        "topology builder to follow. Check that odl-bgpcep-bgp came up on "
        "every member."
    )

    for cwd in running_members():
        member_ip = member_ip_for_dir(cwd)
        log.info(f"Stopping {BGP_TOPOLOGY_BUNDLE} on {cwd} ({member_ip})")
        infra.execute_karaf_command(
            f"bundle:stop {BGP_TOPOLOGY_BUNDLE}", host=member_ip
        )

    for _ in range(timeout):
        if topology_singleton_owner(topology) is None:
            break
        time.sleep(1)
    else:
        log.warning(f"{topology} was still owned after {timeout}s")

    target_ip = member_ip_for_dir(owner_dir)
    log.info(f"Starting {BGP_TOPOLOGY_BUNDLE} on {owner_dir} ({target_ip}) only")
    infra.execute_karaf_command(
        f"bundle:start {BGP_TOPOLOGY_BUNDLE}", host=target_ip
    )

    holder = None
    for _ in range(timeout):
        holder = topology_singleton_owner(topology)
        if holder is not None:
            break
        time.sleep(1)

    assert holder == owner_dir, (
        f"Could not move the {topology} builder onto {owner_dir}, which owns "
        f"the BGP RIB: it is built by {holder}. Stopping bundle "
        f"{BGP_TOPOLOGY_BUNDLE} is what withdraws a member from the election, "
        f"so check that the bundle still carries that symbolic name and still "
        f"registers the topology singleton."
    )
    log.info(f"{topology} is now built by {holder}")
    return holder


def _write_custom_shard_config(cwd: str, modules: dict[str, str]):
    """Lists the modules that should each get a shard of their own.

    ODL's configuration script rebuilds modules.conf and module-shards.conf
    from this file whenever it exists, so it has to be written before the
    script runs. The distribution ships it with every module commented out,
    which is why a stock cluster keeps all data in the default shard.

    Args:
        cwd (str): Member's distribution directory.
        modules (dict[str, str]): Module name -> yang namespace.

    Returns:
        None
    """
    lines = []
    for position, (name, namespace) in enumerate(modules.items(), start=1):
        lines.append(f"FRIENDLY_MODULE_NAMES[{position}]='{name}'")
        lines.append(f"MODULE_NAMESPACES[{position}]='{namespace}'")
    body = "\n".join(lines)
    _checked_shell(f"cat > {CUSTOM_SHARD_CONFIG} <<'EOF'\n{body}\nEOF", cwd=cwd)


def _restrict_shard_to_member(cwd: str, shard: str, member: str):
    """Leaves `shard` with a single replica, on `member`.

    The configuration script widens every replica list to the whole
    membership as its last step, so narrowing one again has to happen after
    it has run. A shard listed on one member only is served by that member
    alone, which is what makes the RIB unreplicated.

    Args:
        cwd (str): Member's distribution directory.
        shard (str): Shard name, as it appears in module-shards.conf.
        member (str): Pekko member name to leave the shard on, e.g. member-2.

    Returns:
        None
    """
    path = f"{cwd}/{MODULE_SHARDS_CONF}"
    with open(path) as config_file:
        content = config_file.read()
    pattern = re.compile(
        rf'(name\s*=\s*"{re.escape(shard)}"\s*\n\s*replicas\s*=\s*\[)[^\]]*(\])'
    )
    content, substitutions = pattern.subn(rf'\g<1>"{member}"\g<2>', content)
    assert substitutions == 1, (
        f"expected exactly one replica list for shard {shard} in {path}, "
        f"found {substitutions}; the file ODL's configuration script writes "
        f"must have changed shape"
    )
    with open(path, "w") as config_file:
        config_file.write(content)


def shard_leader(shard: str, datastore: str = "operational", timeout: int = 60) -> str:
    """Address of the member currently leading `shard`.

    Raft elects one leader per shard, and the suites that read a single node
    have to read that one: on an unreplicated RIB the other members hold no
    copy of the data at all. Members that do not serve the shard answer with
    an error rather than a state, and are skipped.

    Leadership takes a moment to settle after startup, so the lookup is
    retried rather than failing on the first look.

    Args:
        shard (str): Shard name, e.g. topology.
        datastore (str): Either operational or config.
        timeout (int): Seconds to keep looking for a leader.

    Returns:
        str: Address of the member whose raft state for the shard is Leader.
    """
    datastore_type = "Config" if datastore == "config" else "Operational"
    leaders: list[str] = []
    for _ in range(timeout):
        leaders = []
        for index, member_dir in enumerate(get_cluster_dirs(), start=1):
            if not is_member_running(member_dir):
                continue
            member_ip = member_ip_for_dir(member_dir)
            uri = SHARD_MBEAN_URI.format(
                index=index, shard=shard, datastore=datastore, Type=datastore_type
            )
            try:
                response = templated_requests.get_from_uri(uri, host=member_ip)
                state = response.json().get("value", {}).get("RaftState")
            except Exception:
                continue
            if state == "Leader":
                leaders.append(member_ip)
        if len(leaders) == 1:
            log.info(f"{shard} {datastore} shard is led by {leaders[0]}")
            return leaders[0]
        time.sleep(1)
    raise AssertionError(
        f"No single leader for the {shard} {datastore} shard within {timeout}s; "
        f"found {leaders}. Check that the shard exists -- a module only gets a "
        f"shard of its own when it is listed in {CUSTOM_SHARD_CONFIG}."
    )
