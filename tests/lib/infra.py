#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

from contextlib import contextmanager
import paramiko
import signal
import subprocess
import logging

from lib.RemoteSSHSessionHandler import RemoteSSHSessionHandler
from lib import utils

log = logging.getLogger(__name__)


def shell(
    command: str | list | tuple,
    joiner="; ",
    cwd: str | None = None,
    use_shell=True,
    run_in_background: bool = False,
    timeout: int = None,
    check_rc=False,
):
    """Runs single or multiple shell commands.

    Multiple shell command are concatenated together by using joiner.
    It provides mutliple options on how to run the command.

    Args:
        command (str | list | tuple): Shell command(s) to be run.
        joiner (str): Joiner for concatenating multiple commands.
        cwd (str): Current working directory from where the command
            needs to be executed.
        run_in_backgroud (bool): If the command should be started as background
            process without tty.
        timeotu (int): Timeout in seconds for the foreground command.

    Returns:
        tuple[int, str] | subprocess.Popen :
            For foreground process it returns final return code and stdout,
            for backgroud process it returns process handler.
    """
    exec_command = command
    if isinstance(command, (list, tuple)):
        exec_command = joiner.join(command)

    try:
        log.info(exec_command)
        if run_in_background:
            if use_shell:
                process = subprocess.Popen(
                    f"exec {exec_command}",
                    shell=True,
                    text=True,
                    cwd=cwd,
                )
            else:
                process = subprocess.Popen(
                    exec_command.split(" "), shell=False, text=True, cwd=cwd
                )
            return process
        else:
            result = subprocess.run(
                exec_command,
                shell=shell,
                check=True,
                capture_output=True,
                text=True,
                timeout=timeout,
                cwd=cwd,
            )
            log.debug(f"{result.returncode:3d} |--| {result.stdout}")
            if check_rc and result.returncode != 0:
                return AssertionError(f"Expected command {exec_command} to pass")
            return result.returncode, result.stdout
    except subprocess.CalledProcessError as e:
        std_error = e.stderr.strip()
        log.error(
            f"ERROR while command execution '{exec_command}'"\
            f"{':\n' + std_error if std_error else ''}"
        )
        return e.returncode, e.stdout
    except FileNotFoundError:
        log.error(f"ERROR command not found: {exec_command}")
        return None, None


def ssh_run_command(command, host, username, password, port=22, timeout=900):
    log.info(command)
    with open_ssh_connection(host, port, username, password) as ssh_connection:
        stdin, stdout, stderr = ssh_connection.exec_command(
            command, get_pty=True, timeout=timeout
        )
        stdout = stdout.read().decode()
        stderr = stderr.read().decode()

    log.info(f"{stdout=}")
    if stderr:
        log.warn(f"{stderr=}")

    return stdout, stderr


def ssh_start_command(command, host, username, password, port=22):
    session_handler = RemoteSSHSessionHandler(host, username, password, 22)
    session_handler.start_command(command)

    return session_handler


def ssh_stop_command(session_handler):
    session_handler.stop_command()


def ssh_put_file(local_file_path, remot_file_path, host, username, password, port=22):
    with open_ssh_connection(host, port, username, password) as ssh_connection:
        sftp_client = ssh_connection.open_sftp()
        sftp_client.put(local_file_path, remot_file_path)
        sftp_client.close()


def retry_shell_command(retry_count: int, interval: int, *args, **kwargs):
    """Repeatedly runs a shell command until the return code is 0.

    Args:
        retry_count (int): Maximum number of retries
        interval (int): Number of seconds to wait until next retry.
        *args: shell positional argments
        **kwargs: shell keyword arguments

    Returns:
        tuple[int, str]: Return code and standart output of successful run
            of the command.
    """
    validator = lambda result: result[0] == 0
    rc, output = utils.wait_until_function_returns_value_with_custom_value_validator(
        retry_count, interval, validator, shell, *args, **kwargs
    )
    return rc, output


def wait_for_string_in_file(
    retry_count: int, interval: int, string: str, file_name: str, threshold: int = 1
) -> int:
    """Repeatedly reads text file until it finds specific substring.

    Args:
        retry_count (int): Maximum number of retries
        interval (int): Number of seconds to wait until next retry.
        string (str): Subsring expected to be present in text file.
        file_name (str): Name of the text file to be checked for
            presence of substring.
        threshold (int): Minimum number of occurences of the expected
            substring.

    Returns:
        int: number of substring occurences in text file
    """
    validator = lambda result: result[0] == 0 and int(result[1].strip()) >= threshold
    rc, output = utils.wait_until_function_returns_value_with_custom_value_validator(
        retry_count, interval, validator, shell, f"grep -c '{string}' '{file_name}'"
    )
    return int(output.strip())


def start_odl_with_features(features: tuple[str], timeout: int = 60):
    """Starts ODL with installed provided features.

    Args:
        features (tuple[str]): Features to be installed in ODL.
        timeout (int): Timeout within which it needs to start ODL, otherwise fail.

    Returns:
        None
    """
    # set config with the required features
    shell(
        f"sed -ie 's/\(featuresBoot=\|featuresBoot =\)/featuresBoot = " \
        f"{",".join(features)},/g' etc/org.apache.karaf.features.cfg",
        cwd="opendaylight",
    )

    shell(
        "sed -ie 's/memory-mapped = true/memory-mapped = false/g' " \
        "system/org/opendaylight/controller/sal-clustering-config/*/sal-clustering-config-*-factorypekkoconf.xml",
        cwd="opendaylight",
    )

    # start ODL
    shell("JAVA_OPTS=-Xmx8g ./bin/start", cwd="opendaylight")

    # wait for proper message with timeout
    interval = 5
    retry_shell_command(
        timeout // interval,
        interval,
        "grep 'org.opendaylight.infrautils.*System ready' data/log/karaf.log",
        cwd="opendaylight",
    )


@contextmanager
def open_ssh_connection(
    hostname: str, port: int, username: str, password: str
) -> paramiko.SSHClient:
    """Creates ssh connection to remote host

    It also automatically closes this connection using context manager.

    Args:
        hostname (str): Target server hostname or ip address.
        port (int): Port used for ssh conenction.
        username (str): username used to log in to the ssh server
        password (str): password used to log in to the ssh server

    Returns:
        paramiko.SSHClient: ssh client connected to the remote ssh server.
    """
    ssh_client = paramiko.SSHClient()
    ssh_client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh_client.connect(
        hostname=hostname,
        port=port,
        username=username,
        password=password,
        look_for_keys=False,
        allow_agent=False,
    )
    yield ssh_client
    ssh_client.close()


def execute_karaf_command(command: str) -> tuple[str, str]:
    """Executed specific command using ODL karaf CLI console

    It usses ssh connection to connect to karaf CLI.

    Args:
        command (str): Command to be executed.

    Returns:
        tuple[str, str]: Stdout from karaf CLI, stderr from karaf CLI.
    """
    log.info(f"Executing command '{command}' on karaf console.")
    with open_ssh_connection("127.0.0.1", 8101, "karaf", "karaf") as karaf_connection:
        stdin, stdout, stderr = karaf_connection.exec_command(command)
        stdout = stdout.read().decode()
        stderr = stderr.read().decode()

    log.info(f"{stdout=}")
    if stderr:
        log.warn(f"Karaf command {command} failed with {stderr=}")

    return stdout, stderr


def log_message_to_karaf(message: str):
    """Log specific mesage to ODL karaf

    It usses ssh connection to connect to karaf CLI.

    Args:
        message (str): Message to be logged.

    Returns:
        None
    """
    execute_karaf_command(f"log:log 'ROBOT MESSAGE: {message}'")


def is_process_still_running(process: subprocess.Popen):
    """Check if provided process did not finish yet.

    Args:
        process (subprocess.Popen): Process handler.

    Returns:
        None
    """
    return process.poll() is None


def stop_process(process: subprocess.Popen, gracefully: bool = True):
    """Stops process by sending signal a veirfies it is not running.

    Args:
        process (subprocess.Popen): Process handler.
        gracefully (bool): Determines which signal should be sent,
            for gracefully it sends SIGINT, otherwise SIGKILL

    Returns:
        None
    """
    log.info(f"Stopping process with PID {process.pid}")
    signal_to_be_sent = signal.SIGTERM if gracefully else signal.SIGKILL
    log.info(f"Sending signal {signal_to_be_sent} to process with PID {process.pid}")
    process.send_signal(signal_to_be_sent)
    # check if it is still running
    try:
        utils.wait_until_function_returns_value(
            5, 1, False, is_process_still_running, process
        )
    except AssertionError as e:
        raise AssertionError(
            f"Was not able to stop process with PID {process.pid}, it is still running."
        ) from e
