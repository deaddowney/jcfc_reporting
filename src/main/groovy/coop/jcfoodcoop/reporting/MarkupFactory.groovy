package coop.jcfoodcoop.reporting

import coop.jcfoodcoop.productupdate.SProductEntry

/**
 * Given an item, determine the markup
 * @author akrieg
 */
public interface MarkupFactory {

    public Double getMarkup(ProductEntry entry);

    public Double getMarkup(SProductEntry entry);

    /**
     *
     * @param entry
     * @return retail price, or null, if not for sale
     */
    Double getRetailPrice(SProductEntry entry)

    Double getRetailPrice(ProductEntry entry)

}