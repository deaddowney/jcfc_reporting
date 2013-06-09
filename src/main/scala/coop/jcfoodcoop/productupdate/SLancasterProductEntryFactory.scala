package coop.jcfoodcoop.productupdate

/**
 * Scala Version of ProductEntryFactory
 * that creates LancasterProductEntries
 *
 * @author akrieg
 */
object SLancasterProductEntryFactory {

    /**
     * We need to strip this out. Lancaster sometimes ends a product desc with this
     */
    val FINALIZED_PRICE="price finalized "
    val SALE_PATTERN = """\$\d+.\d\d (?:per|/)""".r

    val NOT_PKG_RETAIL = "not packaged for retail"

    val LOOSE_PACKAGING_TRAILER = "note that packaging may be loose"

    //Local IPM - Non-Organic
    val NON_ORGANIC = """-?\\s?Non\\s?-?Organic""".r

    val CERT_ORGANIC_NO_LABEL = "cert. organic/no label"

    val NOT_LABELED = "not label"

    /**
     * Lancaster sometimes marks items with this at the end (after FINALIZED_PRICE)
     * to indicate limited supply
     */
    val LIMITED_SUPP_TRAIL = "Limited Supply"

    /**
     * Lancaster sometimes marks sale items with this tag, e.g.:
     * **SALE** Pork, Fat (unrendered): $1.00 per lb Sweet Stem
     * - Not Packaged for Retail - Price Finalized at Shipping
     */
    val SALE_HEADER = "**SALE** "

    /**
     * Lancaster sometimes marks preorder items with this tag, e.g.:
     * Chocolate Milk - Glass: Trickling Springs Creamery
     * (pre-order by Monday for Friday/Saturday delivery)
     *
     */
    val PRE_ORDER_FLAG = "(pre-order"

    def fromLine(category: String,
                 subcategory: String,
                 rawDescription:String,
                 price:String,
                 size:String) : SProductEntry = {




        val  entry = new SProductEntry(0,
                category=category,
                subCategory = subcategory,
                rawDescription = rawDescription,
                wholesalePrice =  price,
                size = size)

        val catSubCat = parseSubCategoryFromCategory(category, subcategory)
        entry.category = catSubCat._1
        entry.subCategory = catSubCat._2
        var workingDesc = rawDescription
        if (workingDesc.toUpperCase.startsWith(SALE_HEADER)) {
            entry.onSale = true
            //Strip out the leading SALE_HEADER text so that what
            // remains before the : is just the product type
            workingDesc = workingDesc.substring(SALE_HEADER.length())
        }

        val indexOfPreOrder = workingDesc.indexOf(PRE_ORDER_FLAG)
        if (indexOfPreOrder > 0){
            entry.preorder = true
            workingDesc = workingDesc.subSequence(0, indexOfPreOrder).toString
        }

        //Look for Non organic marker
        //This marker can contain - characters which is why we do it outside
        val nonOrganicMarker = NON_ORGANIC findFirstMatchIn (workingDesc)

        if (nonOrganicMarker.isDefined) {
            entry.organic = false
            val nonOrganicMatcher = nonOrganicMarker.get
            workingDesc = workingDesc.substring(0, nonOrganicMatcher.start).trim()+
                workingDesc.substring(nonOrganicMatcher.end).trim()
        }


        workingDesc.split(":").toList match {
            case (productDesc)::Nil => entry.productDescription = productDesc.trim
            case (productDesc)::(otherStuff:String)::xs => {
                entry.productDescription = productDesc.trim
                val desc = new CompoundDescription(otherStuff.trim)
                val indexOfLimited = desc.lastSection.indexOf(LIMITED_SUPP_TRAIL)
                if (indexOfLimited >= 0) {
                    entry.limitedSupply = true
                    desc.trimOffLastSection(indexOfLimited)
                }

                val indexOfLoose = desc.lastSection.toLowerCase.indexOf(LOOSE_PACKAGING_TRAILER)
                if (indexOfLoose >= 0) {
                    entry.loose = true
                    desc.trimOffLastSection(indexOfLoose)
                }

                val indexOfFinalized = desc.lastSection.toLowerCase.indexOf(FINALIZED_PRICE)
                if (indexOfFinalized >= 0) {
                    entry.priceFinal = false
                    desc.trimOffLastSection(indexOfFinalized)
                }

                val indexOfNotPkgRetail = desc.lastSection.toLowerCase.indexOf(NOT_PKG_RETAIL)
                if (indexOfNotPkgRetail >= 0) {
                    entry.retail = false
                    desc.trimOffLastSection(indexOfNotPkgRetail)

                }
                //Don't really do anything with this info, just get it out of the way
                val idxCrtOrganic = desc.lastSection.toLowerCase.indexOf(CERT_ORGANIC_NO_LABEL)
                if (idxCrtOrganic >= 0) {
                    desc.trimOffLastSection(idxCrtOrganic)

                }

                val idxNotLabeled = desc.lastSection.toLowerCase.indexOf(NOT_LABELED)
                if (idxNotLabeled >= 0) {
                    entry.organic = false
                    desc.trimOffLastSection(idxNotLabeled)

                }
            //What we have left can work with is usually either just the manufacturer
            //or a sales price and the manufacturer
            //
            val descManufacturer = desc.computeManufacturer()
            val manLC = descManufacturer.toLowerCase
            // Check if we have a per or a /, that's an indication that there's a sale price
            if (manLC.contains("per") || manLC.contains("/")) {

                def mkManufacturer() :String = {
                    val words = descManufacturer.split(" ")
                    //Walk back from the end of the string and look to see if any of the words
                    // are units.  If so, stop there and that's the name of the manufacturer
                    //Everything before is part of the sale
                    val manComponents = collection.mutable.ListBuffer[String]()
                    for(i<- words.length-1 to 0 -1) {
                        val word = words(i)
                        if (SProductEntry.units.contains(word.toLowerCase)) {
                            return manComponents.mkString(" ")
                        }
                        manComponents.prepend(word)
                    }
                    manComponents.mkString(" ")
                }

                entry.manufacturer = mkManufacturer()
                if (descManufacturer.length() > entry.manufacturer.length()) {
                    entry.salesPrice = descManufacturer.substring(0, descManufacturer.indexOf(entry.manufacturer))
                }

            } else {
                entry.manufacturer = descManufacturer
            }
            def saleMatcher = SALE_PATTERN.findFirstMatchIn(entry.manufacturer)
            if (saleMatcher.isDefined) {
                entry.manufacturer = entry.manufacturer.substring(saleMatcher.get.end)
            }



            }
        }

        entry


    }

    /**
     * Separates Category from SubCategory if they are there
     * @param cat category
     * @param subcat subcategory
     * @return (category, subCategory)
     */
    def parseSubCategoryFromCategory(cat:String, subcat:String) : (String, String) = {
        if (subcat == null) {
            //Category is usually of the form Produce:Fruit
            val catColonIndex = cat.indexOf(":")
            if (catColonIndex > 0 && catColonIndex < cat.length()) {
                return (cat.substring(0, catColonIndex),  cat.substring(catColonIndex + 1))
            } else {
                return (cat, cat) //Fuck it, just make sub category the same as the category
            }
        }
        (cat, subcat)
    }

}

/**
 * Manages state of Description field
 */
class CompoundDescription(desc:String) {
    val workingDescription :String = desc
    //Split the rest of the description into sections separated by "-"
    val sections: Array[String] = workingDescription.split("-")
    var lastIndex:Int = sections.length -1
    var lastSection :String = sections(lastIndex)

    def trimOffLastSection(trimOffAfter: Int) {
        lastSection = lastSection.substring(0, trimOffAfter).trim()
        if (lastSection.isEmpty) {
            lastIndex-=1; //Don't consider the last section anymore
            lastSection = sections(math.max(0, lastIndex))
        }
        lastSection
    }

    def computeManufacturer() :String = {
        if (lastIndex > 0) {
            //Re-assemble manufacturers that have been split
            //e.g. Whole Milk - Non-Homogenized would get split
            val manBuilder = new StringBuilder()
            for (i <- 0 until lastIndex) {
                manBuilder.append(sections(i))
                if (i < lastIndex -1) {
                    manBuilder.append("- ")
                }
            }
            manBuilder.append("- ").append(lastSection).toString()
        } else {
            lastSection
        }

    }


}