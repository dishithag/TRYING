package calendar.controller;

import calendar.CalendarProperty;
import calendar.EventProperty;
import calendar.WorkingHours;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses user command strings into structured commands with typed enums.
 * Behavior and error messages are preserved.
 */
public class CommandParser {

  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter DATETIME_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
  private static final LocalTime ALL_DAY_START = WorkingHours.START;
  private static final LocalTime ALL_DAY_END = WorkingHours.END;

  private static final Pattern FLAG_NAME = Pattern.compile(
      "--name\\s+(\"[^\"]+\"|\\S+)", Pattern.CASE_INSENSITIVE);
  private static final Pattern FLAG_TZ = Pattern.compile(
      "--timezone\\s+(\\S+)", Pattern.CASE_INSENSITIVE);
  private static final Pattern FLAG_PROP = Pattern.compile(
      "--property\\s+(\\S+)\\s+(\"[^\"]+\"|\\S+)", Pattern.CASE_INSENSITIVE);

  /**
   * Immutable value for a parsed command. Legacy string fields remain for compatibility.
   */
  public static class Command {
    private final String type;
    private final String subject;
    private final LocalDateTime startDateTime;
    private final LocalDateTime endDateTime;
    private final Set<DayOfWeek> weekdays;
    private final Integer occurrences;
    private final LocalDate untilDate;
    private final String property;
    private final String newValue;
    private final String editScope;
    private final String fileName;
    private final String calendarName;
    private final String timezoneId;
    private final String targetCalendar;
    private final LocalDateTime targetDateTime;
    private final LocalDate day;
    private final LocalDate rangeStart;
    private final LocalDate rangeEnd;

    private final CommandType typeEnum;
    private final EditScope editScopeEnum;
    private final EventProperty eventPropertyEnum;
    private final CalendarProperty calendarPropertyEnum;

    /**
     * Constructs a parsed command value used by controllers.
     *
     * <p>One constructor serves all command kinds; fields not relevant to the
     * detected command are {@code null}. When both a raw token and an enum are
     * provided, the enum is the normalized value.</p>
     *
     * @param type                 raw command type token (e.g., "create_calendar", "edit")
     * @param subject              event subject, when applicable
     * @param start                start date-time (ISO local), when applicable
     * @param end                  end date-time (ISO local), when applicable
     * @param weekdays             repeating days for series (MTWRFSU), when applicable
     * @param occurrences          repeat count for {@code for N times}, or {@code null}
     * @param untilDate            inclusive series end date for {@code until}, or {@code null}
     * @param property             raw edit property token (e.g., "start","location"),
     *                             or {@code null}
     * @param newValue             raw new value for edits (quoted text or ISO datetime),
     *                             or {@code null}
     * @param editScope            raw edit scope token: "event" | "events" | "series",
     *                             or {@code null}
     * @param fileName             export file name for {@code export cal}, or {@code null}
     * @param calendarName         calendar name for create/edit/use, or {@code null}
     * @param timezoneId           IANA time zone id for create/edit, or {@code null}
     * @param targetCalendar       destination calendar for copy commands, or {@code null}
     * @param targetDateTime       target instant (or target day at 00:00) for copy, or {@code null}
     * @param day                  source day for {@code copy events on}, or {@code null}
     * @param rangeStart           start day for {@code copy events between}, or {@code null}
     * @param rangeEnd             end day for {@code copy events between}, or {@code null}
     * @param typeEnum             normalized command type enum (preferred over
     *                             {@code type}) or {@code null}
     * @param editScopeEnum        normalized edit scope enum (preferred over
     *                             {@code editScope}) or {@code null}
     * @param eventPropertyEnum    normalized event property for edits, or {@code null}
     * @param calendarPropertyEnum normalized calendar property for calendar edits, or {@code null}
     */

    public Command(String type, String subject, LocalDateTime start,
                   LocalDateTime end, Set<DayOfWeek> weekdays,
                   Integer occurrences, LocalDate untilDate,
                   String property, String newValue, String editScope,
                   String fileName, String calendarName, String timezoneId,
                   String targetCalendar, LocalDateTime targetDateTime,
                   LocalDate day, LocalDate rangeStart, LocalDate rangeEnd,
                   CommandType typeEnum, EditScope editScopeEnum,
                   EventProperty eventPropertyEnum, CalendarProperty calendarPropertyEnum) {
      this.type = type;
      this.subject = subject;
      this.startDateTime = start;
      this.endDateTime = end;
      this.weekdays = weekdays;
      this.occurrences = occurrences;
      this.untilDate = untilDate;
      this.property = property;
      this.newValue = newValue;
      this.editScope = editScope;
      this.fileName = fileName;
      this.calendarName = calendarName;
      this.timezoneId = timezoneId;
      this.targetCalendar = targetCalendar;
      this.targetDateTime = targetDateTime;
      this.day = day;
      this.rangeStart = rangeStart;
      this.rangeEnd = rangeEnd;
      this.typeEnum = typeEnum;
      this.editScopeEnum = editScopeEnum;
      this.eventPropertyEnum = eventPropertyEnum;
      this.calendarPropertyEnum = calendarPropertyEnum;
    }

    public String getType() {
      return type;
    }

    public String getSubject() {
      return subject;
    }

    public LocalDateTime getStartDateTime() {
      return startDateTime;
    }

    public LocalDateTime getEndDateTime() {
      return endDateTime;
    }

    public Set<DayOfWeek> getWeekdays() {
      return weekdays;
    }

    public Integer getOccurrences() {
      return occurrences;
    }

    public LocalDate getUntilDate() {
      return untilDate;
    }

    public String getProperty() {
      return property;
    }

    public String getNewValue() {
      return newValue;
    }

    public String getEditScope() {
      return editScope;
    }

    public String getFileName() {
      return fileName;
    }

    public String getCalendarName() {
      return calendarName;
    }

    public String getTimezoneId() {
      return timezoneId;
    }

    public String getTargetCalendar() {
      return targetCalendar;
    }

    public LocalDateTime getTargetDateTime() {
      return targetDateTime;
    }

    public LocalDate getDay() {
      return day;
    }

    public LocalDate getRangeStart() {
      return rangeStart;
    }

    public LocalDate getRangeEnd() {
      return rangeEnd;
    }

    public CommandType getTypeEnum() {
      return typeEnum;
    }

    public EditScope getEditScopeEnum() {
      return editScopeEnum;
    }

    public EventProperty getEventPropertyEnum() {
      return eventPropertyEnum;
    }

    public CalendarProperty getCalendarPropertyEnum() {
      return calendarPropertyEnum;
    }
  }

  /**
   * Parses one user input line into a structured {@link Command}.
   *
   * <p>Trims the input, determines the command family by prefix, and
   * delegates to specialized parsers. On invalid or unsupported
   * syntax, it throws an {@link IllegalArgumentException} with a
   * user-readable message.
   * </p>
   *
   * <p>Recognized forms include:
   * {@code exit},
   * {@code create calendar ...},
   * {@code edit calendar ...},
   * {@code use calendar ...},
   * {@code export cal ...},
   * {@code show status on ...},
   * {@code print events on ...},
   * {@code print events from ...},
   * {@code copy event ...},
   * {@code copy events on ...},
   * {@code copy events between ...},
   * {@code create event ...},
   * and {@code edit event|events|series ...}.
   * </p>
   *
   * @param line raw user input; leading/trailing whitespace ignored
   * @return a populated {@link Command} describing the action to run
   * @throws IllegalArgumentException if {@code line} is null/empty
   *                                  or the detected command has invalid syntax
   */

  public Command parse(String line) {
    if (line == null || line.trim().isEmpty()) {
      throw new IllegalArgumentException("Empty command");
    }
    String trimmed = line.trim();
    String lower = trimmed.toLowerCase();

    if (lower.equals("exit")) {
      return new Command("exit", null, null, null, null, null, null,
          null, null, null, null, null, null, null, null, null, null, null,
          CommandType.EXIT, null, null, null);
    }
    if (lower.startsWith("create calendar ")) {
      return parseCreateCalendar(trimmed);
    }
    if (lower.startsWith("edit calendar ")) {
      return parseEditCalendar(trimmed);
    }
    if (lower.startsWith("use calendar ")) {
      return parseUseCalendar(trimmed);
    }
    if (lower.startsWith("export cal ")) {
      String fileName = trimmed.substring("export cal ".length()).trim();
      return new Command("export", null, null, null, null, null, null,
          null, null, null, fileName, null, null, null, null, null, null, null,
          CommandType.EXPORT, null, null, null);
    }
    if (lower.startsWith("show status on ")) {
      String dateTimeStr = trimmed.substring("show status on ".length()).trim();
      LocalDateTime dateTime = parseDateTime(dateTimeStr);
      return new Command("status", null, dateTime, null, null, null, null,
          null, null, null, null, null, null, null, null, null, null, null,
          CommandType.STATUS, null, null, null);
    }
    if (lower.startsWith("print events on ")) {
      String dateStr = trimmed.substring("print events on ".length()).trim();
      LocalDate date = parseDate(dateStr);
      return new Command("print_on", null, date.atStartOfDay(), null, null, null, null,
          null, null, null, null, null, null, null, null, null, null, null,
          CommandType.PRINT_ON, null, null, null);
    }
    if (lower.startsWith("print events from ")) {
      return parsePrintRange(trimmed);
    }
    if (lower.startsWith("copy event ")) {
      return parseCopyEvent(trimmed);
    }
    if (lower.startsWith("copy events on ")) {
      return parseCopyOnDate(trimmed);
    }
    if (lower.startsWith("copy events between ")) {
      return parseCopyBetween(trimmed);
    }
    if (lower.startsWith("create event ")) {
      return parseCreateEvent(trimmed.substring("create event ".length()));
    }
    if (lower.startsWith("edit event ")
        || lower.startsWith("edit events ")
        || lower.startsWith("edit series ")) {
      return parseEditCommand(trimmed);
    }
    throw new IllegalArgumentException("Unknown command: " + trimmed);
  }

  private Command parseCreateCalendar(String line) {
    Matcher nameM = FLAG_NAME.matcher(line);
    Matcher tzM = FLAG_TZ.matcher(line);
    if (!nameM.find() || !tzM.find()) {
      throw new IllegalArgumentException(
          "Invalid syntax. Expected: create calendar --name <name> --timezone <area/location>");
    }
    String name = unquote(nameM.group(1));
    String tz = tzM.group(1);
    return new Command("create_calendar", null, null, null, null, null, null,
        null, null, null, null, name, tz, null, null, null, null, null,
        CommandType.CREATE_CALENDAR, null, null, null);
  }

  private Command parseEditCalendar(String line) {
    Matcher nameM = FLAG_NAME.matcher(line);
    Matcher propM = FLAG_PROP.matcher(line);
    if (!nameM.find() || !propM.find()) {
      throw new IllegalArgumentException(
          "Invalid syntax. Expected: edit calendar --name <name>"
              +
              " --property <name|timezone> <value>");
    }
    String name = unquote(nameM.group(1));
    String prop = propM.group(1).toLowerCase();
    String val = unquote(propM.group(2));
    CalendarProperty cp;
    if ("name".equals(prop)) {
      cp = CalendarProperty.NAME;
    } else {
      if ("timezone".equals(prop)) {
        cp = CalendarProperty.TIMEZONE;
      } else {
        throw new IllegalArgumentException("Unknown calendar property: " + prop);
      }
    }
    return new Command("edit_calendar", null, null, null, null, null, null,
        prop, val, null, null, name, null, null, null, null, null, null,
        CommandType.EDIT_CALENDAR, null, null, cp);
  }

  private Command parseUseCalendar(String line) {
    Matcher nameM = FLAG_NAME.matcher(line);
    if (!nameM.find()) {
      throw new IllegalArgumentException(
          "Invalid syntax. Expected: use calendar --name <name>");
    }
    String name = unquote(nameM.group(1));
    return new Command("use_calendar", null, null, null, null, null, null,
        null, null, null, null, name, null, null, null, null, null, null,
        CommandType.USE_CALENDAR, null, null, null);
  }

  private Command parsePrintRange(String trimmed) {
    Pattern pattern = Pattern.compile(
        "print events from (\\S+) to (\\S+)", Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(trimmed);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid print range syntax");
    }
    LocalDateTime start = parseDateTime(matcher.group(1));
    LocalDateTime end = parseDateTime(matcher.group(2));
    return new Command("print_range", null, start, end, null, null, null,
        null, null, null, null, null, null, null, null, null, null, null,
        CommandType.PRINT_RANGE, null, null, null);
  }

  private Command parseCreateEvent(String args) {
    String subject;
    String remaining;

    if (args.startsWith("\"")) {
      int endQuote = args.indexOf("\"", 1);
      if (endQuote == -1) {
        throw new IllegalArgumentException("Unclosed quote in subject");
      }
      subject = args.substring(1, endQuote);
      remaining = args.substring(endQuote + 1).trim();
    } else {
      int fromIdx = indexOfIgnoreCase(args, " from ");
      int onIdx = indexOfIgnoreCase(args, " on ");
      if (fromIdx > 0 && (onIdx == -1 || fromIdx < onIdx)) {
        subject = args.substring(0, fromIdx);
        remaining = args.substring(fromIdx).trim();
      } else if (onIdx > 0) {
        subject = args.substring(0, onIdx);
        remaining = args.substring(onIdx).trim();
      } else {
        throw new IllegalArgumentException("Invalid syntax: expected 'from' or 'on'");
      }
    }

    if (remaining.toLowerCase().startsWith("on ")) {
      return parseAllDayEvent(subject, remaining.substring(3).trim());
    }
    if (remaining.toLowerCase().startsWith("from ")) {
      return parseTimedEvent(subject, remaining.substring(5).trim());
    }
    throw new IllegalArgumentException("Invalid create event syntax");
  }

  private Command parseAllDayEvent(String subject, String args) {
    String[] parts = args.split("\\s+");
    LocalDate date = parseDate(parts[0]);
    LocalDateTime start = date.atTime(ALL_DAY_START);
    LocalDateTime end = date.atTime(ALL_DAY_END);

    if (parts.length == 1) {
      return new Command("create_single", subject, start, end,
          null, null, null, null, null, null, null,
          null, null, null, null, null, null, null,
          CommandType.CREATE_SINGLE, null, null, null);
    }

    if (parts.length >= 4 && parts[1].equalsIgnoreCase("repeats")) {
      Set<DayOfWeek> wd = parseWeekdays(parts[2]);

      if (parts[3].equalsIgnoreCase("for") && parts.length >= 6) {
        int occurrences = Integer.parseInt(parts[4]);
        return new Command("create_series", subject, start, end,
            wd, occurrences, null, null, null, null, null,
            null, null, null, null, null, null, null,
            CommandType.CREATE_SERIES, null, null, null);
      }

      if (parts[3].equalsIgnoreCase("until") && parts.length >= 5) {
        LocalDate untilDate = parseDate(parts[4]);
        return new Command("create_series_until", subject, start, end,
            wd, null, untilDate, null, null, null, null,
            null, null, null, null, null, null, null,
            CommandType.CREATE_SERIES_UNTIL, null, null, null);
      }
    }

    throw new IllegalArgumentException("Invalid all-day event syntax");
  }

  private Command parseTimedEvent(String subject, String args) {
    Pattern pattern = Pattern.compile(
        "(\\S+)\\s+to\\s+(\\S+)(?:\\s+repeats\\s+(\\S+)\\s+(for|until)\\s+(\\S+)(?:\\s+times)?)?");
    Matcher matcher = pattern.matcher(args);

    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid timed event syntax");
    }

    LocalDateTime start = parseDateTime(matcher.group(1));
    LocalDateTime end = parseDateTime(matcher.group(2));

    if (matcher.group(3) == null) {
      return new Command("create_single", subject, start, end,
          null, null, null, null, null, null, null,
          null, null, null, null, null, null, null,
          CommandType.CREATE_SINGLE, null, null, null);
    }

    Set<DayOfWeek> wd = parseWeekdays(matcher.group(3));
    String repeatType = matcher.group(4);
    String repeatValue = matcher.group(5);

    if (repeatType.equalsIgnoreCase("for")) {
      int occurrences = Integer.parseInt(repeatValue);
      return new Command("create_series", subject, start, end,
          wd, occurrences, null, null, null, null, null,
          null, null, null, null, null, null, null,
          CommandType.CREATE_SERIES, null, null, null);
    }

    LocalDate untilDate = parseDate(repeatValue);
    return new Command("create_series_until", subject, start, end,
        wd, null, untilDate, null, null, null, null,
        null, null, null, null, null, null, null,
        CommandType.CREATE_SERIES_UNTIL, null, null, null);
  }

  private Command parseEditCommand(String line) {
    String scope;
    String remaining;

    if (line.toLowerCase().startsWith("edit event ")) {
      scope = "event";
      remaining = line.substring("edit event ".length());
    } else if (line.toLowerCase().startsWith("edit events ")) {
      scope = "events";
      remaining = line.substring("edit events ".length());
    } else {
      scope = "series";
      remaining = line.substring("edit series ".length());
    }

    String[] parts = remaining.split("\\s+", 2);
    if (parts.length < 2) {
      throw new IllegalArgumentException("Invalid edit syntax");
    }

    final String property = parts[0];
    String rest = parts[1];
    String subject;
    String afterSubject;

    if (rest.startsWith("\"")) {
      int endQuote = rest.indexOf("\"", 1);
      if (endQuote == -1) {
        throw new IllegalArgumentException("Unclosed quote");
      }
      subject = rest.substring(1, endQuote);
      afterSubject = rest.substring(endQuote + 1).trim();
    } else {
      int fromIdx = rest.toLowerCase().indexOf(" from ");
      if (fromIdx == -1) {
        throw new IllegalArgumentException("Missing 'from'");
      }
      subject = rest.substring(0, fromIdx).trim();
      afterSubject = rest.substring(fromIdx).trim();
    }

    Pattern pattern = Pattern.compile(
        "from\\s+(\\S+)\\s+with\\s+(.+)", Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(afterSubject);

    if (!matcher.matches()) {
      throw new IllegalArgumentException(
          "Invalid edit syntax. Expected: from <dateTime> with <value>");
    }

    LocalDateTime start = parseDateTime(matcher.group(1));
    String newValue = stripQuotes(matcher.group(2).trim());

    EditScope scopeEnum = EditScope.fromToken(scope);
    EventProperty ep = EventProperty.fromToken(property);

    return new Command("edit", subject, start, null,
        null, null, null, property, newValue, scope, null,
        null, null, null, null, null, null, null,
        CommandType.EDIT, scopeEnum, ep, null);
  }

  private Command parseCopyEvent(String line) {
    String body = line.substring("copy event ".length()).trim();

    String subject;
    String rest;
    if (body.startsWith("\"")) {
      int endQuote = body.indexOf("\"", 1);
      if (endQuote == -1) {
        throw new IllegalArgumentException("Unclosed quote in event name");
      }
      subject = body.substring(1, endQuote);
      rest = body.substring(endQuote + 1).trim();
    } else {
      int onIdx = body.toLowerCase().indexOf(" on ");
      if (onIdx < 0) {
        throw new IllegalArgumentException("Missing 'on' for copy event");
      }
      subject = body.substring(0, onIdx).trim();
      rest = body.substring(onIdx).trim();
    }

    Pattern p = Pattern.compile(
        "on\\s+(\\S+)\\s+--target\\s+(\"[^\"]+\"|\\S+)\\s+to\\s+(\\S+)",
        Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(rest);
    if (!m.matches()) {
      throw new IllegalArgumentException("Invalid copy event syntax");
    }

    LocalDateTime sourceStart = parseDateTime(m.group(1));
    String targetCal = unquote(m.group(2));
    LocalDateTime targetStart = parseDateTime(m.group(3));

    return new Command("copy_event", subject, sourceStart, null,
        null, null, null, null, null, null, null,
        null, null, targetCal, targetStart, null, null, null,
        CommandType.COPY_EVENT, null, null, null);
  }

  private Command parseCopyOnDate(String line) {
    Pattern p = Pattern.compile(
        "copy\\s+events\\s+on\\s+(\\S+)\\s+--target\\s+(\"[^\"]+\"|\\S+)\\s+to\\s+(\\S+)",
        Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(line);
    if (!m.matches()) {
      throw new IllegalArgumentException("Invalid copy events on syntax");
    }

    LocalDate sourceDate = parseDate(m.group(1));
    String targetCal = unquote(m.group(2));
    LocalDate targetDate = parseDate(m.group(3));

    return new Command("copy_on_date", null, null, null,
        null, null, null, null, null, null, null,
        null, null, targetCal, targetDate.atStartOfDay(), sourceDate, null, null,
        CommandType.COPY_ON_DATE, null, null, null);
  }

  private Command parseCopyBetween(String line) {
    Pattern p = Pattern.compile(
        "copy\\s+events\\s+between\\s+(\\S+)\\s+and\\s+(\\S+)\\s"
            +
            "+--target\\s+(\"[^\"]+\"|\\S+)\\s+to\\s+(\\S+)",
        Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(line);
    if (!m.matches()) {
      throw new IllegalArgumentException("Invalid copy events between syntax");
    }

    LocalDate start = parseDate(m.group(1));
    LocalDate end = parseDate(m.group(2));
    String targetCal = unquote(m.group(3));
    LocalDate targetStart = parseDate(m.group(4));

    return new Command("copy_between", null, null, null,
        null, null, null, null, null, null, null,
        null, null, targetCal, targetStart.atStartOfDay(), null, start, end,
        CommandType.COPY_BETWEEN, null, null, null);
  }

  private LocalDate parseDate(String dateStr) {
    try {
      return LocalDate.parse(dateStr, DATE_FMT);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(
          "Invalid date: " + dateStr + ". Expected format: YYYY-MM-DD");
    }
  }

  private LocalDateTime parseDateTime(String dateTimeStr) {
    try {
      return LocalDateTime.parse(dateTimeStr, DATETIME_FMT);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(
          "Invalid datetime: " + dateTimeStr + ". Expected format: YYYY-MM-DDTHH:mm");
    }
  }

  private Set<DayOfWeek> parseWeekdays(String token) {
    if (token == null || token.isEmpty()) {
      throw new IllegalArgumentException("Missing weekday pattern");
    }
    EnumSet<DayOfWeek> set = EnumSet.noneOf(DayOfWeek.class);
    for (char c : token.toUpperCase().toCharArray()) {
      switch (c) {
        case 'M':
          set.add(DayOfWeek.MONDAY);
          break;
        case 'T':
          set.add(DayOfWeek.TUESDAY);
          break;
        case 'W':
          set.add(DayOfWeek.WEDNESDAY);
          break;
        case 'R':
          set.add(DayOfWeek.THURSDAY);
          break;
        case 'F':
          set.add(DayOfWeek.FRIDAY);
          break;
        case 'S':
          set.add(DayOfWeek.SATURDAY);
          break;
        case 'U':
          set.add(DayOfWeek.SUNDAY);
          break;
        default:
          throw new IllegalArgumentException("Invalid weekday letter: " + c);
      }
    }
    return set;
  }

  private static String unquote(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
      return t.substring(1, t.length() - 1);
    }
    return t;
  }

  private static String stripQuotes(String s) {
    if (s == null || s.length() < 2) {
      return s;
    }
    if (s.startsWith("\"") && s.endsWith("\"")) {
      return s.substring(1, s.length() - 1);
    }
    return s;
  }

  private static int indexOfIgnoreCase(String haystack, String needle) {
    String h = haystack.toLowerCase();
    String n = needle.toLowerCase();
    return h.indexOf(n);
  }
}
