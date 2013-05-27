package coop.jcfoodcoop.productupdate

import java.io.Reader
import au.com.bytecode.opencsv.CSVReader

/**
 * @author akrieg
 */
object SKnowledgeBaseFactory {
    def parseCsv(reader:Reader) : SKnowledgeBase =  {
        val b = new SKnowledgeBase()
        parseCsv(reader, b)
        b
    }

    /**
     * Adds to an existing KnowledgeBase
     * @param read
     * @param b
     */
    def parseCsv(read:Reader, b:SKnowledgeBase) = {
        val reader = new CSVReader(read)
        val header = reader.readNext() // just get this header out of the way
        var line = reader.readNext()
        while (line != null) {
            if (line.length > 6) {
                val category = trim(line(1))
                val rawSubcategory = trim(line(2))
                val  subcategory = trim(line(3))
                val manufacturer = trim(line(5))
                val product = trim(line(6))
                val size = trim(line(8))
                val price = trim(line(13))
                b.addEntry(new SProductEntry(1, category, subcategory,"","",""))
            } else {
                System.out.println("Skipping bad row ${line}")
            }

            line = reader.readNext()
        }
        b
    }

    /** removes double quotes **/
    def trim(strVal :String) : String = {
        if (strVal.contains("NULL")) {
            return null
        }
        strVal.replaceAll("\"", "")
    }

    /**
     * Reads a persisted KnowledgeBase file
     * @param file
     * @return
     */
    def parseKbFile(file:String): SKnowledgeBase = {
        val kb = new SKnowledgeBase()
        file.split("\n").foreach {
            (line:String) => {
            val parts = line.split("::")
            val manufacturer = parts(0)
            val product = parts(1)
            val category = parts(2)
            val subCategory = parts(3)
            kb.addEntry(
                new SProductEntry(1, category, subCategory,"","",""))
            }
        }

        kb
    }
}



