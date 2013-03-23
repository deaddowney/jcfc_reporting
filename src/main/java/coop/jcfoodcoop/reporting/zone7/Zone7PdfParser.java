package coop.jcfoodcoop.reporting.zone7;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import coop.jcfoodcoop.reporting.CsvProductOutput;
import coop.jcfoodcoop.reporting.ProductEntry;
import coop.jcfoodcoop.reporting.Zone7Markup;


/**
 * @author akrieg
 */
public class Zone7PdfParser {

    public static final Pattern ALL_CAPS_PATTERN = Pattern.compile("(?:[A-Z]+\\s?&?\\s?)+\\(?[a-z\\s,&]*\\)?");
    public static final Pattern PRICE_EXPRESSION_PATTERN = Pattern.compile("\\$\\d+\\.?\\d*");
    /** Identifies a $XX.XX / foo pattern */
    public static final Pattern PRICE_PER_PATTERN = Pattern.compile('(' + PRICE_EXPRESSION_PATTERN.pattern() + " / [^\\.,~\\(]*)");

    /** Identifies a foo / $XX.XX pattern **/
    public static final Pattern PER_PRICE_PATTERN = Pattern.compile(": ([^/]* / " + PRICE_EXPRESSION_PATTERN.pattern() + ")");

    public void parse(String inFile, String outFile) {
        PDDocument doc = null;

        try {
            /**
             * @author akrieg
             */
            //we dump the contents of the file to text


            Collection<ProductEntry> entries = new LinkedList<ProductEntry>();
            ProductEntry currentEntry = new ProductEntry();
            String currentCategory = null;
            Pattern allCaps = ALL_CAPS_PATTERN;
            Pattern priceExpression = PRICE_EXPRESSION_PATTERN;
            String myPat = '(' + priceExpression.pattern() + " / [^\\.,~\\(]*)";
            Pattern pricePer = PRICE_PER_PATTERN;
            Pattern perPrice = PER_PRICE_PATTERN;
            String currentPriceString = null;


            Parser parser = new PDFParser();
           // parser.setSortByPosition(true); // or false
            ContentHandler handler = new BodyContentHandler(new ContentHandler() {
                public void setDocumentLocator(Locator locator) {

                }

                public void startDocument() throws SAXException {
                }

                public void endDocument() throws SAXException {
                }

                public void startPrefixMapping(String prefix, String uri) throws SAXException {
                }

                public void endPrefixMapping(String prefix) throws SAXException {
                }

                public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
                    System.out.println("Start element "+uri+" "+ localName+" "+qName+" "+atts);
                }

                public void endElement(String uri, String localName, String qName) throws SAXException {
                    System.out.println("end element "+uri+" "+ localName+" "+qName);
                }

                public void characters(char[] ch, int start, int length) throws SAXException {
                    System.out.println("characters "+String.valueOf(ch)+" "+start+" "+length);
                }

                public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
                    System.out.println("Ignorable whitespace "+String.valueOf(ch)+" "+start+" "+length);
                }

                public void processingInstruction(String target, String data) throws SAXException {
                    System.out.println("PRocessing instructions "+target+" "+data);
                }

                public void skippedEntity(String name) throws SAXException {
                }
            });
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();
            InputStream instream = new FileInputStream(inFile);
            try {
                parser.parse(instream, handler, metadata, context);
            } finally {
                instream.close();
            }
            WordprocessingMLPackage wordMLPackage =
                    WordprocessingMLPackage.load(new java.io.File(inFile));
            MainDocumentPart mainPart = wordMLPackage.getMainDocumentPart();
            for (Object part : mainPart.getContent()) {

                String line = part.toString();
                //Lines that start with * are usually not relevant
                if (!line.startsWith("*")) {
                    if (allCaps.matcher(line).matches()) {
                        currentCategory = line;

                    } else if (line == null || "".equals(line)) {
                        //end of line  Check to see if we have a price and then finish processing
                        currentEntry = new ProductEntry();

                    } else {
                        int colonIndex = line.indexOf(":");
                        if (colonIndex > 0) {
                            String product = line.substring(0, colonIndex);
                            currentEntry.setProductDescription(product);
                        }
                        Matcher dollarPerMatcher = pricePer.matcher(line);
                        Matcher perDollarMatcher = perPrice.matcher(line);
                        boolean priceFound = false;
                        if (dollarPerMatcher.find()) {
                            priceFound = true;
                            currentPriceString = dollarPerMatcher.group(1);
                            int slashIndex = currentPriceString.indexOf("/");
                            currentEntry.setWholesalePrice(currentPriceString.substring(0, slashIndex));
                            currentEntry.setSize(currentPriceString.substring(slashIndex + 1));

                        } else if (perDollarMatcher.find()) {
                            priceFound = true;
                            currentPriceString = perDollarMatcher.group(1);
                            int slashIndex = currentPriceString.indexOf("/");
                            currentEntry.setSize(currentPriceString.substring(0, slashIndex));
                            currentEntry.setWholesalePrice(currentPriceString.substring(slashIndex + 1));
                        }
                        if (priceFound) {
                            addEntry(currentEntry, currentCategory);
                            entries.add(currentEntry);
                            currentEntry = new ProductEntry();
                        }
                        System.out.println(line);
                    }
                }
            }
            new CsvProductOutput(outFile, new Zone7Markup()).writeOutCsvFile(entries);
        } catch (Exception e) {
            throw new RuntimeException("Exception occurred parsing file " + inFile, e);
        } finally {
            if (doc != null) {
                try {
                    doc.close();
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }


    }

    private void addEntry(ProductEntry currentEntry, String currentCategory) {
        final String productDescription = currentEntry.getProductDescription();
        if (productDescription != null) {
            currentEntry.setProductDescription(productDescription.trim());
        }
        //Use the end of the productDescription as the subcategory, e.g.
        //Fried green tomatoes -> tomatoes
        if (currentEntry.getProductDescription() != null) {
            String[] descWords = currentEntry.getProductDescription().split(" ");
            String lastWord = descWords[descWords.length - 1];
            if (lastWord.startsWith("(")) {
                //For Stuff that ends with '(OG)', look at the previous word
                lastWord = descWords[descWords.length - 2];
            }
            currentEntry.setSubcategory(lastWord);
        }
        currentEntry.setWholesalePrice(currentEntry.getWholesalePrice().trim());
        currentEntry.setSize(currentEntry.getSize().trim());
        currentEntry.setCategory(currentCategory);
    }


}
