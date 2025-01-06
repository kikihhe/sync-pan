package com.xiaohe.pan.common.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {

    public final static String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static String format(Object time) {
        try {
            return format(time, DATE_TIME_PATTERN);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 时间格式化成字符串
     *
     * @param time     时间
     * @param format 格式
     * @return {@link String }
     */
    public static String format(Object time, String format) {
        // 检查 time 是否为 Date 类型
        if (time instanceof Date) {
            Date date = (Date)time;
            // 如果是 Date 类型，调用 formatDate 方法
            return formatDate(date, format);
        }
        // 检查 time 是否为 Calendar 类型
        else if (time instanceof Calendar) {
            Calendar calendar = (Calendar) time;
            // 如果是 Calendar 类型，调用 formatCalendar 方法
            return formatCalendar(calendar, format);
        }
        // 检查 time 是否为 Instant 类型
        else if (time instanceof Instant) {
            Instant instant = (Instant) time;
            // 如果是 Instant 类型，调用 formatInstant 方法
            return formatInstant(instant, format);
        }
        // 检查 time 是否为 ZonedDateTime 类型
        else if (time instanceof ZonedDateTime) {
            ZonedDateTime zonedDateTime = (ZonedDateTime) time;
            // 如果是 ZonedDateTime 类型，调用 formatZonedDateTime 方法
            return formatZonedDateTime(zonedDateTime, format);
        }
        // 检查 time 是否为 LocalDateTime 类型
        else if (time instanceof LocalDateTime) {
            LocalDateTime localDateTime = (LocalDateTime) time;
            // 如果是 LocalDateTime 类型，调用 formatLocalDateTime 方法
            return formatLocalDateTime(localDateTime, format);
        }
        // 检查 time 是否为 Long 类型（时间戳）
        else if (time instanceof Long) {
            Long timestamp = (Long) time;
            // 如果是 Long 类型，调用 formatTimestamp 方法
            return formatTimestamp(timestamp, format);
        }
        // 如果 time 类型不支持，则抛出异常
        else {
            throw new IllegalArgumentException("Unsupported time type: " + time.getClass().getName());
        }
    }

    private static String formatDate(Date date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }

    private static String formatCalendar(Calendar calendar, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(calendar.getTime());
    }

    private static String formatInstant(Instant instant, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }

    private static String formatZonedDateTime(ZonedDateTime zdt, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return zdt.format(formatter);
    }

    private static String formatLocalDateTime(LocalDateTime ldt, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return ldt.format(formatter);
    }

    private static String formatTimestamp(long timestamp, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date date = new Date(timestamp);
        return sdf.format(date);
    }

    /**
     * 将时间字符串转换为 Date 对象
     * @param timeStr 时间字符串
     * @param format  格式化字符串
     * @return 转换后的 Date 对象
     */
    public static Date parseToDate(String timeStr, String format) {
        // 使用 SimpleDateFormat 解析时间字符串
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            return sdf.parse(timeStr);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static Date parseToDate(String timeStr) {
        return parseToDate(timeStr, DATE_TIME_PATTERN);
    }

    /**
     * 将时间字符串转换为 LocalDateTime 对象
     * @param timeStr 时间字符串
     * @param format  格式化字符串
     * @return 转换后的 LocalDateTime 对象
     */
    public static LocalDateTime parseToLocalDateTime(String timeStr, String format) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);

        // 如果格式中不包含时间信息，默认补上时间部分
        // 检查是否只有日期格式
        if (DATE_TIME_PATTERN.equals(format)) {
            // 补全时间为午夜
            timeStr += " 00:00:00";
            // 调整格式以匹配新的字符串
            dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        }

        return LocalDateTime.parse(timeStr, dtf);
    }

    public static LocalDateTime parseToLocalDateTime(String timeStr) {
        return parseToLocalDateTime(timeStr, DATE_TIME_PATTERN);
    }

    /**
     * 将时间字符串转换为不同位数的时间戳
     * @param timeStr 时间字符串
     * @param format  格式化字符串
     * @param digits    时间戳位数（10位秒级别，13位毫秒级别）
     * @return 转换后的时间戳
     */
    public static long parseToTimestamp(String timeStr, String format, int digits) {
        // 将时间字符串解析为 Date 对象
        Date date = parseToDate(timeStr, format);
        // 获取时间戳
        long timestamp = date.getTime();

        // 根据时间戳位数进行处理
        if (digits == 10) {
            // 如果需要10位时间戳，则转换为秒级别
            return timestamp / 1000;
        } else if (digits == 13) {
            // 如果需要13位时间戳，则返回毫秒级别时间戳
            return timestamp;
        } else {
            // 其他位数不支持，抛出异常
            throw new IllegalArgumentException("Unsupported timestamp digits: " + digits);
        }
    }

    public static long parseToTimestamp(String timeStr, int digits) {
        return parseToTimestamp(timeStr, DATE_TIME_PATTERN, digits);
    }

    public static long parseToTimestamp(String timeStr) {
        // 默认 13 位时间戳
        return parseToTimestamp(timeStr, DATE_TIME_PATTERN, 13);
    }

    /**
     * 将 LocalDateTime 对象转换为时间戳
     * @param localDateTime LocalDateTime 对象
     * @param zoneId 时区 ID
     * @param digits 时间戳位数（10位秒级别，13位毫秒级别）
     * @return 转换后的时间戳
     */
    private static long localDateTimeToTimestamp(LocalDateTime localDateTime, ZoneId zoneId, int digits) {
        ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId);
        long timestamp = zonedDateTime.toInstant().toEpochMilli();

        if (digits == 10) {
            // 如果需要10位时间戳，则转换为秒级别
            return timestamp / 1000;
        } else if (digits == 13) {
            // 如果需要13位时间戳，则返回毫秒级别时间戳
            return timestamp;
        } else {
            // 其他位数不支持，抛出异常
            throw new IllegalArgumentException("Unsupported timestamp digits: " + digits);
        }
    }

    public static long localDateTimeToTimestamp(LocalDateTime localDateTime, int digits) {
        return localDateTimeToTimestamp(localDateTime, ZoneId.systemDefault(), digits);
    }

    public static long localDateTimeToTimestamp(LocalDateTime localDateTime) {
        return localDateTimeToTimestamp(localDateTime, 13);
    }

    private static Duration calculateTimeDifferenceDuration(String startTimeStr, String endTimeStr, String format) {
        // 将时间字符串解析为 LocalDateTime 对象
        LocalDateTime startTime = parseToLocalDateTime(startTimeStr, format);
        LocalDateTime endTime = parseToLocalDateTime(endTimeStr, format);

        return calculateTimeDifferenceDuration(startTime, endTime);
    }

    private static Duration calculateTimeDifferenceDuration(LocalDateTime startTime, LocalDateTime endTime) {
        // 计算两个时间点之间的 Duration
        return Duration.between(startTime, endTime);
    }

    /**
     * 计算两个时间字符串之间的时间差，以秒为单位
     * @param startTimeStr 开始时间字符串
     * @param endTimeStr 结束时间字符串
     * @param format 时间字符串的格式
     * @return 时间差的秒数
     */
    public static long calculateTimeDifferenceInSeconds(String startTimeStr, String endTimeStr, String format) {
        // 计算两个时间点之间的 Duration
        Duration duration = calculateTimeDifferenceDuration(startTimeStr, endTimeStr, format);

        // 返回时间差的秒数
        return duration.getSeconds();
    }

    public static long calculateTimeDifferenceInSeconds(String startTimeStr, String endTimeStr) {
        return calculateTimeDifferenceInSeconds(startTimeStr, endTimeStr, DATE_TIME_PATTERN);
    }


    /**
     * 将时间差转换为个性化的时间表达方式
     * @param dateTime 时间字符串
     * @param format 时间字符串的格式
     * @return 个性化的时间表达方式
     */
    public static String formatTimeAgo(String dateTime, String format) {
        LocalDateTime time = parseToLocalDateTime(dateTime, format);
        LocalDateTime now = LocalDateTime.now();

        Duration duration = calculateTimeDifferenceDuration(time, now);

        if (duration.isZero() || duration.getSeconds() < 60) {
            return "刚刚";
        } else if (duration.toMinutes() < 60) {
            return duration.toMinutes() + "分钟前";
        } else if (duration.toHours() < 24) {
            return duration.toHours() + "小时前";
        } else if (duration.toDays() == 1) {
            return "昨天 " + time.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else if (duration.toDays() < 365) {
            return time.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        } else {
            return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }

    public static String formatTimeAgo(String dateTime) {
        return formatTimeAgo(dateTime, DATE_TIME_PATTERN);
    }

    /**
     * 获取当前时间的格式化字符串
     * @return 当前时间的格式化字符串
     */
    public static String getCurrentTime() {
        return format(LocalDateTime.now(), DATE_TIME_PATTERN);
    }

    /**
     * 获取指定日期的开始时间（零点）
     * @param dateStr 日期字符串
     * @param format 日期字符串的格式
     * @return 指定日期的开始时间
     */
    public static LocalDateTime getStartOfDay(String dateStr, String format) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
        LocalDate date = LocalDate.parse(dateStr, dtf);
        return date.atStartOfDay();
    }

    public static LocalDateTime getStartOfDay(String dateStr) {
        return getStartOfDay(dateStr, DATE_TIME_PATTERN);
    }

    /**
     * 获取指定日期的结束时间（23:59:59.999999999）
     * @param dateStr 日期字符串
     * @param format 日期字符串的格式
     * @return 指定日期的结束时间
     */
    public static LocalDateTime getEndOfDay(String dateStr, String format) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
        LocalDate date = LocalDate.parse(dateStr, dtf);
        LocalTime endOfDay = LocalTime.of(23, 59, 59, 999_999_999);
        return date.atTime(endOfDay);
    }

    public static LocalDateTime getEndOfDay(String dateStr) {
        return getEndOfDay(dateStr, DATE_TIME_PATTERN);
    }
}

