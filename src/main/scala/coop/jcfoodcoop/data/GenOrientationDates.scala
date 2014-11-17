package coop.jcfoodcoop.data

import org.joda.time.format.DateTimeFormat
import org.scala_tools.time.Imports._

/**
 * @author akrieg
 */
object GenOrientationDates extends App {

    val queryString = "insert into jcfc_event_dates (type, event_date, location, duration_mins, description) values "
    val df = DateTimeFormat.forPattern("yyyy-MM-dd")
    var startDate = df.parseDateTime("2014-11-19")
    var buf = new StringBuilder(queryString)
    for (i <- 1 to 400) {
        buf.append("('orientation', ").
            append("\'"+df.print(startDate)+"\', ").
            append("'Community Center @ St. Paul Lutheran', ").
            append("60, ").
            append("'Co-op Orientation.  At <a href=\"http://jcfoodcoop.coop/maps-directions\">St. Pauls</a>'")
        println(buf.append(");").toString())
        buf = new StringBuilder(queryString)
        startDate = startDate+28.days

    }

}
