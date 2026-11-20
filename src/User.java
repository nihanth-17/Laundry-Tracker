/** Abstract base class for everyone who uses the system. */
public abstract class User {
    private final int id;      // set once, never changed
    private String name;

    protected User(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }

    /** Each child class supplies its own version (polymorphism). */
    public abstract void showProfile();
}
