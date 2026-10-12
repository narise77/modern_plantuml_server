package com.github.narise77.modern_plantuml_server.utility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TestSampleLoader {

    @Test
    public void testExtractTitle() {
        Assertions.assertEquals("シークエンス図", SampleLoader.extractTitle("@startuml\ntitle シークエンス図\nBob -> Alice\n@enduml"));
        Assertions.assertEquals("ガントチャート", SampleLoader.extractTitle("@startgantt\ntitle \"ガントチャート\"\n[Task] lasts 3 days\n@endgantt"));
        Assertions.assertEquals("データフロー図", SampleLoader.extractTitle("@startuml\ntitle: データフロー図\n[A] -> [B]\n@enduml"));
        Assertions.assertNull(SampleLoader.extractTitle("@startuml\nBob -> Alice\n@enduml"));
        Assertions.assertNull(SampleLoader.extractTitle(null));
    }

    @Test
    public void testExtractFirstComment() {
        Assertions.assertEquals("アクティビティ図", SampleLoader.extractFirstComment("@startuml\n'アクティビティ図\nstart\nstop\n@enduml"));
        Assertions.assertEquals("ガントチャート", SampleLoader.extractFirstComment("@startgantt\n'  ガントチャート  \n[Task] lasts 3 days\n@endgantt"));
        Assertions.assertEquals("ブロックコメント", SampleLoader.extractFirstComment("@startuml\n/' ブロックコメント '/\nBob -> Alice\n@enduml"));
        Assertions.assertNull(SampleLoader.extractFirstComment("@startuml\nBob -> Alice\n@enduml"));
        Assertions.assertNull(SampleLoader.extractFirstComment(null));
    }

    @Test
    public void testLoadSamplesFromStaticResources() {
        SampleLoader loader = new SampleLoader();
        loader.loadSamples();

        List<SampleLoader.SampleDiagram> samples = loader.getSamples();
        Assertions.assertFalse(samples.isEmpty(), "Samples should not be empty");
        Assertions.assertEquals(10, samples.size(), "Should load 10 sample diagrams from static/sample");

        Map<String, String> nameByFile = samples.stream()
            .collect(Collectors.toMap(SampleLoader.SampleDiagram::getFilename, SampleLoader.SampleDiagram::getName));

        Assertions.assertEquals("シークエンス図", nameByFile.get("01.sequence.planuml"));
        Assertions.assertEquals("ユーズケース図", nameByFile.get("02.usecase.plantuml"));
        Assertions.assertEquals("エンティティ図", nameByFile.get("03.entity.plantuml"));
        Assertions.assertEquals("データフロー図", nameByFile.get("04.dfd.plantuml"));
        Assertions.assertEquals("アクティビティ図", nameByFile.get("05.acitivity.plantuml"));
        Assertions.assertEquals("コンポネント図", nameByFile.get("06.component.plantuml"));
        Assertions.assertEquals("状態遷移図", nameByFile.get("07.status.plantuml"));
        Assertions.assertEquals("ワイヤフレーム図", nameByFile.get("08.wireframe.plantuml"));
        Assertions.assertEquals("ガントチャート", nameByFile.get("09.ganttchart.plantuml"));
        Assertions.assertEquals("マインドマップ", nameByFile.get("10.mindmap.plantuml"));

        for (SampleLoader.SampleDiagram sample : samples) {
            Assertions.assertNotNull(sample.getCode());
            Assertions.assertFalse(sample.getCode().trim().isEmpty());
            Assertions.assertTrue(sample.getCode().contains(sample.getName()));
        }
    }

    @Test
    public void testFallbackToClasspathWhenSamplePathDoesNotExist() {
        SampleLoader loader = new SampleLoader();
        loader.setConfiguredSamplePath("non/existing/sample/path");
        loader.loadSamples();

        List<SampleLoader.SampleDiagram> samples = loader.getSamples();
        Assertions.assertFalse(samples.isEmpty(), "Should fall back to classpath samples");
        Assertions.assertEquals(10, samples.size(), "Should load 10 default samples");
    }

    @Test
    public void testLoadSamplesFromCustomDirectory(@TempDir Path tempDir) throws IOException {
        Path sampleFile1 = tempDir.resolve("01.custom.plantuml");
        Files.writeString(sampleFile1, "@startuml\ntitle カスタム図1\nA -> B\n@enduml");

        Path sampleFile2 = tempDir.resolve("02.custom.puml");
        Files.writeString(sampleFile2, "@startuml\n' カスタム図2\nC -> D\n@enduml");

        SampleLoader loader = new SampleLoader();
        loader.setConfiguredSamplePath(tempDir.toString());
        loader.loadSamples();

        List<SampleLoader.SampleDiagram> samples = loader.getSamples();
        Assertions.assertEquals(2, samples.size(), "Should load exactly 2 samples from custom dir");
        Assertions.assertEquals("カスタム図1", samples.get(0).getName());
        Assertions.assertEquals("01.custom.plantuml", samples.get(0).getFilename());
        Assertions.assertEquals("カスタム図2", samples.get(1).getName());
        Assertions.assertEquals("02.custom.puml", samples.get(1).getFilename());
    }
}
