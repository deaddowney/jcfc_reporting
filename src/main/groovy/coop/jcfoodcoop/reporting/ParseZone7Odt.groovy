package coop.jcfoodcoop.reporting

import org.odftoolkit.odfdom.doc.OdfDocument

OdfDocument od = OdfDocument.loadDocument(new File("src/test/resources/zone7.odt"))
def dom = od.getContentDom()
dom.childNodes .each {
    println it
}

