package calendar.controller;

/**
 * Strategy interface for parsing a raw command line into a structured {@link CommandParser.Command}.
 */
public interface CommandParsingStrategy {

  /**
   * Parses a user-provided line into a structured command.
   *
   * @param line raw command text
   * @return parsed command with typed fields
   */
  CommandParser.Command parse(String line);
}
