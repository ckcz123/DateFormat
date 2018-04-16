import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.Locale;

/**
 * <p>A class to format and parse date templates.</p>
 * <p>We define <b>placeholder</b> to represent time. <br /> Current available placeholders:
 * <ul>
 *     <li><b>{@code %Y}</b> 4-digit year. E.g. 2018</li>
 *     <li><b>{@code %y}</b> 2-digit year. E.g. 16 (Will be parsed as 2016)</li>
 *     <li><b>{@code %M}</b> 2-digit month (01~12). E.g. 04</li>
 *     <li><b>{@code %m}</b> 1-digit month (1~12). E.g. 4</li>
 *     <li><b>{@code %D}</b> 2-digit day (01~31). E.g. 08</li>
 *     <li><b>{@code %d}</b> 1-digit day (1~31). E.g. 8</li>
 *     <li><b>{@code %H}</b> 2-digit hour (00~23). E.g. 16</li>
 *     <li><b>{@code %h}</b> 1-digit hour (0~23). E.g. 5</li>
 *     <li><b>{@code %I}</b> 2-digit minute (00~59). E.g. 07</li>
 *     <li><b>{@code %i}</b> 1-digit minute (0~59). E.g. 7</li>
 *     <li><b>{@code %S}</b> 2-digit second (00~59). E.g. 09</li>
 *     <li><b>{@code %s}</b> 1-digit second (0~59). E.g. 9</li>
 *     <li><b>{@code %Z}</b> 3-digit millisecond (000~999). E.g. 342</li>
 *     <li><b>{@code %%}</b> The character '%'</li>
 * </ul>
 * </p>
 * <p>Some template examples:
 * <ul>
 *     <li>{@code aa/b/c-%Y%M%D/d_%H%I%S} will match {@code aa/b/c-20180214/d_220736}</li>
 *     <li>{@code hadoop/t2/%Y%M%D%H%I/ad} will match {@code hadoop/t2/201801190500/ad}</li>
 *     <li>{@code .../user/%Y/%m/%d/%h:%i:%s} will match {@code .../user/2016/3/14/5:13:22}</li>
 * </ul>
 * </p>
 * <p><b>Important: </b> For 1-digit placeholder, it will try to match two digits first, if failure then
 * fall back to 1-digit. For example, given {@code %Y%m%d} and {@code 2018115}, it will return
 * 2018-11-5 instead of 2018-1-15.</p>
 * <p>Created by ckcz123 on 4/12/18.</p>
 */
public class DateFormat {

    private char[] template;
    private Locale locale;

    public DateFormat(String template) {
        this(template, Locale.getDefault());
    }

    public DateFormat(String template, Locale locale) {
        this.template = template.toCharArray();
        this.locale = locale;
    }

    public void setTemplate(String template) {
        this.template = template.toCharArray();
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * Format a date to string.
     * @param date The date to format
     * @return the formatted string.
     */
    public String format(Date date) {
        StringBuilder builder = new StringBuilder();

        Calendar calendar = new GregorianCalendar(locale);
        calendar.setTime(date);

        for (int i=0;i<template.length;i++) {
            if (template[i]!='%') {
                builder.append(template[i]);
                continue;
            }
            i++;
            if (i==template.length) { // The last '%', add directly
                builder.append('%');
                continue;
            }
            switch (template[i]) {
                case 'Y': builder.append(calendar.get(Calendar.YEAR)); break;
                case 'y': builder.append(calendar.get(Calendar.YEAR)%100); break;
                case 'M': builder.append(formatDigit(calendar.get(Calendar.MONTH)+1, 2)); break;
                case 'm': builder.append(calendar.get(Calendar.MONTH)+1); break;
                case 'D': builder.append(formatDigit(calendar.get(Calendar.DAY_OF_MONTH), 2)); break;
                case 'd': builder.append(calendar.get(Calendar.DAY_OF_MONTH)); break;
                case 'H': builder.append(formatDigit(calendar.get(Calendar.HOUR_OF_DAY), 2)); break;
                case 'h': builder.append(calendar.get(Calendar.HOUR_OF_DAY)); break;
                case 'I': builder.append(formatDigit(calendar.get(Calendar.MINUTE), 2)); break;
                case 'i': builder.append(calendar.get(Calendar.MINUTE)); break;
                case 'S': builder.append(formatDigit(calendar.get(Calendar.SECOND), 2)); break;
                case 's': builder.append(calendar.get(Calendar.SECOND)); break;
                case 'Z': builder.append(calendar.get(Calendar.MILLISECOND)); break;
                case '%': builder.append('%'); break; // %%
                default: builder.append('%').append(template[i]); break;
            }

        }
        return builder.toString();
    }

    private String formatDigit(int value, int digit) {
        return String.format("%0"+digit+"d", value);
    }

    /**
     * Parse the string to a date.
     * @param string The string to parse
     * @return The parsed date. If the string cannot match the template, it will return null.
     */
    public Date parse(String string) {
        LinkedList<Pair> list = new LinkedList<>();
        if (!dfs(string.toCharArray(), 0, 0, list)) {
            return null;
        }
        Calendar calendar = new GregorianCalendar(locale);
        calendar.setTime(new Date(0));
        for (Pair pair: list) {
            calendar.set(pair.field, pair.value);
        }
        return calendar.getTime();
    }

    /**
     * A pair to record key-value
     */
    private class Pair {
        int field;
        int value;
        Pair(int _field, int _value) {
            field = _field;
            value = _value;
        }

    }

    /**
     * DFS to match the string with the template
     * @param chars The char array of the search string
     * @param curr The current search position in template
     * @param pos The current search position in string
     * @param list The part already searched; The key is the field (Calendar.MONTH, etc).
     * @return true if success; The list will record the result.
     */
    private boolean dfs(char[] chars, int curr, int pos, LinkedList<Pair> list) {
        while (curr<template.length && pos<chars.length) { // ignore all the non-usable characters
            if (template[curr] != '%') {
                if (template[curr] != chars[pos]) { // not match!
                    return false;
                }
                curr++;
                pos++;
            }
            else break;
        }
        if (curr==template.length && pos==chars.length) { // if matched!
            return true;
        }
        if (curr==template.length || pos==chars.length) {
            return false;
        }

        // Here, template[curr] == '%'
        curr++;
        if (curr == template.length) { // template end with a single '%'
            return chars[pos] == '%' && pos == chars.length-1;
        }
        switch (template[curr]) {
            case 'Y':
                return check(chars, curr, pos, 4, Calendar.YEAR, 1970, 9999, list, 0);
            case 'y':
                return check(chars, curr, pos, 2, Calendar.YEAR, 0, 99, list, 2000);
            case 'M':
                return check(chars, curr, pos, 2, Calendar.MONTH, 1, 12, list, -1);
            case 'm':
                return check(chars, curr, pos, 2, Calendar.MONTH, 1, 12, list, -1)
                        || check(chars, curr, pos, 1, Calendar.MONTH, 1, 12, list, -1);
            case 'D':
                return check(chars, curr, pos,2, Calendar.DAY_OF_MONTH, 1, 31, list, 0);
            case 'd':
                return check(chars, curr, pos,2, Calendar.DAY_OF_MONTH, 1, 31, list, 0)
                        || check(chars, curr, pos, 1, Calendar.DAY_OF_MONTH, 1, 31, list, 0);
            case 'H':
                return check(chars, curr, pos, 2, Calendar.HOUR_OF_DAY, 0, 23, list, 0);
            case 'h':
                return check(chars, curr, pos, 2, Calendar.HOUR_OF_DAY, 0, 23, list, 0)
                        ||  check(chars, curr, pos, 1, Calendar.HOUR_OF_DAY, 0, 23, list, 0);
            case 'I':
                return check(chars, curr, pos, 2, Calendar.MINUTE, 0, 59, list, 0);
            case 'i':
                return check(chars, curr, pos, 2, Calendar.MINUTE, 0, 59, list, 0)
                        || check(chars, curr, pos, 1, Calendar.MINUTE, 0, 59, list, 0);
            case 'S':
                return check(chars, curr, pos, 2, Calendar.SECOND, 0, 59, list, 0);
            case 's':
                return check(chars, curr, pos, 2, Calendar.SECOND, 0, 59, list, 0)
                        || check(chars, curr, pos, 1, Calendar.SECOND, 0, 59, list, 0);
            case 'Z':
                return check(chars, curr, pos, 3, Calendar.MILLISECOND, 0, 999, list, 0);
            case '%': return chars[pos] == '%' && dfs(chars, curr+1, pos+1, list);
            default: return chars[pos] == '%' && pos<chars.length-1 && chars[pos+1] == template[curr]
                    && dfs(chars, curr+1, pos+2, list);
        }
    }

    /**
     * Check if we can match the field in range [lower, upper]
     * @param chars The original string
     * @param curr The current search position in template
     * @param pos the current position in string
     * @param digit the number of digits to check
     * @param field The time field (Calendar.MONTH, etc)
     * @param lower The smallest valid value
     * @param upper The largest value
     * @param list The part already searched
     * @param extra The extra value added.
     * @return true if success.
     */
    private boolean check(char[] chars, int curr, int pos, int digit, int field, int lower,
                          int upper, LinkedList<Pair> list, int extra) {
        int val = parseInt(chars, pos, digit);
        if (val>=lower && val<=upper);
        list.offer(new Pair(field, val + extra));
        if (dfs(chars, curr+1, pos+digit, list)) {
            return true;
        }
        list.pollLast(); // remove the last one
        return false;
    }

    /**
     * Try to parse string to integer
     * @param chars The original string
     * @param pos The start position
     * @param digit The length
     * @return -1 if failure, or non-negative number if success
     */
    private int parseInt(char[] chars, int pos, int digit) {
        if (pos > chars.length - digit) {
            return -1;
        }
        try {
            int val = Integer.parseInt(String.valueOf(chars, pos, digit));
            return val>=0?val:-1;
        }
        catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Test if the string can match the template.
     * @param string The string to test
     * @return true if the string matches the template.
     */
    public boolean matches(String string) {
        return parse(string) != null;
    }

    /**
     * Return the next partition
     * @param curr The current partition
     * @param deltaTimeInSecond The time interval
     * @return The next partition. If curr can not be parsed, it will return null.
     */
    public String nextPartition(String curr, long deltaTimeInSecond) {
        Date date = parse(curr);
        if (date == null) {
            return null;
        }
        date.setTime(date.getTime() + 1000 * deltaTimeInSecond);
        return format(date);
    }

}
