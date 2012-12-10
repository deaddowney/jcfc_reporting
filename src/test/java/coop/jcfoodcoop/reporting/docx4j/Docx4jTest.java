package coop.jcfoodcoop.reporting.docx4j;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.junit.Test;

/**
 * @author akrieg
 */
public class Docx4jTest {


    @Test
    public final void testLoadDoc() throws Exception {
        WordprocessingMLPackage wordMLPackage =
              WordprocessingMLPackage.load(new java.io.File("src/test/resources/zone7.docx"));
        MainDocumentPart mainPart = wordMLPackage.getMainDocumentPart();
        for (Object o : mainPart.getContent()) {
            System.out.println("Part = "+o.getClass()+" content = "+o);
        }
    }
}
