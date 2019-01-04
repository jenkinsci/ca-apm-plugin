package com.ca.apm.jenkins.core.util;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Utility for date-time conversions, internally used by the Plug-in
 * @author Avinash Chandwani
 *
 */
public class JenkinsPluginUtility {
	
	private JenkinsPluginUtility(){
		super();
	}

	public static String getGMTTime(long time) {
		Date now = new Date(time);
		DateFormat converter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		converter.setTimeZone(TimeZone.getTimeZone("GMT"));
		return converter.format(now);
	}

    public static long[] getEMTimeinMillis(long startTime, long endTime, String emTimeZone)
        throws ParseException {
        long[] emTimeinMillis = new long[2];
        Date startDate = new Date(startTime);
        Date endDate = new Date(endTime);
        DateFormat converter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        converter.setTimeZone(TimeZone.getTimeZone(emTimeZone));
        String emStartDate = converter.format(startDate);
        String emEndDate = converter.format(endDate);
        DateFormat apmnMSConverter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        long apmStartTimeinMillis = apmnMSConverter.parse(emStartDate).getTime();
        long apmEndTimeinMillis = apmnMSConverter.parse(emEndDate).getTime();
        emTimeinMillis[0] = apmStartTimeinMillis;
        emTimeinMillis[1] = apmEndTimeinMillis;
        return emTimeinMillis;

    }

    public static String[] getEMTimeinDateFormat(long startTime, long endTime, String emTimeZone)
        throws ParseException {
        String[] emTimeinDateFormat = new String[2];
        Date startDate = new Date(startTime);
        Date endDate = new Date(endTime);
        DateFormat converter = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss z");
        converter.setTimeZone(TimeZone.getTimeZone(emTimeZone));
        String emStartDate = converter.format(startDate);
        String emEndDate = converter.format(endDate);
        emTimeinDateFormat[0] = emStartDate;
        emTimeinDateFormat[1] = emEndDate;
        return emTimeinDateFormat;

    }

    public static long getLongTimeValue(String dateStr) {
        long time = 0;
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date date = inputDateFormat.parse(dateStr);
            time = date.getTime();
        } catch (ParseException e) {
            time = 0;
        }
        return time;
    }

	public static long getDuration(String startDateStr, String endDateStr) {
		SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date startDate;
		Date endDate;
		long difference = 0L;
		try {
			startDate = inputDateFormat.parse(startDateStr);
			endDate = inputDateFormat.parse(endDateStr);
			difference = endDate.getTime() - startDate.getTime();
		} catch (ParseException e) {
		}
		return difference;
	}

	public static double getDoubleFormattedToTwoDecimalPlaces(double inputValue) {
		DecimalFormat format = new DecimalFormat("##.00");
		format.format(inputValue);
		inputValue = Double.valueOf(format.format(inputValue));
		return inputValue;
	}

	public static String getDurationInString(long startTime, long endTime) {
		long duration = endTime - startTime;
		long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(duration);
		long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(duration);
		long diffInHours = TimeUnit.MILLISECONDS.toHours(duration);
		long diffInDays = TimeUnit.MILLISECONDS.toDays(duration);
		if (diffInSeconds < 60)
			return diffInSeconds + " seconds";
		if (diffInSeconds >= 60)
			return diffInMinutes + " minutes";
		else if (diffInMinutes >= 60)
			return diffInHours + " hours";
		else if (diffInHours >= 24)
			return diffInDays + " day(s)";
		else
			return null;
	}

	public static String getDateFromMilliTimeStamp(long time) {
		Date date = new Date(time);
		SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return inputDateFormat.format(date);
	}

	public static String getGMTTimeOfInput(String dateStr) {
		String outputDate = null;
		SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat outputDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		TimeZone currentTimeZone = TimeZone.getDefault();
		inputDateFormat.setTimeZone(currentTimeZone);
		try {
			Date d = inputDateFormat.parse(dateStr);
			outputDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			outputDate = outputDateFormat.format(d);
		} catch (ParseException e) {
			outputDate = null;
		}
		return outputDate;
	}
}