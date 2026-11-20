import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Reads and writes the project text files. */
public class FileManager {
    private final Path dir;

    public FileManager(String folder) {
        this.dir = Paths.get(folder);
    }

    public void init() throws IOException {
        Files.createDirectories(dir);
    }

    // ---------- loading ----------

    private List<String> readLines(String fileName) throws IOException {
        Path path = dir.resolve(fileName);
        List<String> lines = new ArrayList<>();
        if (!Files.exists(path)) return lines;      // first run: empty list
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) lines.add(line);
        } catch (IOException e) {
            throw new IOException("Cannot read " + fileName + ": " + e.getMessage(), e);
        }
        return lines;
    }

    private static IOException bad(String file, int lineNo, String why) {
        return new IOException(file + " line " + lineNo + ": " + why);
    }

    private static int parsePositiveId(String text, String file, int lineNo) throws IOException {
        try {
            int v = Integer.parseInt(text.trim());
            if (v <= 0) throw bad(file, lineNo, "ID must be positive.");
            return v;
        } catch (NumberFormatException e) {
            throw bad(file, lineNo, "ID is not a valid number: " + text);
        }
    }

    public ArrayList<Customer> loadCustomers() throws IOException {
        final String file = "customers.txt";
        ArrayList<Customer> result = new ArrayList<>();   // temporary list
        HashSet<Integer> ids = new HashSet<>();
        HashSet<String> phones = new HashSet<>();
        List<String> lines = readLines(file);
        for (int i = 0; i < lines.size(); i++) {
            int lineNo = i + 1;
            String line = lines.get(i);
            if (line.trim().isEmpty()) continue;
            String[] f = line.split("\\|", -1);
            if (f.length != 3) throw bad(file, lineNo, "expected 3 fields but found " + f.length);
            int id = parsePositiveId(f[0], file, lineNo);
            if (!ids.add(id)) throw bad(file, lineNo, "duplicate ID " + id);
            String name = f[1].trim();
            String nameError = LaundryService.validateName(name);
            if (nameError != null) throw bad(file, lineNo, nameError);
            String phone = f[2].trim();
            String phoneError = LaundryService.validatePhone(phone);
            if (phoneError != null) throw bad(file, lineNo, phoneError);
            if (!phones.add(phone)) throw bad(file, lineNo, "duplicate phone " + phone);
            result.add(new Customer(id, name, phone));
        }
        return result;
    }

    public ArrayList<LaundryOrder> loadOrders(List<Customer> customers) throws IOException {
        final String file = "orders.txt";
        HashSet<Integer> customerIds = new HashSet<>();
        for (Customer c : customers) customerIds.add(c.getId());

        ArrayList<LaundryOrder> result = new ArrayList<>();
        HashSet<Integer> ids = new HashSet<>();
        List<String> lines = readLines(file);
        for (int i = 0; i < lines.size(); i++) {
            int lineNo = i + 1;
            String line = lines.get(i);
            if (line.trim().isEmpty()) continue;
            String[] f = line.split("\\|", -1);
            if (f.length != 4) throw bad(file, lineNo, "expected 4 fields but found " + f.length);
            int id = parsePositiveId(f[0], file, lineNo);
            if (!ids.add(id)) throw bad(file, lineNo, "duplicate ID " + id);
            int customerId = parsePositiveId(f[1], file, lineNo);
            if (!customerIds.contains(customerId)) {
                throw bad(file, lineNo, "unknown customer ID " + customerId);
            }
            String[] q = f[2].split(",", -1);
            if (q.length != 3) throw bad(file, lineNo, "expected exactly three quantities");
            int[] quantities = new int[3];
            for (int k = 0; k < 3; k++) {
                try {
                    quantities[k] = Integer.parseInt(q[k].trim());
                } catch (NumberFormatException e) {
                    throw bad(file, lineNo, "quantity is not a valid integer: " + q[k]);
                }
            }
            String qError = LaundryOrder.validateQuantities(quantities);
            if (qError != null) throw bad(file, lineNo, qError);
            String status = f[3].trim();
            if (!LaundryOrder.isValidStatus(status)) {
                throw bad(file, lineNo, "invalid status: " + status);
            }
            result.add(new LaundryOrder(id, customerId, quantities, status));
        }
        return result;
    }

    // ---------- saving ----------

    /** Writes to a temp file first, then replaces the original. */
    private void writeLines(String fileName, List<String> lines) throws IOException {
        Path target = dir.resolve(fileName);
        Path temp = dir.resolve(fileName + ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
            throw new IOException("Cannot save " + fileName + ": " + e.getMessage(), e);
        }
    }

    public void saveCustomers(List<Customer> customers) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Customer c : customers) lines.add(c.toRecord());
        writeLines("customers.txt", lines);
    }

    public void saveOrders(List<LaundryOrder> orders) throws IOException {
        List<String> lines = new ArrayList<>();
        for (LaundryOrder o : orders) lines.add(o.toRecord());
        writeLines("orders.txt", lines);
    }

    public void saveReport(List<String> reportLines) throws IOException {
        writeLines("report.txt", reportLines);
    }
}
