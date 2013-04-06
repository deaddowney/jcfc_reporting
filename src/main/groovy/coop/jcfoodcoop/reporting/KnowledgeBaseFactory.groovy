package coop.jcfoodcoop.reporting

import au.com.bytecode.opencsv.CSVReader

/**
 * Parses a previous Lancaster file and adds it to a "KnowledgeBase"
 * @author akrieg
 */
class KnowledgeBaseFactory {
    public static KnowledgeBase parseCsv(Reader reader) {
        KnowledgeBase b = new KnowledgeBase();

        parseCsv(reader, b)
        
        return b;

    }

    /**
     * Adds to an existing KnowledgeBase
     * @param read
     * @param b
     */
    public static void parseCsv(Reader read, KnowledgeBase b) {
        boolean headerEncountered = false
        CSVReader reader = new CSVReader(read)
        reader.readAll().each {
            String[] cells ->
            if (headerEncountered) {
                if (cells.length > 6) {
                    String category = trim(cells[1])
                    String rawSubcategory = trim(cells[2])
                    String subcategory = trim(cells[3])
                    String manufacturer = trim(cells[5])
                    String product = trim(cells[6])
                    String size = trim(cells[8])
                    String price = trim(cells[13])
                    b.addEntry(new ProductEntry(
                            productDescription: product,
                            rawSubCategory: rawSubcategory,
                            subcategory: subcategory,
                            manufacturer: manufacturer,
                            category: category,
                            size: size,
                            wholesalePrice: price
                    ))
                } else {
                    System.out.println("Skipping bad row ${cells}")
                    return;
                }
            } else {
                //First line is the header, skip it!
                headerEncountered = true
            }
        }
    }

    /** removes double quotes **/
    private static String trim(String val) {
        if (val.contains("NULL")) {
            return null;
        }
        return val.replaceAll("\"", "");
    }

    /**
     * Reads a persisted KnowledgeBase file
     * @param file
     * @return
     */
    public static KnowledgeBase parseKbFile(String file) {
        KnowledgeBase kb = new KnowledgeBase()
        file.eachLine {
            String line ->
            String[] parts = line.split("::")
            String manufacturer = parts[0]
            String product = parts[1]
            String category = parts[2]
            String subCategory = parts[3]
            kb.addEntry(new ProductEntry(manufacturer: manufacturer, productDescription: product, category: category, subcategory: subCategory))
        }

        return kb
    }

}
