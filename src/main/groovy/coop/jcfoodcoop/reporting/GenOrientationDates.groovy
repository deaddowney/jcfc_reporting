package coop.jcfoodcoop.reporting

import java.text.SimpleDateFormat

/**
 * This script generates sql insert statements for populating the database table jcfc_preshop_dates.
 * It creates 3 columns:
 * order_start :  the starting day to order
 * order_stop :   the last day to order (two days after the start)
 * week_one_pickup :   the pickup day the first week (Wednesday)
 * week_two_pickup :   the pickup day the second week (Tuesday)
 * @author akrieg
 */

SimpleDateFormat df = new SimpleDateFormat('yyyy-MM-dd')
String queryString = "insert into jcfc_event_dates (type, event_date, location, duration_mins, description) values "
Date startDate = df.parse('2012-05-08')
StringBuilder buf = new StringBuilder(queryString);
for (int i = 1; i < 401; i++) {
    buf.append("('orientation', ").
        append("\'${df.format(startDate)}\', ").
        append("'Community Center @ St. Paul Lutheran', ").
        append("60, ").
        append("'Co-op Orientation.  At <a href=\"http://jcfoodcoop.coop/maps-directions\">St. Pauls</a>'")
    println(buf.append(');').toString())
    buf = new StringBuilder(queryString)
    startDate = startDate.plus(28)

}
