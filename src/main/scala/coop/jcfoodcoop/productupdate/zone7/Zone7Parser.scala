package coop.jcfoodcoop.productupdate.zone7

import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import scala.annotation.tailrec
import scala.Option
import scala.util.matching.Regex
import coop.jcfoodcoop.productupdate.ProductEntry

/**
 * @author akrieg
 */
object Zone7Parser {

    val priceRegex =  """\$(\d+\.?\d*)""".r

    type Line = Array[String]

    def readHeader(reader: CSVReader): ColumnHeader = {

        @tailrec
        def readHeaderRec(reader: CSVReader, curLine: Line): ColumnHeader = {
            if (curLine == null) {
                throw new IllegalArgumentException("No Header row found in file")
            }
            val productIndexes = for (index <- curLine.indices if curLine(index) == "Product / Description") yield index
            if (productIndexes.nonEmpty) {
                val productIndex = productIndexes.head
                new ColumnHeader(product = productIndex)
            } else {
                readHeaderRec(reader, reader.readNext())
            }

        }

        readHeaderRec(reader, reader.readNext())
    }

    abstract class LineType

    case object Blank extends LineType

    case class Category(name: String) extends LineType

    object Header extends LineType

    case class Item(name: String, farm: String, organic: String, price: String, notes: String) extends LineType


    type Lines = Stream[Line]

    abstract class StackState

    case class Building(state: List[String]) extends StackState {
        def this(desc:String) = {
            this(List(desc))
        }

        def merge(existingState: List[String]): List[String] = {
            if (state.length > existingState.length) {
                state
            } else {
                state ::: existingState.drop(state.size)
            }
        }

        def add(desc: String): Building = {
            new Building(desc :: state)
        }

    }

    case object Finished extends StackState

    class ProductTypeStack(state: StackState, descriptions: List[String]) {

        def getCategory ={
            descriptions(2)
        }

        def getSubCategory = {
            descriptions(0)
        }

        def addLine(line: LineType): (ProductTypeStack, Option[Item]) = {
            line match {
                case Blank => (this, Option.empty)
                case Header => (this, Option.empty)
                case (i: Item) => (new ProductTypeStack(Finished,
                    this.state match {
                        case (b: Building) => b.merge(this.descriptions)
                        case Finished => this.descriptions
                    }), Some(i))
                case Category(c) => this.state match {
                    case (b: Building) => (new ProductTypeStack(b.add(c), this.descriptions), Option.empty)
                    case Finished => (new ProductTypeStack(new Building(c), this.descriptions), Option.empty)
                }
            }
        }

    }


    def parse(file: String): Stream[ProductEntry] = {

        val reader = new CSVReader(new FileReader(file))

        val header = readHeader(reader)
        def isBlankRow(row: Line): Boolean = {
            row.isEmpty || row.size < header.farm || ("" == row(header.product))
        }

        def isCategoryRow(row: Line): Boolean = {
            !("" == row(header.product)) && ("" == row(header.price))
        }

        def isHeaderRow(row: Line): Boolean = {
            "Product / Description" == row(header.product)

        }

        def isItemRow(row: Line): Boolean = {
            !("" == row(header.price))
        }

        def convertToType(line: Line): LineType = {
            try {
                if (isBlankRow(line)) {
                    Blank
                } else if (isCategoryRow(line)) {
                    new Category(line(header.product))
                } else if (isHeaderRow(line)) {
                    Header
                } else if (isItemRow(line)) {
                    header.toItem(line)
                } else {
                    System.out.println("Warning, could not figure out what type of line " + line)
                    Blank
                }
            } catch {
                case e:Exception => throw new RuntimeException("Exception occurred processing line " + line.mkString(","))
            }
        }

        def streamRows: Lines = {
            val row = reader.readNext()
            if (row == null) {
                Stream.empty
            } else {
                row #:: streamRows
            }
        }

        def streamProducts(lines: Lines): Stream[(ProductTypeStack, Option[Item])] = {
            @tailrec
            def streamProductsRec(lines: Lines, product: Stream[(ProductTypeStack, Option[Item])]): Stream[(ProductTypeStack, Option[Item])] = {
                if (lines.isEmpty) {
                    product
                } else {
                    val head = lines.head

                    val myType = convertToType(head)
                    val productsAndItem = product.head
                    val pduct = productsAndItem._1
                    val thing = pduct.addLine(myType)
                    streamProductsRec(lines.tail, thing #:: product)
                }

            }

            streamProductsRec(lines, Stream((new ProductTypeStack(Finished, List()), Option.empty)))
        }

        def toProductEntry(item:Item, productStack:ProductTypeStack) :ProductEntry = {
            val p = new ProductEntry(
                productStack.getCategory,
                productStack.getSubCategory,
                item.name,
                item.price,
                ""
            )
            p.manufacturer = item.farm
            p.productDescription = item.name
            p
        }


         for (product <- streamProducts(streamRows) if product._2.isDefined) yield toProductEntry(product._2.get, product._1)


    }

    /**
     * Contains the indexes for
     * "",Product / Description,Farm,GP*,Price,Notes,""
     */
    class ColumnHeader(val product: Int) {

        val farm: Int = product + 1
        val label: Int = farm + 1
        val price: Int = label + 1
        val notes: Int = price + 1

        def parsePrice(prize: String): String = {
            val parsedPrize = priceRegex.findFirstMatchIn(prize)
            if (parsedPrize.isDefined) {
                parsedPrize.get.group(1)
            } else {
                throw new RuntimeException("Couldn't figure out price for "+prize)
            }

        }

        def toItem(line: Line): Item = {
            new Item(line(product),
                line(farm),
                line(label),
                parsePrice(line(price)),
                line(notes)
            )

        }
    }

}
