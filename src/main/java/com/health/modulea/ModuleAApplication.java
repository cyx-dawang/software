package com.health.modulea;

import com.health.modulea.controller.ModuleAHttpServer;

public class ModuleAApplication {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        ModuleAHttpServer server = new ModuleAHttpServer(port);
        server.start();
        System.out.println("Module A server started at http://localhost:" + port);
    }
}
