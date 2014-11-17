package coop.jcfoodcoop.data

import org.joda.time.Days
import org.joda.time.format.DateTimeFormat
import org.scala_tools.time.Imports._

/**
 * This script generates sql insert statements for populating the database table jcfc_preshop_dates.
 * It creates 3 columns:
 * order_start :  the starting day to order
 * order_stop :   the last day to order (two days after the start)
 * week_one_pickup :   the pickup day the first week (Wednesday)
 * week_two_pickup :   the pickup day the second week (Wednesday)
 * @author akrieg
 */
object GenPreshopDates extends App {


    val queryString = "insert into jcfc_preshop_dates (order_start, order_end, week_one_pickup, week_two_pickup) values "
    val df = DateTimeFormat.forPattern("yyyy-MM-dd")
    val startDate = df.parseDateTime("2014-11-08")
    var buf = new StringBuilder(queryString)
    for (i <- 0 to 400) {
        val beginWeek = startDate.plus(Days.days(i*14))
        buf.append("('" + df.print(beginWeek) + "', ").
            append("'" + df.print(beginWeek + 2.days) + "', ").
            append("'" + df.print(beginWeek + 4.days) + "', ").
            append("'" + df.print(beginWeek + 11.days) + "'")
        println(buf.append(");").toString())
        buf = new StringBuilder(queryString)
    }

}
