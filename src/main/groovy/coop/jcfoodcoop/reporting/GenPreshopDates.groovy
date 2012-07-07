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
String queryString = "insert into jcfc_preshop_dates (order_start, order_end, week_one_pickup, week_two_pickup) values "
Date startDate = df.parse('2011-11-26')
StringBuilder buf = new StringBuilder(queryString);
for (int i = 1; i < 401; i++) {
    buf.append("('${df.format(startDate)}', ").
        append("'${df.format(startDate.plus(2))}', ").
        append("'${df.format(startDate.plus(4))}', ").
        append("'${df.format(startDate.plus(10))}'")
    println(buf.append(');').toString())
    buf = new StringBuilder(queryString)
    startDate = startDate.plus(14)

}
