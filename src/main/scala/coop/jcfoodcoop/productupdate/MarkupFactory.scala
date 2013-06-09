package coop.jcfoodcoop.productupdate

/**
 * Abstracts different markups for different vendors
 * @author akrieg
 */
trait MarkupFactory {

    /**
     * Looks up the markup for a product
     * @param entry the entry to lookup
     * @return the markup value
     */
    def getMarkup(entry: ProductEntry):Double


}
