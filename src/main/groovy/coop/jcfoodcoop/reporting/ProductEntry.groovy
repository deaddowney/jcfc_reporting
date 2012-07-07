package coop.jcfoodcoop.reporting

/**
 * @author akrieg
 */
class ProductEntry {

    int id

    /** e.g. Produce, Dairy, Meat, etc. */
    String category

    /** subcategory as first parsed from the file and before manual fudging */
    String rawSubCategory

    /** e.g. Cheese, Eggs, Apples, etc.*/
    String subcategory

    /**
     * Cheddar, Smoked Goat: $10.50 per lb Misty Creek Dairy - Price Finalized at Shipping
     * => "Misty Creek Dairy"
     */
    String manufacturer

    /**
     * Cheddar, Smoked Goat: $10.50 per lb Misty Creek Dairy - Price Finalized at Shipping
     * => "Cheddar, Smoked Goat"
     */
    String productDescription

    /**
     * Description of how the item comes, e.g. 12 (8 oz size) per case
     */
    String size

    /**
     * The un-marked up price for the item
     */
    String wholesalePrice

    boolean limitedSupply = false

    boolean onSale = false

    boolean priceFinal = true

    boolean preorder = false

    boolean organic = true
    boolean loose = false
    boolean retail = false
    /**
     * Lancaster puts a lot of crap in the description that is redundant:
     * e.g.:
     * Bison, Brisket: $11.99 per lb CL Bison - Price Finalized at Shipping
     * We want to bust out "Bison, Brisket" as the product type
     * and "CL Bison" as producer
     */
    String rawDescription

    public static final Set<String> units = [
            "lb",
            "lbs",
            "lb.",
            "lbs",
            "oz",
            "pack",
            "gal",
            "bar",

    ]

    String salesPrice

    @Override
    public String toString() {
        return "ProductEntry{" +
                "id=" + id +
                ", category='" + category + '\'' +
                ", subcategory='" + subcategory + '\'' +
                ", manufacturer='" + manufacturer + '\'' +
                ", productDescription='" + productDescription + '\'' +
                ", size='" + size + '\'' +
                ", wholesalePrice='" + wholesalePrice + '\'' +
                ", preorder=" + preorder +
                '}';
    }
}
