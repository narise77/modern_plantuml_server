package com.github.narise77.modern_plantuml_server;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
    TestAsciiArt.class,
    TestAsciiCoder.class,
    TestCharset.class,
    TestCheck.class,
    TestEPS.class,
    TestImage.class,
    TestLanguage.class,
    TestMap.class,
    TestMultipageUml.class,
    TestOldProxy.class,
    TestProxy.class,
    TestSVG.class,
    TestWebUI.class
})
public class AllTests {}
