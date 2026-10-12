package com.github.narise77.modern_plantuml_server.server;


public interface ServerUtils {

    public void startServer() throws Exception;

    public void stopServer() throws Exception;

    public abstract String getServerUrl();

}
