package coop.jcfoodcoop.productupdate

object ProductEntry {

    /**
     * The Units of Size
     */
    val units: Set[String] = Set("lb",
        "lbs",
        "lb.",
        "lbs",
        "oz",
        "pack",
        "gal",
        "bar")
}

/**
 * Parsed Entry representing a Product
 *                category=category,
                 subCategory = subcategory,
                 rawDescription = rawDescription,
                 wholesalePrice =  price,
                 size = size
 * @author akrieg
 */
class ProductEntry(
                    var category: String,

                    /** subcategory as first parsed from the file and before manual fudging */
                    var subCategory: String,
                    /**
                     * Lancaster puts a lot of crap in the description that is redundant:
                     * e.g.:
                     * Bison, Brisket: $11.99 per lb CL Bison - Price Finalized at Shipping
                     * We want to bust out "Bison, Brisket" as the product type
                     * and "CL Bison" as producer
                     */
                    rawDescription: String,
                    val wholesalePrice: String, //The un-marked price for the item
                    val size: String //Description of how the item comes, e.g. 12 (8 oz size) per case
                       )  {

    /** e.g. Cheese, Eggs, Apples, etc. */
    var manufacturer: String = ""

    /**
     * Cheddar, Smoked Goat: $10.50 per lb Misty Creek Dairy - Price Finalized at Shipping
     * -> "Cheddar, Smoked Goat"
     */
    var productDescription: String = null//Description of how the item comes, e.g. 12 (8 oz size) per case
    var limitedSupply: Boolean = false
    var onSale: Boolean = false
    var priceFinal: Boolean = true  //Things like Meat are not priced until delivery, because we don't know how big a cut we're going to get
    var preorder: Boolean = false
    var organic: Boolean = false
    var loose: Boolean = false
    var retail: Boolean = true
    var salesPrice = ""

    override def toString: String = {
        "ProductEntry{" +
            "category='" + category + '\'' +
            ", subcategory='" + subCategory + '\'' +
            ", manufacturer='" + manufacturer + '\'' +
            ", productDescription='" + productDescription + '\'' +
            ", size='" + size + '\'' +
            ", wholesalePrice='" + wholesalePrice + '\'' +
            ", preorder=" + preorder +
            '}'
    }
}
