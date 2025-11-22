# USEME

## Build
./gradlew clean jar

## Run (interactive)
java -jar build/libs/calendar-1.0.jar --mode interactive

Example (type one per line):
create calendar --name Fall --timezone America/New_York
use calendar --name Fall
create event "Lecture 1" from 2024-09-05T10:00 to 2024-09-05T11:00
print events on 2024-09-05
export cal fall_calendar.csv
export cal fall_calendar.ical
exit

## Run (headless)
java -jar build/libs/calendar-1.0.jar --mode headless res/commands.txt

Notes:
- Run from project root (same folder as `gradlew`).
- Scripts must have no blank lines. Format is auto-detected by extension (.csv / .ical).
