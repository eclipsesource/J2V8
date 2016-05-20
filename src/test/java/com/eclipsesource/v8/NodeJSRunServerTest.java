/*******************************************************************************
 * Copyright (c) 2016 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 *    Dukehoff GmbH - node.js server test
 ******************************************************************************/

package com.eclipsesource.v8;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;

public class NodeJSRunServerTest {
    private NodeJS node;

    @Before
    public void createNode() {
        node = NodeJS.createNodeJS();
    }

    @After
    public void destroyNode() {
        node.release();
    }

    @Test
    public void runServerAndConnetToIt() throws IOException, InterruptedException, ExecutionException {
        final int port = findEmptyPort();
        String code =
            "var http = require('http');\n" +
            "function handleRequest(request, response){\n" +
            "    response.end('Connected: ' + request.url);\n" +
            "}\n" +
            "var p = " + port + ";\n" +
            "var server = http.createServer(handleRequest);\n" +
            "server.listen(p, function(){\n" +
            "    console.log(\"Server listening on: http://localhost:%s\", p);\n" +
            "});\n" +
            "return server";

        File serverScript = createScriptFile(code);
        V8Object server = node.require(serverScript);
        assertNotNull(server);

        Future<String> done = Executors.newSingleThreadExecutor().submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                URL u = new URL("http://127.0.0.1:" + port + "/hello");
                final URLConnection conn = u.openConnection();

                InputStreamReader r = new InputStreamReader(conn.getInputStream());
                BufferedReader br = new BufferedReader(r);
                String line = br.readLine();
                return line;
            }
        });

        while (!done.isDone()) {
            process(node, done);
        }

        assertEquals("Connected: /hello", done.get());

        server.release();
        serverScript.delete();
    }

    private File createScriptFile(String code) throws IOException {
        File serverScript = File.createTempFile("temp", ".js");
        FileWriter w = new FileWriter(serverScript);
        w.write(code);
        w.close();
        serverScript.deleteOnExit();
        return serverScript;
    }

    private int findEmptyPort() throws IOException {
        final ServerSocket ss = new ServerSocket(0);
        final int port = ss.getLocalPort();
        ss.close();
        return port;
    }

    private void process(NodeJS node, Future<?> await) {
        while (node.isRunning()) {
            if (await != null && await.isDone()) {
                break;
            }
            node.handleMessage();
        }
    }
}
