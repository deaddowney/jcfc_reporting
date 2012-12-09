package coop.jcfoodcoop.reporting

import java.text.DecimalFormat

/**
 * @author akrieg
 */
class CsvProductOutput {

    private String output
    private MarkupFactory markup

    public CsvProductOutput(String outputFile, MarkupFactory markupFactory) {
        this.markup = markupFactory
        this.output = outputFile
    }

    public void writeOutCsvFile(LinkedList<ProductEntry> entries) {
        PrintWriter writer = new PrintWriter(new FileWriter(output), true)

        writer.println("code,category,sub_category,sub_category2,manufacturer,product,short_description,size,case_units,each_size,unit_weight,case_weight,wholesale_price,price,sale_price,unit_price,retail_price,price_per_weight,is_priced_by_weight,valid_price,taxed,upc,origin,image_url,thumb_url,num_available,valid_order_increment,valid_split_increment,last_updated,last_updated_by,last_ordered,num_orders")
        int count = 0
        entries.each { ProductEntry entry ->


            try {
                writer.println createLine(count++, entry, markup, true)
            } catch (Exception e) {
                System.out.println("Exception writing entry " + entry);
                e.printStackTrace()
            }
        }
    }

    static def genCsvRow(Object... args) {
        //Removes all the commas out of the contents of the cells
        return args.collect { escapeValue(it) }.join(",")
    }

    private static String escapeValue(Object val) {
        if (val == null) {
            return "NULL"
        }
        if (val instanceof String) {
            String stringVal = (String) val;
            return "\"${stringVal.replaceAll("\"", "''").trim()}\""
        }
        return String.valueOf(val)
    }

    private static String createLine(int count, ProductEntry entry, MarkupFactory markupFactory, boolean inStock) {

        DecimalFormat df = new DecimalFormat("#.00");
        double wholesalePrice = 0.0
        String wholesalePriceString = entry.wholesalePrice
        if (wholesalePriceString == null || wholesalePriceString.equals('') || !wholesalePriceString.startsWith('$')) {
            System.out.println("Error: item " + entry.productDescription + " was missing its price '" + wholesalePriceString + "'.  Will mark it out of stock")
            inStock = false
        } else {
            wholesalePrice = Double.valueOf(wholesalePriceString.substring(1))
        }
        double price = markupFactory.getMarkup(entry) * wholesalePrice;

        genCsvRow(count, entry.category, entry.subcategory, "", entry.manufacturer, entry.productDescription, "", entry.size,
                0, //case_units
                "", //each_size
                0, //unit_weight
                0, //case_weight
                df.format(wholesalePrice), //wholesale_price
                df.format(price), //price
                0, //sale_price
                0, //unit_price
                0, //retail_price
                0, //price_per_weight
                0, //is_priced_by_weight
                (inStock ? 1 : 0), //valid_price
                0, //taxed
                0, //upc
                "",//origin
                null,//image_url
                null, //thumb_url
                (inStock ? 500 : 0),//num_available
                (inStock ? 1 : 0),//valid_order_increment
                (inStock ? 1 : 0),//valid_split_increment
                "0000-00-00 00:00:00",//last_updated
                "",//last_updated_by
                "0000-00-00 00:00:00",//last_ordered
                0//num_orders

        )
    }
}
