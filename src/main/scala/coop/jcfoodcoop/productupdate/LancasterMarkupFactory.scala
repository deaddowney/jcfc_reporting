package coop.jcfoodcoop.productupdate

/**
 * @author akrieg
 */
object LancasterMarkupFactory extends MarkupFactory {


    /** Produce is 15% Markup **/
    val PRODUCE = 1.15
    /** Grocery is 20% Markup **/
    val GROCERY = 1.2
    /** Non Food is 30% Markup **/
    val NON_FOOD = 1.3

    val MARKUPS = Map(
            "produce" -> PRODUCE,
            "crafts" -> NON_FOOD)

    def getMarkup(entry:ProductEntry) :Double = {
        val category = entry.category
        MARKUPS.get(category.toLowerCase()).getOrElse(GROCERY)
    }


}
