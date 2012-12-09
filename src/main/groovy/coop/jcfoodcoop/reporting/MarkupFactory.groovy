package coop.jcfoodcoop.reporting

/**
 * Given an item, determine the markup
 * @author akrieg
 */
public interface MarkupFactory {

    public Double getMarkup(ProductEntry entry);

    /**
     *
     * @param entry
     * @return retail price, or null, if not for sale
     */
    Double getRetailPrice(ProductEntry entry)
}