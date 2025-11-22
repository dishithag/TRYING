package calendar.controller;

import calendar.Calendar;
import calendar.CalendarBook;
import calendar.CalendarProperty;
import calendar.Event;
import calendar.EventProperty;
import calendar.util.ExportUtil;
import calendar.view.CalendarView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Base controller with shared execution logic for the calendar app.
 */
public abstract class AbstractCalendarController implements CalendarController {

  protected final CalendarBook book;
  protected final CalendarContext context;
  protected final CalendarView view;
  protected final BufferedReader reader;
  protected final CommandParser parser;

  /**
   * Constructs a controller wired to a {@link calendar.CalendarBook} model,
   * a {@link calendar.view.CalendarView}, and a character {@link java.io.Reader}.
   * Initializes the {@link CalendarContext}, buffered input, and command parser.
   *
   * @param book  calendar-book model; must not be {@code null}
   * @param view  view used for user-facing output; must not be {@code null}
   * @param input character stream to read commands from; must not be {@code null}
   * @throws IllegalArgumentException if any argument is {@code null}
   */

  protected AbstractCalendarController(CalendarBook book, CalendarView view, Reader input) {
    if (book == null || view == null || input == null) {
      throw new IllegalArgumentException("Arguments cannot be null");
    }
    this.book = book;
    this.context = new CalendarContext(book);
    this.view = view;
    this.reader = new BufferedReader(input);
    this.parser = new CommandParser();
  }

  @Override
  public void run() {
    displayWelcome();
    boolean foundExit = false;
    int lineNumber = 0;
    try {
      while (true) {
        showPrompt();
        String line = reader.readLine();
        if (line == null) {
          break;
        }
        lineNumber++;
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
          continue;
        }
        try {
          CommandParser.Command cmd = parser.parse(trimmed);
          if (cmd.typeEnum == CommandType.EXIT) {
            handleExit();
            foundExit = true;
            break;
          }
          executeCommand(cmd);
        } catch (Exception e) {
          handleError(e.getMessage(), lineNumber);
        }
      }
      if (!foundExit) {
        handleMissingExit();
      }
    } catch (IOException e) {
      view.displayError("IO error: " + e.getMessage());
    }
  }

  /**
   * Executes a parsed {@link CommandParser.Command} by dispatching on
   * {@code cmd.typeEnum}. Depending on the type, this method creates or edits
   * calendars/events, prints listings or status, exports data, or copies events,
   * delegating to helpers such as {@link #handleEdit(CommandParser.Command)},
   * {@link #handleEditCalendar(CommandParser.Command)},
   * {@link #handleCopySingle(CommandParser.Command)},
   * {@link #handleCopyOnDate(CommandParser.Command)}, and
   * {@link #handleCopyBetween(CommandParser.Command)}.
   * Any runtime errors are caught and surfaced through the {@link calendar.view.CalendarView}.
   *
   * @param cmd the parsed command to execute (must not be {@code null})
   */

  protected void executeCommand(CommandParser.Command cmd) {
    try {
      switch (cmd.typeEnum) {
        case CREATE_CALENDAR:
          {
          book.createCalendar(cmd.calendarName, ZoneId.of(cmd.timezoneId));
          context.use(cmd.calendarName);
          view.displayMessage("Created and switched to calendar: " + cmd.calendarName);
          break;
          }
        case EDIT_CALENDAR:
          {
          handleEditCalendar(cmd);
          break;
          }
        case USE_CALENDAR:
          {
          context.use(cmd.calendarName);
          view.displayMessage("Using calendar: " + cmd.calendarName);
          break;
          }
        case CREATE_SINGLE:
          {
          Calendar cal = context.current();
          Event e = cal.createEvent(cmd.subject, cmd.startDateTime, cmd.endDateTime);
          view.displayMessage("Event created: " + e.getSubject());
          break;
          }
        case CREATE_SERIES:
          {
          Calendar cal = context.current();
          List<Event> series = cal.createEventSeries(
              cmd.subject, cmd.startDateTime, cmd.endDateTime, cmd.weekdays, cmd.occurrences);
          view.displayMessage("Created " + series.size() + " events in series");
          break;
          }
        case CREATE_SERIES_UNTIL:
          {
          Calendar cal = context.current();
          List<Event> seriesUntil = cal.createEventSeriesUntil(
              cmd.subject, cmd.startDateTime, cmd.endDateTime, cmd.weekdays, cmd.untilDate);
          view.displayMessage("Created " + seriesUntil.size() + " events in series");
          break;
          }
        case EDIT:
          {
          handleEdit(cmd);
          break;
          }
        case PRINT_ON:
          {
          Calendar cal = context.current();
          view.displayEvents(cal.getEventsOnDate(cmd.startDateTime.toLocalDate()));
          break;
          }
        case PRINT_RANGE:
          {
          Calendar cal = context.current();
          view.displayEvents(cal.getEventsInRange(cmd.startDateTime, cmd.endDateTime));
          break;
          }
        case STATUS:
          {
          Calendar cal = context.current();
          view.displayStatus(cal.isBusyAt(cmd.startDateTime));
          break;
          }
        case EXPORT:
          {
          Calendar cal = context.current();
          String path = ExportUtil.export(cal, cmd.fileName);
          view.displayMessage("Exported to: " + path);
          break;
          }
        case COPY_EVENT:
          {
          handleCopySingle(cmd);
          break;
          }
        case COPY_ON_DATE:
          {
          handleCopyOnDate(cmd);
          break;
          }
        case COPY_BETWEEN:
          {
          handleCopyBetween(cmd);
          break;
          }
        default:
          view.displayError("Unknown command type: " + cmd.type);
      }
    } catch (Exception e) {
      view.displayError(e.getMessage());
    }
  }

  /**
   * Applies an edit command to the active calendar.
   * Resolves the target {@link EventProperty} and the edit scope
   * (event / events-from-date / whole series), coerces {@code cmd.newValue}
   * to a {@link java.time.LocalDateTime} when editing START/END or to text
   * otherwise, and delegates to the corresponding Calendar edit method.
   * Required fields in {@code cmd}: {@code subject}, {@code startDateTime},
   * property (as {@code eventPropertyEnum} or token), new value, and scope
   * (as {@code editScopeEnum} or token).
   *
   * @param cmd parsed edit command (not {@code null})
   * @throws IllegalArgumentException if fields are missing or values cannot be parsed
   */

  protected void handleEdit(CommandParser.Command cmd) {
    Calendar cal = context.current();
    EventProperty prop = cmd.eventPropertyEnum != null
        ? cmd.eventPropertyEnum
        : EventProperty.fromToken(cmd.property);

    LocalDateTime dt = null;
    String text = null;
    if (prop == EventProperty.START || prop == EventProperty.END) {
      dt = LocalDateTime.parse(cmd.newValue);
    } else {
      text = cmd.newValue;
    }

    EditScope scope = cmd.editScopeEnum != null
        ? cmd.editScopeEnum
        : EditScope.fromToken(cmd.editScope);

    switch (scope) {
      case EVENT:
        cal.editEvent(cmd.subject, cmd.startDateTime, prop, dt, text);
        view.displayMessage("Event edited");
        break;
      case EVENTS:
        cal.editEventsFromDate(cmd.subject, cmd.startDateTime, prop, dt, text);
        view.displayMessage("Events edited");
        break;
      case SERIES:
        cal.editSeries(cmd.subject, cmd.startDateTime, prop, dt, text);
        view.displayMessage("Series edited");
        break;
      default:
        view.displayError("Unknown edit scope: " + cmd.editScope);
    }
  }

  /**
   * Handles an "edit calendar" command.
   *
   * <p>Supported properties:
   * <ul>
   *   <li>{@link CalendarProperty#NAME} — renames {@code cmd.calendarName}
   *   to {@code cmd.newValue}.</li>
   *   <li>{@link CalendarProperty#TIMEZONE} — updates the calendar
   *   timezone to {@code ZoneId.of(cmd.newValue)}.</li>
   * </ul>
   * On success, a confirmation is printed via the {@link calendar.view.CalendarView}.
   * If the property is not supported, an error message is shown.
   *
   * <p>Required fields in {@code cmd}: {@code calendarName}, {@code newValue},
   * and {@code calendarPropertyEnum}.</p>
   *
   * @param cmd parsed calendar-edit command (not {@code null})
   * @throws IllegalArgumentException    if the calendar does not exist or a rename conflicts
   * @throws java.time.DateTimeException if {@code newValue} is not a valid IANA timezone ID
   */

  protected void handleEditCalendar(CommandParser.Command cmd) {
    CalendarProperty cp = cmd.calendarPropertyEnum;
    if (cp == CalendarProperty.NAME) {
      book.renameCalendar(cmd.calendarName, cmd.newValue);
      view.displayMessage("Calendar renamed to: " + cmd.newValue);
    } else if (cp == CalendarProperty.TIMEZONE) {
      book.changeTimezone(cmd.calendarName, ZoneId.of(cmd.newValue));
      view.displayMessage("Timezone updated for: " + cmd.calendarName);
    } else {
      view.displayError("Unknown calendar property: " + cmd.property);
    }
  }

  /**
   * Copies a single event; prevents exact duplicates only.
   */
  protected void handleCopySingle(CommandParser.Command cmd) {
    Calendar src = context.current();
    Calendar dst = book.getCalendar(cmd.targetCalendar);
    List<Event> matches = src.findEvents(cmd.subject, cmd.startDateTime);
    if (matches.size() != 1) {
      throw new IllegalArgumentException("Event not found or not unique");
    }
    Event e = matches.get(0);
    Duration dur = Duration.between(e.getStartDateTime(), e.getEndDateTime());
    LocalDateTime newStart = cmd.targetDateTime;
    LocalDateTime newEnd = newStart.plus(dur);

    if (existsExact(dst, e.getSubject(), newStart, newEnd)) {
      view.displayMessage("Copied 0 event(s) to " + cmd.targetCalendar);
      return;
    }

    dst.copyFrom(e, newStart, newEnd);
    view.displayMessage("Copied 1 event to " + cmd.targetCalendar);
  }

  /**
   * Copies all events on a given day; skips if subject already exists on that day in the target.
   */
  protected void handleCopyOnDate(CommandParser.Command cmd) {
    Calendar src = context.current();
    Calendar dst = book.getCalendar(cmd.targetCalendar);
    LocalDate srcDay = cmd.day;
    List<Event> todays = src.getEventsOnDate(srcDay);
    int copied = 0;
    for (Event e : todays) {
      Duration dur = Duration.between(e.getStartDateTime(), e.getEndDateTime());
      LocalDateTime localClockAtDst = e.getStartDateTime()
          .atZone(src.getZoneId())
          .withZoneSameInstant(dst.getZoneId())
          .toLocalDateTime();
      LocalDateTime newStart = cmd.targetDateTime
          .toLocalDate().atTime(localClockAtDst.toLocalTime());
      LocalDateTime newEnd = newStart.plus(dur);

      if (existsExact(dst, e.getSubject(), newStart, newEnd)
          || existsSameDaySubject(dst, e.getSubject(), newStart)) {
        continue;
      }

      dst.copyFrom(e, newStart, newEnd);
      copied++;
    }
    view.displayMessage("Copied " + copied + " event(s) to " + cmd.targetCalendar);
  }

  /**
   * Copies all events in a range; skips if subject already exists on that day in the target.
   */
  protected void handleCopyBetween(CommandParser.Command cmd) {
    Calendar src = context.current();
    Calendar dst = book.getCalendar(cmd.targetCalendar);

    LocalDate startDay = cmd.rangeStart;
    LocalDate endDay = cmd.rangeEnd;
    LocalDateTime srcStart = startDay.atStartOfDay();
    LocalDateTime srcEnd = endDay.plusDays(1).atStartOfDay();

    List<Event> inRange = src.getEventsInRange(srcStart, srcEnd.minusSeconds(1));
    int copied = 0;

    for (Event e : inRange) {
      LocalDate d = e.getStartDateTime().toLocalDate();
      long dayOffset = java.time.temporal.ChronoUnit.DAYS.between(startDay, d);
      Duration dur = Duration.between(e.getStartDateTime(), e.getEndDateTime());
      LocalDateTime localClockAtDst = e.getStartDateTime()
          .atZone(src.getZoneId())
          .withZoneSameInstant(dst.getZoneId())
          .toLocalDateTime();

      LocalDate targetDay = cmd.targetDateTime.toLocalDate().plusDays(dayOffset);
      LocalDateTime newStart = targetDay.atTime(localClockAtDst.toLocalTime());
      LocalDateTime newEnd = newStart.plus(dur);

      if (existsExact(dst, e.getSubject(), newStart, newEnd)
          || existsSameDaySubject(dst, e.getSubject(), newStart)) {
        continue;
      }

      dst.copyFrom(e, newStart, newEnd);
      copied++;
    }

    view.displayMessage("Copied " + copied + " event(s) to " + cmd.targetCalendar);
  }

  private boolean existsExact(Calendar cal, String subject, LocalDateTime start,
                              LocalDateTime end) {
    if (!cal.findEvents(subject, start, end).isEmpty()) {
      return true;
    }
    return !cal.findEvents(subject, start).isEmpty();
  }

  private boolean existsSameDaySubject(Calendar cal, String subject, LocalDateTime candidateStart) {
    LocalDate day = candidateStart.toLocalDate();
    for (Event e : cal.getEventsOnDate(day)) {
      if (e.getSubject().equals(subject)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Prints the startup banner or instructions for this controller mode.
   * Called once at the beginning of {@link #run()}.
   */
  protected abstract void displayWelcome();

  /**
   * Shows the user prompt before reading each command.
   * Implementations may no-op for headless/script modes.
   */
  protected abstract void showPrompt();

  /**
   * Runs when an explicit {@code exit} command is processed.
   * Use to print a farewell and perform final cleanup.
   */
  protected abstract void handleExit();

  /**
   * Reports a recoverable error tied to a single input line.
   *
   * @param message    human-readable error text
   * @param lineNumber 1-based line number in the input
   */
  protected abstract void handleError(String message, int lineNumber);

  /**
   * Invoked when input ends without an explicit {@code exit}.
   * Useful for warning users/scripts about an incomplete session.
   */
  protected abstract void handleMissingExit();

}
