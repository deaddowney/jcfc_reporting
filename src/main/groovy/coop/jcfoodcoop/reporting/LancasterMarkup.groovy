package coop.jcfoodcoop.reporting

/**
 * Looks at the product and pulls out a Markup
 * @author akrieg
 */
class LancasterMarkup implements MarkupFactory {

    /** Produce is 15% Markup **/
    private static final Double PRODUCE = 1.15;
    /** Grocery is 20% Markup **/
    private static final Double GROCERY = 1.2;
    /** Non Food is 30% Markup **/
    private static final Double NON_FOOD = 1.3;

    private static final Map<String, Double> MARKUPS = [
            "produce" : PRODUCE,
            "crafts" : NON_FOOD
    ]

    @Override
    Double getMarkup(ProductEntry entry) {
        String category = entry.category
        Double markup = MARKUPS.get(category.toLowerCase())
        if (markup == null) {
            markup = GROCERY
        }
        return markup
    }
}
