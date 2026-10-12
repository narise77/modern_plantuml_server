package com.github.narise77.modern_plantuml_server.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import com.github.narise77.modern_plantuml_server.PlantUmlApplication;

public class EmbeddedSpringServer implements ServerUtils {

    private static final String contextPath = "/plantuml";
    private ConfigurableApplicationContext context;

    public void startServer() throws Exception {
        SpringApplication app = new SpringApplication(PlantUmlApplication.class);
        context = app.run(
            "--server.port=0",
            "--server.servlet.context-path=" + contextPath,
            "--spring.main.banner-mode=off"
        );
    }

    public void stopServer() throws Exception {
        if (context != null) {
            context.close();
        }
    }

    public String getServerUrl() {
        int port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
        return "http://localhost:" + port + contextPath;
    }

}
