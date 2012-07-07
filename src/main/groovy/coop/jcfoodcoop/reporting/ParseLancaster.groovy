package coop.jcfoodcoop.reporting

import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor

import java.text.DecimalFormat

/**
 * Parses the Word Doc we get from Lancaster farm fresh
 * and turns it into a csv
 *
 * @author akrieg
 */
def inputFile = "src/test/resources/pricelist_july_11_and_12.doc"
def outputFile = "out.csv"
KnowledgeBase kb = KnowledgeBaseFactory.parseCsv("src/test/resources/db-6-13-2012.csv")
HWPFDocument doc = new HWPFDocument(new BufferedInputStream(new FileInputStream(inputFile)))
WordExtractor ext = new WordExtractor(doc);
String lastLine = null;
ProductEntry currentEntry = null;
List<ProductEntry> entries = new LinkedList<ProductEntry>();

MarkupFactory markup = new LancasterMarkup()
def text = ext.paragraphText
for(int i = 0; i< text.length; i++) {
    String line = text[i].trim()
    if (!line.isEmpty()) {
        if ("Qty:".equals(line)) {
            /**
             *  We're in a Product Entry.  The next two lines will describe the entry, e.g.:
             * Qty: 
             * Burgers, OG Vegan Burgers Chipotle (Frozen): Asherah's Gourmet 
             * $32.00 per 1 Case - 6/4pk 
             */

            //Sanity check, just make sure we have 2 more lines to read
            if(i+2 > text.length) {
                throw new RuntimeException("Malformed file, missing product description")
            }

            String rawDesc = text[i+1].trim()
            String priceDesc = text[i+2].trim()
            int perIndex =  priceDesc.indexOf(" per")
            String price = null
            String size = null
            if (perIndex > 0) {
                price = priceDesc.substring(0, perIndex)
                if (priceDesc.size() > perIndex+5) {
                    size = priceDesc.substring(perIndex+5)//5 is the size of " per "
                } else {
                    //
                    System.out.println("Warning: could not parse out size from description : ${priceDesc} for"+
                                            " ${rawDesc}.  \n"+
                                            "Assigning size = 1")
                    size = "1"
                }

            }

            //The first time we come into a Product entry, the category is the previous
            // line
            String productCategory = lastLine
            String subCategory = null

            //Within a category, you get all the products, one after another
            if (currentEntry != null) {
                productCategory = currentEntry.category
                subCategory = currentEntry.subcategory

            }
            currentEntry = LancasterProductEntryFactory.fromLine(
                    productCategory,
                    subCategory,
                    rawDesc,
                    price,
                    size)
            entries.add(currentEntry)
            i+=2

        } else {
            //We're reading a non product or transitioning from one category to another
            lastLine = line
            currentEntry = null

        }
    }
}
def genCsvRow(Object... args) {
    //Removes all the commas out of the contents of the cells
    return args.collect {escapeValue(it)}.join(",")
}

private String escapeValue(Object val) {
    if (val == null) {
        return "NULL"
    }
    if (val instanceof String) {
        String stringVal = (String) val;
        return "\"${stringVal.replaceAll("\"","''").trim()}\""
    }
    return String.valueOf(val)
}


File out = new File(outputFile)
PrintWriter writer = new PrintWriter(new FileWriter(out, true))

writer.println ("code,category,sub_category,sub_category2,manufacturer,product,short_description,size,case_units,each_size,unit_weight,case_weight,wholesale_price,price,sale_price,unit_price,retail_price,price_per_weight,is_priced_by_weight,valid_price,taxed,upc,origin,image_url,thumb_url,num_available,valid_order_increment,valid_split_increment,last_updated,last_updated_by,last_ordered,num_orders")
int count = 0
List<ProductEntry> problems = new LinkedList<ProductEntry>()
entries.each { ProductEntry entry ->

    if (entry.subcategory == null) {
        ProductEntry kbEntry = kb.remove(entry)
        if (kbEntry != null) {
            System.out.println("Found entry ${kbEntry.manufacturer} ${kbEntry.subcategory}")
            entry.subcategory = kbEntry.subcategory
            entry.category = kbEntry.category
        } else {
            System.out.println("Unable to find subcategory for entry ${entry.productDescription}")
            problems.add(entry)
            return
        }
    }
    try {
        writer.println createLine(count++, entry, markup, true)
    } catch(Exception e) {
        System.out.println("Exception writing entry "+entry);
        e.printStackTrace()
    }
}


  //  writer.println """\"${entry.category}\"\t\"${entry.productDescription}\"\t\"${entry.manufacturer}\"\t\"${entry.organic}\"\t\"${entry.price}\"\t\"${entry.size}\"\t\"${entry.salesPrice}\""""


writer.println("##PROBLEMS.  Could not find a subcategory for these items##")
problems.each {ProductEntry entry ->
    try {
        writer.println createLine(count++, entry, markup,true)
    } catch(Exception e) {
        System.out.println("Exception writing problem entry "+entry);
        e.printStackTrace()
    }
}
writer.println("##DISCONTINUED ITEMS##")
kb.entries().each {
    try {
        writer.println createLine(count++, it, markup, false)
    } catch(Exception e) {
        System.out.println("Exception writing discontinuted item "+it);
        e.printStackTrace()
    }
}

private String createLine(int count, ProductEntry entry, MarkupFactory markupFactory, boolean inStock) {

    DecimalFormat df = new DecimalFormat("#.00");
    double wholesalePrice = 0.0
    String wholesalePriceString = entry.wholesalePrice
    if (wholesalePriceString == null || wholesalePriceString.equals('') || !wholesalePriceString.startsWith('$')) {
        System.out.println("Error: item "+entry.productDescription+" was missing its price '"+wholesalePriceString+"'.  Will mark it out of stock")
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
            (inStock?1:0), //valid_price
            0, //taxed
            0, //upc
            "",//origin
            null,//image_url
            null, //thumb_url
            (inStock?500:0),//num_available
            (inStock?1:0),//valid_order_increment
            (inStock?1:0),//valid_split_increment
            "0000-00-00 00:00:00",//last_updated
            "",//last_updated_by
            "0000-00-00 00:00:00",//last_ordered
            0//num_orders

    )
}

System.out.println("Printed ${count} records to ${outputFile}")
writer.close()
