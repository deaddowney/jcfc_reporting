package coop.jcfoodcoop.productupdate

import java.io._
import java.text.DecimalFormat
import coop.jcfoodcoop.reporting.{MarkupFactory, LancasterMarkup, ProductEntry, KnowledgeBaseFactory}
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import scala.collection.mutable.ListBuffer

/**
 * @author akrieg
 */
class SLancasterParser(inputFile:File, outputFile:File) {


    def parse() {
        val df = new DecimalFormat("#.00");

        def createLine(count:Int, entry:SProductEntry, markupFactory:MarkupFactory, inStock:Boolean):String = {
            def genCsvRow(args:Any*) = {
                //Removes all the commas out of the contents of the cells
                args.map { (a)=>escapeValue(a) }.mkString(",")
            }

            /**
             * Todo: start with this and Scalify it
             * @param value
             * @return
             */
            def escapeValue(value:Any):String = {
                if (value == null) {
                    return "NULL"
                }
                if (value.isInstanceOf[String]) {
                    val stringVal = value.asInstanceOf[String]
                    return "\""+stringVal.replaceAll("\"", "''").trim()+"\""
                }
                String.valueOf(value)
            }

            var wholesalePrice = 0.0
            val wholesalePriceString = entry.wholesalePrice
            var trueInstock = inStock
            if (wholesalePriceString == null || wholesalePriceString.equals("") || !wholesalePriceString.startsWith("$")) {
                System.out.println("Error: item " + entry.productDescription + " was missing its price '" + wholesalePriceString + "'.  Will mark it out of stock")
                trueInstock = false
            } else {
                wholesalePrice = (wholesalePriceString.substring(1)).toDouble
            }
            val price = markupFactory.getMarkup(entry) * wholesalePrice;

            genCsvRow(count, entry.category, entry.subCategory, "", entry.manufacturer, entry.productDescription, "", entry.size,
                    0, //case_units
                    "", //each_size
                    0, //unit_weight
                    0, //case_weight
                    df.format(price), //price
                    0, //sale_price
                    0, //unit_price
                    0, //retail_price
                    0, //price_per_weight
                    0, //is_priced_by_weight
                    if(trueInstock)  1 else 0, //valid_price
                    0, //taxed
                    0, //upc
                    "",//origin
                    null,//image_url
                    null, //thumb_url
                    if(trueInstock) 500 else 0,//num_available
                    if(trueInstock) 1 else 0,//valid_order_increment
                    if(trueInstock)  1 else 0,//valid_split_increment
                    "0000-00-00 00:00:00",//last_updated
                    "",//last_updated_by
                    "0000-00-00 00:00:00",//last_ordered
                    0//num_orders)

            )
        }
        val  doc = new HWPFDocument(new BufferedInputStream(new FileInputStream(inputFile)))
        val ext = new WordExtractor(doc)
        val entries = collection.mutable.ListBuffer[SProductEntry]()
        val markup = new LancasterMarkup()

        def text = ext.getParagraphText


        val preparsedEntries = preparseEntries(text)

        for(index <-0 until preparsedEntries.length) {
            //Need to copy the categories from the previous line
            val preparsed = preparsedEntries(index)

            val priceDesc = preparsed.priceDesc
            val rawDesc = preparsed.description
            val perIndex = priceDesc.indexOf(" per")
            var price:String = null
            var size:String= null
            if (perIndex > 0) {
                price = priceDesc.substring(0, perIndex)
                if (priceDesc.size > perIndex + 5) {
                    size = priceDesc.substring(perIndex + 5)//5 is the size of " per "
                } else {
                    //
                    println("Warning: could not parse out size from description :"+priceDesc+" for" +
                            " "+rawDesc+".  \n" +
                            "Assigning size = 1")
                    size = "1"
                }

            }


            entries+=(SLancasterProductEntryFactory.fromLine(
                                preparsed.category,
                                null,
                                rawDesc,
                                price,
                                size))

        }

        val writer = new PrintWriter(new FileWriter(outputFile))


        writer.println("code,category,sub_category,sub_category2,manufacturer,product,short_description,size,case_units,each_size,unit_weight,case_weight,wholesale_price,price,sale_price,unit_price,retail_price,price_per_weight,is_priced_by_weight,valid_price,taxed,upc,origin,image_url,thumb_url,num_available,valid_order_increment,valid_split_increment,last_updated,last_updated_by,last_ordered,num_orders")
        var count = 0
        val problems = ListBuffer[SProductEntry]()
        entries.foreach {(entry) =>

            try {
                writer.println(createLine(count, entry, markup, inStock = true))
                count+=1
            } catch  {
                case (e:Exception) =>
                System.out.println("Exception writing entry " + entry)
                e.printStackTrace()
            }
        }

        System.out.println("Printed "+count+" records to "+outputFile)
        writer.flush()
        writer.close()

    }


    /**
     * Goes through the document and pulls out the product entries with their categories
     * @param text
     * @return
     */
    private def preparseEntries(text:Array[String]) : List[PreParsedProductEntry] = {
        var previousEntryLineNum = 0

        var lastLine:String = null
        val preparsedEntries = new ListBuffer[PreParsedProductEntry]()
        for (i <- 0 until text.length) {
            val line = text(i).trim()
            if (!line.isEmpty) {
                //TODO: Qty: line (0), Description(line1), Price (line 2)
                if ("Qty:".equals(line)) {
                    /**
                     *  We're in a Product Entry.  The next two lines will describe the entry, e.g.:
                     * Qty: 
                     * Burgers, OG Vegan Burgers Chipotle (Frozen): Asherah's Gourmet 
                     * $32.00 per 1 Case - 6/4pk 
                     */

                    //Sanity check, just make sure we have 2 more lines to read
                    if (i + 2 > text.length) {
                        throw new RuntimeException("Malformed file, missing product description")
                    }

                    val rawDesc = text(i + 1).trim()
                    val priceDesc = text(i + 2).trim()
                    val preparsed = new PreParsedProductEntry(i, null, rawDesc, priceDesc)
                    if (preparsed.linNum - previousEntryLineNum > 5) {
                        preparsed.category = text(preparsed.linNum -1).trim()
                    } else {
                        preparsed.category = preparsedEntries.last.category
                    }

                    preparsedEntries+= preparsed


                    //There are 5 "lines" between each entry


                    previousEntryLineNum = i


                } else {
                    //We're reading a non product or transitioning from one category to another
                    lastLine = line

                }
            }
        }
        preparsedEntries.toList
    }

    /**
     * When we're parsing the Lancaster file, we run into "Qty:"  product descriptions   qtyString, e.g.
     * Qty: 	Eggs, Duck (Pastured, Water): Sunnyside Farm 	$5.75 per 1/2 Dozen
     *
     * This captures this data in a functional way as well as the enclosing category
     *
     * @param description
     * @param priceDesc
     */
    case class PreParsedProductEntry(linNum:Int, var category:String, description:String, priceDesc:String) {

    }

}


