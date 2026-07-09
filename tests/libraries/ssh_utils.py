#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import logging
from contextlib import contextmanager

import paramiko
import controller_testlib.ssh_utils

from libraries.RemoteSSHSessionHandler import RemoteSSHSessionHandler

log = logging.getLogger(__name__)

# Re-exported so open_ssh_connection() below keeps working, but the origin is
# explicit here rather than hidden behind a bare-name import.
create_ssh_client = controller_testlib.ssh_utils.create_ssh_client


@contextmanager
def open_ssh_connection(
    hostname: str, port: int, username: str, password: str
) -> paramiko.SSHClient:
    """Creates ssh connection to remote host.

    It also automatically closes this connection using context manager.

    Args:
        hostname (str): Target server hostname or ip address.
        port (int): Port used for ssh conenction.
        username (str): username used to log in to the ssh server
        password (str): password used to log in to the ssh server

    Returns:
        paramiko.SSHClient: ssh client connected to the remote ssh server.
    """
    ssh_client = create_ssh_client(hostname, port, username, password)
    try:
        yield ssh_client
    finally:
        ssh_client.close()


def ssh_run_command(
    command: str,
    host: str,
    username: str,
    password: str,
    port: int = 22,
    timeout: int = 900,
):
    """Runs single command on remote host using SSH.

    Command needs to finish within specific time otherwise it would time out.

    Args:
        command (str): Shell command to be run.
        host (str): SSH server host name.
        username (str): Client username credentials.
        password (bool): Client password credentials.
        port (int): SSH server port number.
        timeout (int): Timeout in seconds for the command.

    Returns:
        tuple[str, str]: A tuple containing the standard output
            and standard error.
    """
    log.info(command)
    with open_ssh_connection(host, port, username, password) as ssh_connection:
        stdin, stdout, stderr = ssh_connection.exec_command(
            command, get_pty=True, timeout=timeout
        )
        stdout_text = stdout.read().decode()
        stderr_text = stderr.read().decode()

    log.info(f"{stdout_text=}")
    if stderr_text:
        log.warn(f"{stderr_text=}")

    return stdout_text, stderr_text


def ssh_start_command(
    command: str, host: str, username: str, password: str, port: int = 22
) -> RemoteSSHSessionHandler:
    """Opens an ssh session to remote host and start a command.

    Enters a command on an SSH session and returns
    a handler to this session.

    Args:
        command (str): Shell command to be run.
        host (str): SSH server host name.
        username (str): Client username credentials.
        password (bool): Client password credentials.
        port (int): SSH server port number.

    Returns:
        RemoteSSHSessionHandler: Remote session handler.
    """
    session_handler = RemoteSSHSessionHandler(host, username, password, port)
    session_handler.start_command(command)

    return session_handler


def ssh_stop_command(session_handler: RemoteSSHSessionHandler):
    """Stops command which is beeing executed through SSH session.

    Args:
        session_handler (RemoteSSHSessionHandler): Handler for remote SSH session.

    Returns:
        None
    """
    session_handler.stop_command()


def ssh_put_file(
    local_file_path: str,
    remot_file_path: str,
    host: str,
    username: str,
    password: str,
    port: int = 22,
):
    """Transfers a file from the local machine to the remote host via SFTP.

    Args:
        local_file_path (str): The full path to the file on the local machine.
        remot_file_path (str): The full destination path on the remote host.
        host (str): SSH server host name.
        username (str): Client username credentials.
        password (str): Client password credentials.
        port (int): SSH server port number.

    Returns:
        None
    """
    with open_ssh_connection(host, port, username, password) as ssh_connection:
        sftp_client = ssh_connection.open_sftp()
        sftp_client.put(local_file_path, remot_file_path)
        sftp_client.close()
