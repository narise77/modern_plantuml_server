package com.github.narise77.modern_plantuml_server;

import org.junit.jupiter.api.Test;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.preproc.Defines;
import net.sourceforge.plantuml.core.Diagram;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import java.io.ByteArrayOutputStream;

public class TestDpi {
    @Test
    public void testGanttWithDpi() throws Exception {
        String enc = "pPInReCm48PtFyLjQGSBcuQadOWwPEvUPJY1bT0IE_8PLNdxWZ499IH1QeTwiCJTV_zzlfDOaKFhjgYTOxEqFQn4lQrmg-036Q6hR8v7GAiWAmsfd56b1v6kj2v-i-P3PGwyCO7RAOX5BA8aafATUxodwGPsU3WeJLx0HSA5REBX3DCBBaDwc5boEG_EF-hpWWub7WcC1iAz0ck-00cQtftJJKrdwXyWaYX-WO8WCwMnAeUYgIZtHhSo5fuP2ozg1-DZYqWkm1deFfvx0iOvDDN0UOlqaAQF6oYHwk4zGJrdkep7FWweym7FkifsL62qBgnwjKNs6Vo3UzyZENdte_FfpgVAi6bqS0QT4IYTJz9S9LoE4Yv_ItYVxvAklw0REvgIkiKoHJRQARaEaTo5I4UsDbbxTpvIN6SRp_qXtBy3MjMt_zj-0m00";
        String uml = com.github.narise77.modern_plantuml_server.utility.UmlExtractor.getUmlSource(enc);
        System.out.println("ORIGINAL UML:\n" + uml);

        // Test with mock request with dpi=150
        org.springframework.mock.web.MockHttpServletRequest req = new org.springframework.mock.web.MockHttpServletRequest();
        req.setParameter("dpi", "150");
        org.springframework.mock.web.MockHttpServletResponse res = new org.springframework.mock.web.MockHttpServletResponse();

        DiagramResponse resp = new DiagramResponse(res, FileFormat.PNG, req);
        resp.sendDiagram(uml, 0);

        byte[] bytes = res.getContentAsByteArray();
        System.out.println("STATUS: " + res.getStatus());
        System.out.println("OUTPUT BYTES: " + bytes.length);

        String umlWithComment = uml.replace("@endgantt", "'150dpi\n@endgantt");
        org.springframework.mock.web.MockHttpServletRequest reqNoParam = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.mock.web.MockHttpServletResponse resNoParam = new org.springframework.mock.web.MockHttpServletResponse();
        DiagramResponse respNoParam = new DiagramResponse(resNoParam, FileFormat.PNG, reqNoParam);
        respNoParam.sendDiagram(umlWithComment, 0);
        System.out.println("NO PARAM COMMENT BYTES: " + resNoParam.getContentAsByteArray().length);

        org.springframework.mock.web.MockHttpServletResponse resDef = new org.springframework.mock.web.MockHttpServletResponse();
        DiagramResponse respDef = new DiagramResponse(resDef, FileFormat.PNG, new org.springframework.mock.web.MockHttpServletRequest());
        respDef.sendDiagram(uml, 0);

        org.junit.jupiter.api.Assertions.assertTrue(resNoParam.getContentAsByteArray().length > resDef.getContentAsByteArray().length * 1.5);
    }

    private void check(String name, String uml) throws Exception {
        SourceStringReader reader = new SourceStringReader(Defines.createEmpty(), uml);
        System.out.println("=== " + name + " ===");
        System.out.println("Blocks size: " + reader.getBlocks().size());
        if (!reader.getBlocks().isEmpty()) {
            Diagram d = reader.getBlocks().get(0).getDiagram();
            System.out.println("Diagram class: " + d.getClass().getName());
            System.out.println("WarningOrError: " + d.getWarningOrError());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            d.exportDiagram(baos, 0, new FileFormatOption(FileFormat.PNG));
            System.out.println("PNG bytes: " + baos.toByteArray().length);
        }
    }
}
