package com.eclipsesource.v8;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class DebugTunnel {

    private static final String LOCALHOST = "localhost";
    Socket                      host      = null, target = null;
    boolean                     started   = false;
    private int                 hostPort;
    private int                 targetPort;
    private ServerSocket        ss;

    public DebugTunnel(final int hostPort, final int targetPort) {
        this.hostPort = hostPort;
        this.targetPort = targetPort;
    }

    public synchronized void start() {
        createSocketTunnel();
    }

    public synchronized void stop() {
        close(ss);
        close(host);
        close(target);
    }

    private void close(final Closeable socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                // Do nothing
            }
        }
    }

    private void setupSockets() {
        try {
            ss = new ServerSocket();
            ss.setReuseAddress(true);
            ss.bind(new InetSocketAddress(hostPort));
            host = ss.accept();
            host.setKeepAlive(true);
            target = new Socket(LOCALHOST, targetPort);
            target.setKeepAlive(true);
        } catch (IOException e) {
            // Print the exception if we cannot setup the sockets
            e.printStackTrace();
        }
    }

    private void createSocketTunnel() {
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                InputStream inputStream;
                OutputStream outputStream;
                setupSockets();
                try {
                    createReverseTunnle(host, target);
                    inputStream = host.getInputStream();
                    outputStream = target.getOutputStream();
                    for (int i = 0; (i = inputStream.read()) != -1;) {
                        outputStream.write(i);
                    }
                } catch (IOException e) {
                    // Do nothing
                } finally {
                    stop();
                }
            }
        });
        t.start();
    }

    private void createReverseTunnle(final Socket host, final Socket target) {
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {
                    inputStream = target.getInputStream();
                    outputStream = host.getOutputStream();
                    for (int i = 0; (i = inputStream.read()) != -1;) {
                        outputStream.write(i);
                    }
                } catch (IOException e) {
                    // Do nothing
                } finally {
                    stop();
                }
            }

        });
        t.start();
    }

}
