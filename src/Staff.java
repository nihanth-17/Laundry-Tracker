/** The single built-in staff member (ID 0). */
public class Staff extends User {
    public Staff() {
        super(0, "Laundry Staff");
    }

    @Override
    public void showProfile() {
        System.out.println("Role: Staff | ID: " + getId() + " | Name: " + getName());
    }
}
