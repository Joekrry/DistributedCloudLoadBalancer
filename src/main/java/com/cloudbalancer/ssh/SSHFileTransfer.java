package com.cloudbalancer.ssh;

import com.jcraft.jsch.*;
import java.io.*;

public class SSHFileTransfer {

    private static final String USERNAME = "root";
    private static final String PASSWORD = "fileserver";
    private static final int PORT = 22;

    public static void uploadChunk(String host, byte[] data, String remotePath) throws Exception {
        Session session = createSession(host);
        session.connect();
        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect();

        String dir = remotePath.substring(0, remotePath.lastIndexOf('/'));
        try { sftp.mkdir(dir); } catch (SftpException ignored) {}

        sftp.put(new ByteArrayInputStream(data), remotePath);
        sftp.disconnect();
        session.disconnect();
    }

    public static byte[] downloadChunk(String host, String remotePath) throws Exception {
        Session session = createSession(host);
        session.connect();
        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect();

        byte[] data = sftp.get(remotePath).readAllBytes();
        sftp.disconnect();
        session.disconnect();
        return data;
    }

    public static String executeCommand(String host, String command) throws Exception {
        Session session = createSession(host);
        session.connect();
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        channel.setErrStream(System.err);
        InputStream in = channel.getInputStream();
        channel.connect();

        StringBuilder output = new StringBuilder();
        byte[] buffer = new byte[1024];
        while (true) {
            while (in.available() > 0) {
                int len = in.read(buffer, 0, 1024);
                if (len < 0) break;
                output.append(new String(buffer, 0, len));
            }
            if (channel.isClosed()) break;
            Thread.sleep(100);
        }

        channel.disconnect();
        session.disconnect();
        return output.toString();
    }

    private static Session createSession(String host) throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(USERNAME, host, PORT);
        session.setPassword(PASSWORD);
        session.setConfig("StrictHostKeyChecking", "no");
        return session;
    }
}
