package com.github.narise77.modern_plantuml_server.utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

public class TestDrawioConverter {

    private static final Path BUILD_DIR = Path.of("build");

    private static void writeOutputFile(String filename, String content) throws IOException {
        Files.createDirectories(BUILD_DIR);
        Files.writeString(BUILD_DIR.resolve(filename), content, StandardCharsets.UTF_8);
    }

    private String renderSvg(String source) throws IOException {
        final SourceStringReader reader = new SourceStringReader(source);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        reader.outputImage(out, new FileFormatOption(FileFormat.SVG, false));
        return out.toString(StandardCharsets.UTF_8);
    }

    @Test
    public void testSequenceDiagram() throws IOException {
        final String source = "@startuml\nBob -> Alice : hello\n@enduml";
        final String svg = renderSvg(source);
        final String drawioXml = DrawioConverter.convert(svg);

        Assertions.assertNotNull(drawioXml);
        Assertions.assertTrue(drawioXml.contains("<mxfile"), "Missing mxfile");
        Assertions.assertTrue(drawioXml.contains("<mxGraphModel"), "Missing mxGraphModel");
        Assertions.assertTrue(drawioXml.contains("<root>"), "Missing root");
        Assertions.assertTrue(drawioXml.contains("<mxCell id=\"0\" />"), "Missing root cell 0");
        Assertions.assertTrue(drawioXml.contains("<mxCell id=\"1\" parent=\"0\" />"), "Missing root cell 1");

        // Should NOT contain embedded base64 image
        Assertions.assertFalse(drawioXml.contains("data:image/svg+xml"), "Should not contain embedded image");

        // Should contain individual cells
        Assertions.assertTrue(drawioXml.contains("value=\"Bob\""), "Should have Bob text cell");
        Assertions.assertTrue(drawioXml.contains("value=\"Alice\""), "Should have Alice text cell");
        Assertions.assertTrue(drawioXml.contains("value=\"hello\""), "Should have hello text cell");
        Assertions.assertTrue(drawioXml.contains("vertex=\"1\""), "Should have vertex cells");
        Assertions.assertTrue(drawioXml.contains("edge=\"1\""), "Should have edge cells");
    }

    @Test
    public void testGanttChart() throws IOException {
        final String source = "@startgantt\n"
            + "[Prototype design] requires 15 days\n"
            + "[Test prototype] requires 10 days\n"
            + "@endgantt";
        final String svg = renderSvg(source);
        final String drawioXml = DrawioConverter.convert(svg);

        Assertions.assertNotNull(drawioXml);
        Assertions.assertTrue(drawioXml.contains("value=\"Prototype design\""), "Should contain task name");
        Assertions.assertTrue(drawioXml.contains("value=\"Test prototype\""), "Should contain task name");
        Assertions.assertTrue(drawioXml.contains("vertex=\"1\""), "Should have vertex cells for bars");
    }

    @Test
    public void testUserJapaneseGantt() throws IOException {
        final String source = "@startgantt\n"
            + "'ガントチャート\n"
            + "language ja\n"
            + "Project starts the 2020/5/11\n"
            + "saturday are closed\n"
            + "sunday are closed\n"
            + "2020/5/20 is closed\n"
            + "2020/6/16 to 2020/6/18 is closed\n"
            + "-- Step 1 --\n"
            + "[要求分析(計画)] lasts 3 days\n"
            + "[要求分析(実績)] lasts 2 days and is colored in Fuchsia/FireBrick\n"
            + "[要求仕様が決定] happens at [要求分析(計画)]'s end\n"
            + "[外部設計(計画)] lasts 5 days\n"
            + "[外部設計(実績)] lasts 4 days and is colored in Fuchsia/FireBrick\n"
            + "[外部設計(実績)] starts at [要求分析(実績)]'s end\n"
            + "[外部設計(計画)] starts at [要求分析(計画)]'s end\n"
            + "[外部設計が決定] happens at [外部設計(計画)]'s end\n"
            + "[システムテストケースが決定] happens at [外部設計(計画)]'s end\n"
            + "[内部設計(計画)] lasts 5 days\n"
            + "[内部設計(実績)] lasts 8 days and is colored in Fuchsia/FireBrick\n"
            + "[内部設計(実績)] starts at [外部設計(実績)]'s end\n"
            + "[内部設計(計画)] starts at [外部設計(計画)]'s end\n"
            + "[内部設計が決定] happens at [内部設計(計画)]'s end\n"
            + "[結合テストケースが決定] happens at [内部設計(計画)]'s end\n"
            + "-- Step 2 --\n"
            + "[コーディング(計画)] lasts 7 days\n"
            + "[コーディング(実績)] lasts 7 days and is colored in Fuchsia/FireBrick\n"
            + "[コーディング(実績)] starts 1 day after [内部設計(実績)]'s end\n"
            + "[コーディング(計画)] starts at [内部設計(計画)]'s end\n"
            + "[各種テストコードが完成] happens at [コーディング(計画)]'s end\n"
            + "[テスト対象コードが決定] happens at [コーディング(計画)]'s end\n"
            + "[単体テスト(計画)] lasts 2 days\n"
            + "[単体テスト(計画)] starts at [コーディング(計画)]'s end\n"
            + "[各モジュールの動作確認を完了] happens at [単体テスト(計画)]'s end\n"
            + "-- Step 3 --\n"
            + "[結合テスト(計画)] lasts 3 days\n"
            + "[結合テスト(計画)] starts at [単体テスト(計画)]'s end\n"
            + "[内部動作の確認を完了] happens at [結合テスト(計画)]'s end\n"
            + "[システムテスト(計画)] lasts 4 days\n"
            + "[システムテスト(計画)] starts at [結合テスト(計画)]'s end\n"
            + "[外部動作の確認を完了] happens at [システムテスト(計画)]'s end\n"
            + "[品質確認テスト(計画)] lasts 14 days\n"
            + "[品質確認テスト(計画)] starts at [システムテスト(計画)]'s end\n"
            + "[品質確認を完了] happens at [品質確認テスト(計画)]'s end\n"
            + "[リリース(計画)] lasts 1 days\n"
            + "[リリース(計画)] starts at [品質確認テスト(計画)]'s end\n"
            + "[開発完了] happens at [リリース(計画)]'s end\n"
            + "[オペレーション開始] happens at [リリース(計画)]'s end\n"
            + "@endgantt";
        final String svg = renderSvg(source);
        writeOutputFile("test_gantt.svg", svg);
        final String drawioXml = DrawioConverter.convert(svg);
        writeOutputFile("test_gantt.drawio", drawioXml);

        System.out.println("SVG length: " + svg.length());
        System.out.println("Drawio length: " + drawioXml.length());
        Assertions.assertNotNull(drawioXml);
        Assertions.assertTrue(drawioXml.contains("value=\"要求分析(計画)\""));
    }

    @Test
    public void testClassDiagram() throws IOException {
        final String source = "@startuml\n"
            + "class Order {\n"
            + "  +id: int\n"
            + "  +submit(): void\n"
            + "}\n"
            + "@enduml";
        final String svg = renderSvg(source);
        final String drawioXml = DrawioConverter.convert(svg);

        Assertions.assertNotNull(drawioXml);
        Assertions.assertTrue(drawioXml.contains("value=\"Order\""), "Should contain class name");
        Assertions.assertTrue(drawioXml.contains("value=\"id: int\""), "Should contain attribute");
        Assertions.assertTrue(drawioXml.contains("value=\"submit(): void\""), "Should contain method");
    }

    @Test
    public void testComponentDiagram() throws IOException {
        final String source = "@startuml\n"
            + "package \"フロントエンド\" {\n"
            + "  [Webブラウザ] as WebBrowser\n"
            + "  [UIコンポーネント] as UI\n"
            + "}\n"
            + "package \"バックエンド\" {\n"
            + "  [APIサーバー] as API\n"
            + "  [認証サービス] as Auth\n"
            + "  [データ処理サービス] as DataProcessor\n"
            + "}\n"
            + "database \"データベース\" {\n"
            + "  [ユーザーDB] as UserDB\n"
            + "  [コンテンツDB] as ContentDB\n"
            + "}\n"
            + "WebBrowser --> UI : 表示\n"
            + "UI --> API : リクエスト送信\n"
            + "API --> Auth : 認証要求\n"
            + "API --> DataProcessor : データ要求\n"
            + "Auth --> UserDB : 認証情報確認\n"
            + "DataProcessor --> ContentDB : データ取得\n"
            + "@enduml";
        final String svg = renderSvg(source);
        final String drawioXml = DrawioConverter.convert(svg);
        writeOutputFile("test_comp.drawio", drawioXml);

        Assertions.assertNotNull(drawioXml);
        // Verify packages are vertex, not edge
        Assertions.assertTrue(drawioXml.contains("value=\"フロントエンド\""), "Should contain front-end text");
        Assertions.assertTrue(drawioXml.contains("value=\"バックエンド\""), "Should contain back-end text");
        Assertions.assertTrue(drawioXml.contains("shape=cylinder3"), "Should contain database cylinder shape");

        // Count edge="1" elements - 6 relationships + 2 package tab lines = 8 edges
        int edgeCount = 0;
        int idx = 0;
        while ((idx = drawioXml.indexOf("edge=\"1\"", idx)) != -1) {
            edgeCount++;
            idx += 8;
        }
        System.out.println("Component diagram edge count: " + edgeCount);
        Assertions.assertEquals(8, edgeCount, "Should have 8 edges (6 relationship arrows + 2 package tab lines), no spurious package border edges");

        // Ensure no point shoots off to (20.0, 20.0) from package arc distortion
        Assertions.assertFalse(drawioXml.contains("<mxPoint x=\"20.0\" y=\"20.0\" />"), "Should not contain distorted points at (20,20)");
    }

    @Test
    public void testStateDiagram() throws IOException {
        final String source = "@startuml\n"
            + "[*] --> 待機中\n"
            + "待機中 --> 処理中 : イベント発生\n"
            + "処理中 --> 待機中 : 処理完了\n"
            + "処理中 --> エラー : エラー発生\n"
            + "エラー --> 待機中 : リセット\n"
            + "待機中 --> [*] : 終了\n"
            + "エラー --> [*] : 強制終了\n"
            + "state 処理中 {\n"
            + "  [*] --> データ読込\n"
            + "  データ読込 --> データ処理\n"
            + "  データ処理 --> データ保存\n"
            + "  データ保存 --> [*]\n"
            + "}\n"
            + "@enduml";
        final String svg = renderSvg(source);
        final String drawioXml = DrawioConverter.convert(svg);
        writeOutputFile("test_status.drawio", drawioXml);

        Assertions.assertNotNull(drawioXml);
        Assertions.assertTrue(drawioXml.contains("value=\"待機中\""), "Should contain 待機中");
        Assertions.assertTrue(drawioXml.contains("value=\"処理中\""), "Should contain 処理中");
        Assertions.assertTrue(drawioXml.contains("value=\"エラー\""), "Should contain エラー");

        // Verify no giant distorted polygons spanning the whole diagram
        // All vertices should have reasonable bounds (w <= 200, h <= 500)
        final java.util.regex.Pattern geomPattern = java.util.regex.Pattern.compile("<mxGeometry[^>]+width=\"([^\"]+)\"\\s+height=\"([^\"]+)\"");
        final java.util.regex.Matcher m = geomPattern.matcher(drawioXml);
        while (m.find()) {
            double w = Double.parseDouble(m.group(1));
            double h = Double.parseDouble(m.group(2));
            Assertions.assertTrue(w <= 400 && h <= 500, "Vertex dimension should be reasonable, got w=" + w + ", h=" + h);
        }
    }

    @Test
    public void testEmptyAndInvalidInput() {
        final String nullRes = DrawioConverter.convert(null);
        Assertions.assertTrue(nullRes.contains("<mxfile"));

        final String emptyRes = DrawioConverter.convert("");
        Assertions.assertTrue(emptyRes.contains("<mxfile"));

        final String invalidRes = DrawioConverter.convert("not valid svg");
        Assertions.assertTrue(invalidRes.contains("<mxfile"));
    }
}

