/** A registered laundry customer. Phone is a String to keep leading zeros. */
public class Customer extends User {
    private final String phone;

    public Customer(int id, String name, String phone) {
        super(id, name);
        this.phone = phone;
    }

    public String getPhone() { return phone; }

    @Override
    public void showProfile() {
        System.out.println("Role: Customer | ID: " + getId() + " | Name: " + getName()
                + " | Phone: " + phone);
    }

    /** Line format used in customers.txt: id|name|phone */
    public String toRecord() {
        return getId() + "|" + getName() + "|" + phone;
    }
}
