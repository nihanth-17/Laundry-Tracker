/** One laundry order. Quantities and customer never change after creation. */
public class LaundryOrder {
    public static final String[] NAMES = {"Wash", "Dry", "Iron"};
    public static final int[] RATES = {50, 30, 20};
    public static final int MAX_QTY = 50;

    public static final String PENDING = "PENDING";
    public static final String READY = "READY";
    public static final String COLLECTED = "COLLECTED";

    private final int id;
    private final int customerId;
    private final int[] quantities;   // index 0 = Wash, 1 = Dry, 2 = Iron
    private String status;

    public LaundryOrder(int id, int customerId, int[] quantities, String status) {
        this.id = id;
        this.customerId = customerId;
        this.quantities = quantities.clone();   // copy so outside code cannot change it
        this.status = status;
    }

    public int getId() { return id; }
    public int getCustomerId() { return customerId; }
    public int[] getQuantities() { return quantities.clone(); }
    public String getStatus() { return status; }

    public static boolean isValidStatus(String s) {
        return PENDING.equals(s) || READY.equals(s) || COLLECTED.equals(s);
    }

    /** Returns an error message, or null if the quantities are valid. */
    public static String validateQuantities(int[] q) {
        if (q == null || q.length != 3) {
            return "Exactly three quantities are required (Wash, Dry, Iron).";
        }
        boolean anyPositive = false;
        for (int i = 0; i < q.length; i++) {
            if (q[i] < 0 || q[i] > MAX_QTY) {
                return NAMES[i] + " quantity must be from 0 to " + MAX_QTY + ".";
            }
            if (q[i] > 0) anyPositive = true;
        }
        return anyPositive ? null : "At least one quantity must be positive.";
    }

    /** Sum of quantity x rate for the same index. */
    public int calculateCharge() {
        int charge = 0;
        for (int i = 0; i < quantities.length; i++) {
            charge += quantities[i] * RATES[i];
        }
        return charge;
    }

    /** Only PENDING -> READY and READY -> COLLECTED are allowed. */
    public boolean changeStatus(String newStatus) {
        boolean allowed = (PENDING.equals(status) && READY.equals(newStatus))
                || (READY.equals(status) && COLLECTED.equals(newStatus));
        if (!allowed) return false;
        status = newStatus;
        return true;
    }

    /** Used only to undo a change when saving fails. */
    void restoreStatus(String oldStatus) {
        this.status = oldStatus;
    }

    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append("Order ").append(id).append(" | Customer ").append(customerId).append(" | ");
        for (int i = 0; i < quantities.length; i++) {
            sb.append(NAMES[i]).append(' ').append(quantities[i]).append(i < 2 ? ", " : "");
        }
        sb.append(" | ").append(calculateCharge()).append(" rupees | ").append(status);
        return sb.toString();
    }

    /** Line format used in orders.txt: id|customerId|w,d,i|status */
    public String toRecord() {
        return id + "|" + customerId + "|" + quantities[0] + "," + quantities[1] + ","
                + quantities[2] + "|" + status;
    }
}
