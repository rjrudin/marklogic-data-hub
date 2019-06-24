package com.marklogic.hub.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.marklogic.hub.cli.Installer;

import java.io.File;

public class InstallHandler implements RequestHandler<Request, Response> {

    @Override
    public Response handleRequest(Request request, Context context) {
        String tmpDir = System.getProperty("java.io.tmpdir");
        File dir = new File(tmpDir, "dhf-installer");
        
        String[] args = new String[]{
            "--path", dir.getAbsolutePath(),
            "--host", request.getHost(),
            "--username", request.getUsername(),
            "--password", request.getPassword(),
            request.getCommand()
        };

        long start = System.currentTimeMillis();
        Installer.main(args);
        return new Response("Deployment finished; time: " + (System.currentTimeMillis() - start) + "ms");
    }
}
