# Class Attendance App

A desktop class attendance app built with **Java Swing**. Teachers can manage a student list, record daily attendance on a color-coded calendar, and mark holidays and vacations. No external libraries and no database are needed.

## Features

- **Course name:** editable from the header bar
- **Students:** add and remove students with a name and ID (duplicate IDs are rejected)
- **Attendance:** mark each student as Present, Absent, Late or Excused for the selected date
- **Mark All Present:** one click to fill in the whole class
- **Calendar:** month view with previous and next buttons
  - Red = holiday, orange = vacation, gray = weekend, purple = selected day, green border = today
- **Holidays and vacations:** add a name and a date range; attendance is disabled on those days and on weekends
- **Attendance %:** calculated automatically for each student (Present and Late count as attended, Excused days are ignored)
- **Auto-save:** data is saved to `attendance.dat` after every change and when the window closes

## Requirements

- JDK 11 or newer (JDK 17 recommended)
- IntelliJ IDEA (or any Java IDE)

## How to run

### In IntelliJ IDEA

1. Download or clone this repository.
2. Open the folder in IntelliJ IDEA and set a JDK under **File > Project Structure > Project > SDK**.
3. Open `src/com/classtrack/AttendanceApp.java`.
4. Click the green arrow next to the `main` method and choose **Run**.

### From the command line

```
javac -d out src/com/classtrack/AttendanceApp.java
java -cp out com.classtrack.AttendanceApp
```

## How to use

1. Type the course name in the purple bar and press Enter.
2. Click **+ Add Student** and enter an ID and a name.
3. Click a date on the calendar.
4. Choose a status for each student in the **Status** column, or click **Mark All Present**.
5. To add a holiday or vacation, click **+ Add Holiday / Vacation** and enter the dates as `yyyy-MM-dd` (for example `2026-12-25`).

## Data structures used

| Structure | Purpose |
|---|---|
| `Student`, `Event`, `Data` classes | Hold one student, one holiday or vacation, and all saved data |
| `ArrayList<Student>` | The list of students, in the order they were added |
| `ArrayList<Event>` | The list of holidays and vacations |
| `HashMap<LocalDate, HashMap<String, String>>` | Attendance records: date, then student ID, then status |
| `String[]` | The fixed list of status options |

## Data storage

Everything is stored in a file named `attendance.dat` (Java object serialization), created in the folder where the app runs. This file contains student names and IDs, so it is **not** included in this repository.

## Possible improvements

- Export attendance reports to CSV or Excel
- Support several courses
- Search box for students
- Attendance charts
- Date picker for holidays and vacations

## Author

Wemegah Stephanie Sefakor
School of Computer Science and Engineering, Southwest University of Science and Technology
