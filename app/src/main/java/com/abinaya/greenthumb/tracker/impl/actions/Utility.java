package com.abinaya.greenthumb.tracker.impl.actions;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class Utility {
    public static int calcDaysFromTime(Calendar start, Calendar end)    {
        if (start == null || end == null)   {
            return -1;
        }

        Date startDate = start.getTime();
        Date endDate = end.getTime();
        long startTime = startDate.getTime();
        long endTime = endDate.getTime();
        long diffTime = endTime - startTime;
        long diffDays = diffTime / (1000 * 60 * 60 * 24);

        return (int)(diffDays + 1);    // shift to 1 based
    }

    public static int calcWeeksFromTime(Calendar start, Calendar end)   {
        if (start == null || end == null)   {
            return -1;
        }

        Date startDate = start.getTime();
        Date endDate = end.getTime();
        long startTime = startDate.getTime();
        long endTime = endDate.getTime();
        long diffTime = endTime - startTime;
        long diffDays = diffTime / (1000 * 60 * 60 * 24 * 7);

        return (int)(diffDays + 1);    // shift to 1 based
    }

    public static String getDateTimeFileString()    {
        Calendar c = Calendar.getInstance();
        String backupFileName = "backup_" + c.get(Calendar.HOUR_OF_DAY);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
        String formatted = dateFormat.format(c.getTime());

        return formatted;
    }

}
