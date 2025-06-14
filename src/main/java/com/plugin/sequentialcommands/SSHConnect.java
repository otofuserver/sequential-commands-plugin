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

    private String userName;
    private String sshKeyPass;
    private String hostName;
    private String strictHostKey;
    boolean usePrivKey;

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

        if (strictHostKey.equals("true")) {
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

    public static void printResult(InputStream input, Channel channel) throws Exception {
        byte[] buffer = new byte[1024];
        long lastRead = System.currentTimeMillis();
        final long timeoutMillis = 2000; // 2秒無応答で終了

        while (true) {
            if (input.available() > 0) {
                int read = input.read(buffer);
                if (read > 0) {
                    System.out.print(new String(buffer, 0, read));
                    lastRead = System.currentTimeMillis(); // 最後に受信した時間を更新
                }
            } else {
                Thread.sleep(100); // 少しだけ待つ
            }

            // 一定時間応答がないなら終了（プロンプトで止まるのを防ぐ）
            if (System.currentTimeMillis() - lastRead > timeoutMillis) {
                break;
            }
        }

        if (channel.isConnected()) {
            channel.disconnect();
        }
    }


}
