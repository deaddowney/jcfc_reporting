package coop.jcfoodcoop.reporting

import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument

System.setProperty("org.xml.sax.driver", "org.apache.xerces.parsers.SAXParser")

def inputFile = "src/test/resources/zone7.docx"
XWPFDocument doc = new XWPFDocument(new BufferedInputStream(new FileInputStream(inputFile)))
XWPFWordExtractor ext = new XWPFWordExtractor(doc);
String lastLine = null;
String currentTopic = null;
List<ProductEntry> entries = new LinkedList<ProductEntry>();
def text = doc.getParagraphs()
//BERRIES & TREE FRUIT
//VEGGIES (fruit)
def allCaps = ~/(?:[A-Z]+\s?&?\s?)+\(?[a-z\s,&]*\)?/
for(int i = 0; i< text.size(); i++) {
    textThing = text[i]
    lastLine = textThing.text
    if (allCaps.matcher(lastLine).matches()) {
        currentTopic = lastLine
    } else {
        int colonIndex = lastLine.indexOf(":")
        if (colonIndex > 0) {
            System.out.println("Topic = ${currentTopic} Product = ${lastLine.substring(0, colonIndex)}")
        }
    }

}

