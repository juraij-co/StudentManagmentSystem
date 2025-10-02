public class StudentAttendance {
    private final IntegerProperty studentId;
    private final StringProperty name;
    private final BooleanProperty present;

    public StudentAttendance(int id, String name) {
        this.studentId = new SimpleIntegerProperty(id);
        this.name = new SimpleStringProperty(name);
        this.present = new SimpleBooleanProperty(false);
    }

    public IntegerProperty studentIdProperty() { return studentId; }
    public StringProperty nameProperty() { return name; }
    public BooleanProperty presentProperty() { return present; }

    public boolean isPresent() { return present.get(); }
    public void setPresent(boolean value) { present.set(value); }
}
