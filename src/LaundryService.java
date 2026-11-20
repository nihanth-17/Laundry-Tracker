import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/** Project actions: customers, orders, status changes and reports. */
public class LaundryService {
    private final FileManager files;
    private final ArrayList<Customer> customers;
    private final ArrayList<LaundryOrder> orders;
    private final HashSet<String> phones = new HashSet<>();
    private int nextCustomerId = 1;
    private int nextOrderId = 1;

    public LaundryService(FileManager files, ArrayList<Customer> customers,
                          ArrayList<LaundryOrder> orders) {
        this.files = files;
        this.customers = customers;
        this.orders = orders;
        for (Customer c : customers) {          // rebuild calculated data
            phones.add(c.getPhone());
            nextCustomerId = Math.max(nextCustomerId, c.getId() + 1);
        }
        for (LaundryOrder o : orders) {
            nextOrderId = Math.max(nextOrderId, o.getId() + 1);
        }
    }

    // ---------- validation helpers (also used by FileManager) ----------

    /** Returns an error message, or null if the name is valid. */
    public static String validateName(String name) {
        if (name == null || name.trim().isEmpty()) return "Name is required.";
        if (name.contains("|") || name.contains("\n") || name.contains("\r")) {
            return "Name cannot contain '|' or line breaks.";
        }
        return null;
    }

    /** Returns an error message, or null if the phone is valid. */
    public static String validatePhone(String phone) {
        if (phone == null || !phone.matches("\\d{10}")) {
            return "Phone must contain exactly ten digits.";
        }
        return null;
    }

    // ---------- lookups ----------

    public List<Customer> getCustomers() { return Collections.unmodifiableList(customers); }
    public List<LaundryOrder> getOrders() { return Collections.unmodifiableList(orders); }

    public Customer findCustomer(int id) {
        for (Customer c : customers) if (c.getId() == id) return c;
        return null;
    }

    public LaundryOrder findOrder(int id) {
        for (LaundryOrder o : orders) if (o.getId() == id) return o;
        return null;
    }

    public List<LaundryOrder> ordersForCustomer(int customerId) {
        List<LaundryOrder> result = new ArrayList<>();
        for (LaundryOrder o : orders) if (o.getCustomerId() == customerId) result.add(o);
        return result;
    }

    // ---------- actions ----------

    /** Throws IllegalArgumentException for invalid input, IOException if saving fails. */
    public Customer registerCustomer(String name, String phone) throws IOException {
        name = name == null ? "" : name.trim();
        phone = phone == null ? "" : phone.trim();
        String error = validateName(name);
        if (error == null) error = validatePhone(phone);
        if (error != null) throw new IllegalArgumentException(error);
        if (phones.contains(phone)) {
            throw new IllegalArgumentException("Customer phone already exists.");
        }

        Customer customer = new Customer(nextCustomerId, name, phone);
        customers.add(customer);
        try {
            files.saveCustomers(customers);
        } catch (IOException e) {
            customers.remove(customer);      // cancel the change in memory
            throw e;
        }
        phones.add(phone);                   // add phone key only after saving
        nextCustomerId++;
        return customer;
    }

    public LaundryOrder addOrder(int customerId, int[] quantities) throws IOException {
        if (findCustomer(customerId) == null) {
            throw new IllegalArgumentException("Customer " + customerId + " does not exist.");
        }
        String error = LaundryOrder.validateQuantities(quantities);
        if (error != null) throw new IllegalArgumentException(error);

        LaundryOrder order = new LaundryOrder(nextOrderId, customerId, quantities,
                LaundryOrder.PENDING);
        orders.add(order);
        try {
            files.saveOrders(orders);
        } catch (IOException e) {
            orders.remove(order);
            throw e;
        }
        nextOrderId++;
        return order;
    }

    public LaundryOrder updateStatus(int orderId, String newStatus) throws IOException {
        LaundryOrder order = findOrder(orderId);
        if (order == null) throw new IllegalArgumentException("Order " + orderId + " not found.");
        String oldStatus = order.getStatus();
        if (!order.changeStatus(newStatus)) {
            throw new IllegalArgumentException("Cannot change " + oldStatus + " to " + newStatus
                    + ". Allowed: PENDING -> READY -> COLLECTED.");
        }
        try {
            files.saveOrders(orders);
        } catch (IOException e) {
            order.restoreStatus(oldStatus);
            throw e;
        }
        return order;
    }

    // ---------- reports ----------

    public List<String> buildReport() {
        int pending = 0, ready = 0, collected = 0, sales = 0;
        for (LaundryOrder o : orders) {
            switch (o.getStatus()) {
                case LaundryOrder.PENDING: pending++; break;
                case LaundryOrder.READY: ready++; break;
                case LaundryOrder.COLLECTED:
                    collected++;
                    sales += o.calculateCharge();
                    break;
                default: break;
            }
        }
        List<String> lines = new ArrayList<>();
        lines.add("Laundry Service Report");
        lines.add("Customers: " + customers.size());
        lines.add("Orders: " + orders.size());
        lines.add("PENDING orders: " + pending);
        lines.add("READY orders: " + ready);
        lines.add("COLLECTED orders: " + collected);
        lines.add("Collected sales: " + sales + " rupees");
        return lines;
    }

    public void showReport() {
        for (String line : buildReport()) System.out.println(line);
    }

    public void saveReport() throws IOException {
        files.saveReport(buildReport());
    }
}
