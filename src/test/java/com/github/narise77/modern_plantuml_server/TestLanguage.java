package com.github.narise77.modern_plantuml_server;

import java.io.IOException;
import java.net.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.narise77.modern_plantuml_server.utils.WebappTestCase;


public class TestLanguage extends WebappTestCase {

    /**
     * Tests that the language for the current PlantUML server can be obtained through HTTP
     */
    @Test
    public void testRetrieveLanguage() throws IOException {
        final URL url = new URL(getServerUrl() + "/language");
        String languageText = getContentText(url);
        Assertions.assertTrue(languageText.indexOf("@startuml") > 0, "Language contains @startuml");
    }

}
