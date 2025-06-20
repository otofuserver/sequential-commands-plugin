package com.plugin.sequentialcommands;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/*
Created by Jake Cohen on 3/20/2022
This Rundeck plugin is used to send sequential commands via SSH.
 */
public class SSHConnect {

    private final String userName;
    private final String sshKeyPass;
    private final String hostName;
    private final String strictHostKey;
    private final boolean usePrivKey;

    public SSHConnect(String userName, String sshKeyPass, String hostName, boolean usePrivKey, String strictHostKey) {

        this.userName = userName;
        this.sshKeyPass = sshKeyPass;
        this.hostName = hostName;
        this.usePrivKey = usePrivKey;
        this.strictHostKey = strictHostKey;

    }

    public Session connect() throws JSchException {

        JSch jsch = new JSch();
        Session session = jsch.getSession(userName, hostName, 22);

        if ("true".equalsIgnoreCase(strictHostKey)) {
            session.setConfig("strictHostKeyChecking", "yes");
        } else {
            session.setConfig("StrictHostKeyChecking", "no");
        }

        if(usePrivKey) {
            byte[] privKey = sshKeyPass.getBytes(StandardCharsets.US_ASCII);
            jsch.addIdentity("privKey",privKey, null, null);
        } else {
            session.setPassword(sshKeyPass);
        }
        session.connect();

        // 接続直後の機器応答待ち（例：ルーターのログインバナーなど）
        try {
            Thread.sleep(300); // 0.3秒待機
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JSchException("Interrupted while waiting after SSH connect", e);
        }

        return session;
    }

    public static void printResult(InputStream input, Channel channel, String errorKeywords) throws Exception {
        byte[] buffer = new byte[1024];
        long lastRead = System.currentTimeMillis();
        final long timeoutMillis = 2000;

        StringBuilder outputBuilder = new StringBuilder();

        while (true) {
            if (input.available() > 0) {
                int read = input.read(buffer);
                if (read > 0) {
                    String chunk = new String(buffer, 0, read);
                    System.out.print(chunk);
                    outputBuilder.append(chunk);
                    lastRead = System.currentTimeMillis();
                }
            } else {
                Thread.sleep(100);
            }

            if (System.currentTimeMillis() - lastRead > timeoutMillis) {
                break;
            }
        }

        if (channel.isConnected()) {
            channel.disconnect();
        }

        // エラーワードチェック（大文字小文字無視、空白やnullなら無視）
        if (errorKeywords != null && !errorKeywords.trim().isEmpty()) {
            String resultOutput = outputBuilder.toString().toLowerCase();  // 出力を小文字に変換

            String[] keywords = errorKeywords.split(",");
            for (String keyword : keywords) {
                String trimmed = keyword.trim().toLowerCase(); // キーワードも小文字に変換
                if (!trimmed.isEmpty() && resultOutput.contains(trimmed)) {
                    throw new RuntimeException("SSH command failed. Detected error output matching keyword (case-insensitive): " + keyword.trim());
                }
            }
        }
    }


}
