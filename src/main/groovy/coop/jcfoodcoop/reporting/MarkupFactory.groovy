package coop.jcfoodcoop.reporting

/**
 * Given an item, determine the markup
 * @author akrieg
 */
public interface MarkupFactory {

    public Double getMarkup(ProductEntry entry);
}