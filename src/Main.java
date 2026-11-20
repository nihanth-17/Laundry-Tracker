import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** Console menus. Uses one Scanner and nextLine() everywhere. */
public class Main {
    private static final Scanner in = new Scanner(System.in);
    private static LaundryService service;

    public static void main(String[] args) {
        FileManager files = new FileManager("data");
        try {
            files.init();
            ArrayList<Customer> customers = files.loadCustomers();     // customers first
            ArrayList<LaundryOrder> orders = files.loadOrders(customers);
            service = new LaundryService(files, customers, orders);
        } catch (IOException e) {
            System.out.println("Cannot start: " + e.getMessage());
            System.out.println("Fix the file in the data folder and run again.");
            return;
        }
        showMenu();
        System.out.println("Goodbye.");
    }

    // ---------- input helpers ----------

    private static String readLine(String prompt) {
        System.out.print(prompt);
        if (!in.hasNextLine()) {            // input closed: exit cleanly
            System.out.println();
            System.out.println("Goodbye.");
            System.exit(0);
        }
        return in.nextLine().trim();
    }

    /** Asks until the user enters a whole number from min to max. */
    private static int readInt(String prompt, int min, int max) {
        while (true) {
            String text = readLine(prompt);
            try {
                int v = Integer.parseInt(text);
                if (v >= min && v <= max) return v;
            } catch (NumberFormatException ignored) { }
            System.out.println("Invalid input. Enter a number from " + min + " to " + max + ".");
        }
    }

    /** Returns an existing customer, or null if the user presses Enter to cancel. */
    private static Customer askCustomer() {
        while (true) {
            String text = readLine("Enter customer ID (press Enter to go back): ");
            if (text.isEmpty()) return null;
            try {
                Customer c = service.findCustomer(Integer.parseInt(text));
                if (c != null) return c;
            } catch (NumberFormatException ignored) { }
            System.out.println("Customer ID not found. Try again.");
        }
    }

    private static void printOrders(List<LaundryOrder> list, boolean forCustomer) {
        if (list.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (LaundryOrder o : list) {
            if (forCustomer) {
                System.out.println("Order " + o.getId() + " | " + o.calculateCharge()
                        + " rupees | " + o.getStatus());
            } else {
                System.out.println(o.describe());
            }
        }
    }

    // ---------- menus ----------

    public static void showMenu() {
        while (true) {
            System.out.println();
            System.out.println("MAIN MENU");
            System.out.println("1. Staff menu");
            System.out.println("2. Customer menu");
            System.out.println("0. Exit");
            int choice = readInt("Choose: ", 0, 2);
            if (choice == 0) return;
            if (choice == 1) staffMenu();
            else customerMenu();
        }
    }

    private static void staffMenu() {
        Staff staff = new Staff();
        User user = staff;          // polymorphism: User reference, Staff object
        user.showProfile();
        while (true) {
            System.out.println();
            System.out.println("STAFF MENU");
            System.out.println("1. Register customer");
            System.out.println("2. View customers");
            System.out.println("3. Add laundry order");
            System.out.println("4. Update order status");
            System.out.println("5. View orders and reports");
            System.out.println("0. Back");
            int choice = readInt("Choose: ", 0, 5);
            switch (choice) {
                case 0: return;
                case 1: registerCustomer(); break;
                case 2: viewCustomers(); break;
                case 3: addOrder(); break;
                case 4: updateStatus(); break;
                default: viewOrdersAndReports(); break;
            }
        }
    }

    private static void registerCustomer() {
        String name = readLine("Customer name: ");
        String phone = readLine("Phone (10 digits): ");
        try {
            Customer c = service.registerCustomer(name, phone);
            System.out.println("Customer " + c.getId() + " saved.");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage() + " (change cancelled)");
        }
    }

    private static void viewCustomers() {
        if (service.getCustomers().isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (Customer c : service.getCustomers()) {
            System.out.println("Customer " + c.getId() + " | " + c.getName() + " | " + c.getPhone());
        }
    }

    private static void addOrder() {
        Customer c = askCustomer();
        if (c == null) return;
        int[] q = new int[3];
        for (int i = 0; i < q.length; i++) {
            q[i] = readInt(LaundryOrder.NAMES[i] + " quantity (0-" + LaundryOrder.MAX_QTY + "): ",
                    0, LaundryOrder.MAX_QTY);
        }
        try {
            LaundryOrder o = service.addOrder(c.getId(), q);
            System.out.println("Order " + o.getId() + " saved. Charge: " + o.calculateCharge()
                    + " rupees");
            System.out.println("Status: " + o.getStatus());
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage() + " (change cancelled)");
        }
    }

    private static void updateStatus() {
        if (service.getOrders().isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        printOrders(service.getOrders(), false);
        LaundryOrder order;
        while (true) {
            String text = readLine("Enter order ID (press Enter to go back): ");
            if (text.isEmpty()) return;
            try {
                order = service.findOrder(Integer.parseInt(text));
                if (order != null) break;
            } catch (NumberFormatException ignored) { }
            System.out.println("Order ID not found. Try again.");
        }
        System.out.println("Current status: " + order.getStatus());
        String next;
        while (true) {
            next = readLine("New status (PENDING, READY or COLLECTED): ").toUpperCase();
            if (LaundryOrder.isValidStatus(next)) break;
            System.out.println("Invalid status. Enter PENDING, READY or COLLECTED.");
        }
        try {
            service.updateStatus(order.getId(), next);
            System.out.println("Order " + order.getId() + " is now " + next + ".");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage() + " (change cancelled)");
        }
    }

    private static void viewOrdersAndReports() {
        printOrders(service.getOrders(), false);
        System.out.println();
        service.showReport();
        String answer = readLine("Save this summary to report.txt? (y/n): ");
        if (answer.equalsIgnoreCase("y")) {
            try {
                service.saveReport();
                System.out.println("Report saved to data/report.txt.");
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void customerMenu() {
        Customer customer = askCustomer();
        if (customer == null) return;
        while (true) {
            System.out.println();
            System.out.println("CUSTOMER MENU");
            System.out.println("1. View my profile");
            System.out.println("2. View my orders");
            System.out.println("0. Back");
            int choice = readInt("Choose: ", 0, 2);
            if (choice == 0) return;
            if (choice == 1) {
                User user = customer;       // polymorphism: Customer version runs
                user.showProfile();
            } else {
                printOrders(service.ordersForCustomer(customer.getId()), true);
            }
        }
    }
}
