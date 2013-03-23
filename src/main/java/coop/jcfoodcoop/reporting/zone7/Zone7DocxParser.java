package coop.jcfoodcoop.reporting.zone7;

import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;

import coop.jcfoodcoop.reporting.CsvProductOutput;
import coop.jcfoodcoop.reporting.ProductEntry;
import coop.jcfoodcoop.reporting.Zone7Markup;


/**
 * @author akrieg
 */
public class Zone7DocxParser {

    public static final Pattern ALL_CAPS_PATTERN = Pattern.compile("(?:[A-Z]+\\s?&?\\s?)+\\(?[a-z\\s,&]*\\)?");
    public static final Pattern PRICE_EXPRESSION_PATTERN = Pattern.compile("\\$\\d+\\.?\\d*");
    /** Identifies a $XX.XX / foo pattern */
    public static final Pattern PRICE_PER_PATTERN = Pattern.compile('(' + PRICE_EXPRESSION_PATTERN.pattern() + " / [^\\.,~\\(]*)");

    /** Identifies a foo / $XX.XX pattern **/
    public static final Pattern PER_PRICE_PATTERN = Pattern.compile(": ([^/]* / " + PRICE_EXPRESSION_PATTERN.pattern() + ")");

    public void parse(String inFile, String outFile) {

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
