package coop.jcfoodcoop.reporting

import java.util.regex.Pattern

/**
 * @author akrieg
 */
public class LancasterProductEntryFactory {

    /**
     * We need to strip this out. Lancaster sometimes ends a product desc with this
     */
    private static final String FINALIZED_PRICE="price finalized "

    private static final Pattern SALE_PATTERN = ~/\$\d+.\d\d (?:per|\/)/

    private static final String NOT_PKG_RETAIL = "not packaged for retail";

    private static final String LOOSE_PACKAGING_TRAILER = "note that packaging may be loose"

    //Local IPM - Non-Organic
    private static final Pattern NON_ORGANIC = Pattern.compile("-?\\s?Non\\s?-?Organic", Pattern.CASE_INSENSITIVE)

    private static final String CERT_ORGANIC_NO_LABEL = "cert. organic/no label";

    private static final String NOT_LABELED = "not label"

    /**
     * Lancaster sometimes marks items with this at the end (after FINALIZED_PRICE)
     * to indicate limited supply
     */
    private static final String LIMITED_SUPP_TRAIL = "Limited Supply"

    /**
     * Lancaster sometimes marks sale items with this tag, e.g.:
     * **SALE** Pork, Fat (unrendered): $1.00 per lb Sweet Stem
     * - Not Packaged for Retail - Price Finalized at Shipping
     */
    private static final String SALE_HEADER = "**SALE** "

    /**
     * Lancaster sometimes marks preorder items with this tag, e.g.:
     * Chocolate Milk - Glass: Trickling Springs Creamery
     * (pre-order by Monday for Friday/Saturday delivery)
     *
     */
    private static final String PRE_ORDER_FLAG = "(pre-order"

    public static ProductEntry fromLine(String category, String subcategory,
                                        String rawDescription, String price, String size) {
        ProductEntry entry = new ProductEntry(
                category:category,
                subcategory: subcategory,
                rawDescription: rawDescription,
                wholesalePrice: price,
                size: size)

        parseSubCategoryFromCategory(entry)
        String workingDesc = rawDescription
        entry.salesPrice = ""
        if (workingDesc.toUpperCase().startsWith(SALE_HEADER)) {
            entry.onSale = true
            //Strip out the leading SALE_HEADER text so that what
            // remains before the : is just the product type
            workingDesc = workingDesc.substring(SALE_HEADER.length())
        }

        int indexOfPreOrder = workingDesc.indexOf(PRE_ORDER_FLAG)
        if (indexOfPreOrder > 0){
            entry.preorder = true
            workingDesc = workingDesc.subSequence(0, indexOfPreOrder)
        }

        //Look for Non organic marker
        //This marker can contain - characters which is why we do it outside
        def matcher = NON_ORGANIC.matcher(workingDesc)
        if (matcher.find()) {
            entry.organic = false
            workingDesc = workingDesc.substring(0, matcher.start()).trim()+workingDesc.substring(matcher.end()).trim()
        }




        String[] parts = workingDesc.split(":")
        entry.productDescription = parts[0].trim()
        if (parts.length > 1) {
            //After the colon we need to ease out the manufacturer and a bunch
            // of other flags, e.g. unfinalized price,

            workingDesc = parts[1].trim()
            CompoundDescription desc = new CompoundDescription(workingDesc)


            int indexOfLimited = desc.lastSection.indexOf(LIMITED_SUPP_TRAIL)
            if (indexOfLimited >= 0) {
                entry.limitedSupply = true
                desc.trimOffLastSection(indexOfLimited)
            }

            int indexOfLoose = desc.lastSection.toLowerCase().indexOf(LOOSE_PACKAGING_TRAILER)
            if (indexOfLoose >= 0) {
                entry.loose = true
                desc.trimOffLastSection(indexOfLoose)
            }

            int indexOfFinalized = desc.lastSection.toLowerCase().indexOf(FINALIZED_PRICE)
            if (indexOfFinalized >= 0) {
                entry.priceFinal = false
                desc.trimOffLastSection(indexOfFinalized)
            }

            int indexOfNotPkgRetail = desc.lastSection.toLowerCase().indexOf(NOT_PKG_RETAIL)
            if (indexOfNotPkgRetail >= 0) {
                entry.retail = false
                desc.trimOffLastSection(indexOfNotPkgRetail)

            }
            //Don't really do anything with this info, just get it out of the way
            int idxCrtOrganic = desc.lastSection.toLowerCase().indexOf(CERT_ORGANIC_NO_LABEL)
            if (idxCrtOrganic >= 0) {
                desc.trimOffLastSection(idxCrtOrganic)

            }

            int idxNotLabeled = desc.lastSection.toLowerCase().indexOf(NOT_LABELED)
            if (idxNotLabeled >= 0) {
                entry.organic = false
                desc.trimOffLastSection(idxNotLabeled)

            }

            //What we have left can work with is usually either just the manufacturer
            //or a sales price and the manufacturer
            //
            String descManufacturer = desc.computeManufacturer()
            String manLC = descManufacturer.toLowerCase()
            // Check if we have a per or a /, that's an indication that there's a sale price
            if (manLC.contains("per") || manLC.contains("/")) {
                String[] words = descManufacturer.split(" ")
                //Walk back from the end of the string and look to see if any of the words
                // are units.  If so, stop there and that's the name of the manufacturer
                //Everything before is part of the sale
                List<String> manComponents = new ArrayList<String>();
                for(int i = words.length-1; i >=0; i--) {
                    String word = words[i]
                    if (entry.units.contains(word.toLowerCase())) {
                        break;
                    }
                    manComponents.add(0, word)
                }


                entry.manufacturer = manComponents.join(" ")
                if (descManufacturer.length() > entry.manufacturer.length()) {
                    entry.salesPrice = descManufacturer.substring(0, descManufacturer.indexOf(entry.manufacturer))
                }

            } else {
                entry.manufacturer = descManufacturer
            }
            def saleMatcher = SALE_PATTERN.matcher(entry.manufacturer)
            if (saleMatcher.find()) {
                entry.manufacturer = entry.manufacturer.substring(saleMatcher.end())
            }

        }

        return entry
    }

    /**
     * Separates Category from SubCategory if they are there
     * @param entry
     * @return
     */
    static def parseSubCategoryFromCategory(ProductEntry entry) {
        if (entry.subcategory == null) {
            //Category is usually of the form Produce:Fruit
            def catColonIndex = entry.category.indexOf(":")
            String origCategory = entry.category;
            if (catColonIndex > 0 && catColonIndex < entry.category.length()) {
                entry.category = origCategory.substring(0, catColonIndex)
                entry.subcategory = origCategory.substring(catColonIndex + 1)
            }
        }
    }

    /**
     * Manages state of Description field
     */
    private static class CompoundDescription {
        String workingDescription
        //Split the rest of the description into sections separated by "-"
        String[] sections
        String lastSection
        int lastIndex

        CompoundDescription(String desc) {
            workingDescription = desc
            sections = workingDescription.split("-")
            //Start with the last section and work backwards
            lastIndex = sections.length - 1
            lastSection = sections[lastIndex].trim()
        }

        public String trimOffLastSection(int trimOffAfter) {
            lastSection = lastSection.substring(0, trimOffAfter).trim()
            if (lastSection.isEmpty()) {
                lastIndex --; //Don't consider the last section anymore
                lastSection = sections[Math.max(0, lastIndex)]
            }
            return lastSection
        }

        public String computeManufacturer() {
            String manufacturer
            if (lastIndex > 0) {
                //Re-assemble manufacturers that have been split
                //e.g. Whole Milk - Non-Homogenized would get split
                StringBuilder manBuilder = new StringBuilder();
                for(int i = 0; i < lastIndex; i++) {
                    manBuilder.append(sections[i])
                    if (i < lastIndex-1) {
                        manBuilder.append("- ")
                    }
                }
                manufacturer = manBuilder.append("- ").append(lastSection).toString()
            } else {
                manufacturer = lastSection
            }

            return manufacturer
        }


    }
}