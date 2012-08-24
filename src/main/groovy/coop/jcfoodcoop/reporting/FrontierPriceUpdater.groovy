package coop.jcfoodcoop.reporting

import au.com.bytecode.opencsv.CSVReader
import au.com.bytecode.opencsv.CSVWriter

/**
 * Merges a Price update file with an existing Frontier data file.
 *
 * @author akrieg
 */
class FrontierPriceUpdater {

    private String sourceFilePath;
    private String updateFilePath;

    public FrontierPriceUpdater(String sourceFilePath, String updateFilePath) {
        this.sourceFilePath = sourceFilePath;
        this.updateFilePath = updateFilePath;
    }

    public void update(String outputFile) {
        def products = parseSourceToMap(sourceFilePath)
        def updates = parseUpdateFile(updateFilePath)

        updates.each {
            code, price ->
            String[] row = products.get(code);
            if (row != null) {
                row[7] = '$'+price;
            }
        }

        // Copy over the header from the source file.
        String[] header = parseHeader(sourceFilePath)

        CSVWriter writer = new CSVWriter(new FileWriter(outputFile) )
        writer.writeNext(header)
        products.each {
            code, row ->
            writer.writeNext(row)
        }
        writer.close()
    }

    private String[] parseHeader(String csvPath) {
        CSVReader reader = new CSVReader(new FileReader(csvPath));
        String[] header = reader.readNext()
        reader.close();
        return header;
    }

    /**
     * Parses the source file (the complete Frontier database) and turns it into a Map of product code -> row
     * @param outputPath
     * @return
     */
    private Map<String, String[]> parseSourceToMap(String sourceFilePath) {
        boolean headerEncountered = false;
        CSVReader reader = new CSVReader(new FileReader(sourceFilePath))

        /**
         * Columns in the source file are:
        * Bar Code
        * Item Number
        * WPCCATALOGNAME
        * Description
        * Long Description
        * Case Size
        *  Unit Weight
        *  Wholesale Price
        *  SRP
        *  Brand
        *  Origin
        * Kosher
        * Organic
        * Bontanical Name
        * Ingredients
        * Label Directions
        * Suggested Uses
        * Safety Info
        * Product Info
        * GRAPHICNAME
        * THUMBNAILNAME
        * COMMONPICTURENAME
        * UOM
        * UOMPEREA*/

        Map<String, String[]> result = new LinkedHashMap<String, String[]>();
        reader.readAll().each {
            String[] cells ->
            if (headerEncountered) {
                if (cells.length > 3) {
                    String codeString = cells[1];
                    if (codeString == null || codeString.trim().equals("")) {
                        return;
                    }
                    result.put(codeString, cells)
                } else {
                    System.out.println("Skipping bad row ${cells}")
                }
            } else {
                //First line is the header, skip it!
                headerEncountered = true
            }
        }
        reader.close()
        return result;
    }

    private Map<String, String> parseUpdateFile(String updateFile) {
        boolean headerEncountered = false;
        CSVReader reader = new CSVReader(new FileReader(updateFile))

        /**
         * Columns in the update file are:
         * ItemNumber
         * Description
         * ListPrice
         * SalesPrice
        */

        Map<String, String> result = new HashMap<String, String>();
        reader.readAll().each {
            String[] cells ->
            if (headerEncountered) {
                if (cells.length > 3) {
                    def code = cells[0];
                    String salesPrice = cells[3]
                    result.put(code, salesPrice)
                } else {
                    System.out.println("Skipping bad row ${cells}")
                }
            } else {
                //First line is the header, skip it!
                headerEncountered = true
            }
        }
        return result;
    }




}
