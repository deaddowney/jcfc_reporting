package coop.jcfoodcoop.data

import org.joda.time.format.DateTimeFormat
import org.scala_tools.time.Imports._

/**
 * This script generates sql insert statements for populating the database table jcfc_preshop_dates.
 * It creates 3 columns:
 * order_start :  the starting day to order
 * order_stop :   the last day to order (two days after the start)
 * week_one_pickup :   the pickup day the first week (Wednesday)
 * week_two_pickup :   the pickup day the second week (Tuesday)
 * @author akrieg
 */
object GenPreshopDates extends App {


    val queryString = "insert into jcfc_preshop_dates (order_start, order_end, week_one_pickup, week_two_pickup) values "
    val df = DateTimeFormat.forPattern("yyyy-MM-dd")
    var startDate = df.parseDateTime("2011-11-26")
    var buf = new StringBuilder(queryString)
    for (i <- 1 to 400) {
        buf.append("('" + df.print(startDate) + "', ").
            append("'" + df.print(startDate + 2.days) + "', ").
            append("'" + df.print(startDate + 4.days) + "', ").
            append("'" + df.print(startDate + 10.days) + "'")
        println(buf.append(");").toString())
        buf = new StringBuilder(queryString)
        startDate = startDate.plus(14)
    }

}
