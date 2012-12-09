package coop.jcfoodcoop.reporting
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.ParseContext
import org.apache.tika.parser.Parser
import org.apache.tika.parser.odf.OpenDocumentParser
import org.apache.tika.sax.BodyContentHandler

import java.util.regex.Pattern
/**
 * @author akrieg
 */
//we dump the contents of the file to text
def temp = File.createTempFile("zone7Tmp", "txt")
def outStream = new FileOutputStream(temp)
def handler = new BodyContentHandler(outStream);

Metadata metadata = new Metadata();
Parser parser = new OpenDocumentParser();
InputStream stream = new FileInputStream(new File("src/test/resources/zone7.odt"))
ParseContext context = new ParseContext();
parser.parse(stream, handler, metadata, context);
outStream.close()

List<ProductEntry> entries = new LinkedList<ProductEntry>()
int counter = 1
ProductEntry currentEntry = new ProductEntry(id: counter++)
String currentCategory = null
def allCaps = ~/(?:[A-Z]+\s?&?\s?)+\(?[a-z\s,&]*\)?/
def priceExpression = ~/\$\d+\.?\d*/
def myPat = '(' + priceExpression.pattern() + ' / [^\\.,~\\(]*)'
def pricePer = Pattern.compile(myPat)
String currentManufacturer
def perPrice = Pattern.compile(': ([^/]*/ ' + priceExpression.pattern() + ')')
String currentPriceString = null


temp.readLines().each {
    String line ->
        //Lines that start with * are usually not relevant
        if (!line.startsWith("*")) {
            if (allCaps.matcher(line).matches()) {
                currentCategory = line

            } else if (line == null || "" == line) {
                //end of line  Check to see if we have a price and then finish processing
                currentEntry = new ProductEntry()

            } else {
                int colonIndex = line.indexOf(":")
                if (colonIndex > 0) {
                    String product = line.substring(0, colonIndex)
                    currentEntry.productDescription = product
                }
                def dollarPerMatcher = pricePer.matcher(line)
                def perDollarMatcher = perPrice.matcher(line)
                boolean priceFound = false
                if (dollarPerMatcher.find()) {
                    priceFound = true
                    currentPriceString = dollarPerMatcher.group(1)
                    def slashIndex = currentPriceString.indexOf("/")
                    currentEntry.wholesalePrice = currentPriceString.substring(0, slashIndex)
                    currentEntry.size = currentPriceString.substring(slashIndex + 1)

                } else if (perDollarMatcher.find()) {
                    priceFound = true
                    currentPriceString = perDollarMatcher.group(1)
                    def slashIndex = currentPriceString.indexOf("/")
                    currentEntry.size = currentPriceString.substring(0, slashIndex)
                    currentEntry.wholesalePrice = currentPriceString.substring(slashIndex + 1)
                }
                if (priceFound) {
                    addEntry(currentEntry, currentCategory)
                    entries.add(currentEntry)
                    counter++
                    currentEntry = new ProductEntry()
                }
                System.out.println(line)
            }
        }
}

private void addEntry(ProductEntry currentEntry, String currentCategory) {
    currentEntry.productDescription = currentEntry.productDescription?.trim()
    //Use the end of the productDescription as the subcategory, e.g.
    //Fried green tomatoes -> tomatoes
    if (currentEntry.productDescription != null) {
        String[] descWords = currentEntry.productDescription.split(" ")
        lastWord = descWords[descWords.length - 1]
        if (lastWord.startsWith("(")) {
            //For Stuff that ends with '(OG)', look at the previous word
            lastWord = descWords[descWords.length - 2]
        }
        currentEntry.subcategory = lastWord
    }
    currentEntry.wholesalePrice = currentEntry.wholesalePrice.trim()
    currentEntry.size = currentEntry.size.trim()
    currentEntry.category = currentCategory
}

String outputFile = "zoneOut.csv"


new CsvProductOutput(outputFile, new Zone7Markup()).writeOutCsvFile(entries)

