import java.sql.*;
import java.util.Scanner;

public class InventoryManagementSystem {
    // Update your database URL, username, and password as needed
    private static final String DB_URL = "jdbc:mysql://localhost:3306/retail_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Yash2312@";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Scanner scanner = new Scanner(System.in)) {

            int choice;
            do {
                System.out.println("\n--- Inventory Management System Menu ---");
                System.out.println("1. Insert Product Records (a)");
                System.out.println("2. Conditional Retrieval and Sorting (b)");
                System.out.println("3. Update Product Stock (c)");
                System.out.println("4. Search and Modify Product Details (d)");
                System.out.println("5. Aggregate Query and Conditional Deletion (e)");
                System.out.println("6. Exit");
                System.out.print("Enter your choice: ");
                choice = scanner.nextInt();

                switch (choice) {
                    case 1:
                        insertProducts(conn, scanner);
                        break;
                    case 2:
                        retrieveAndSortProducts(conn, scanner);
                        break;
                    case 3:
                        updateStock(conn, scanner);
                        break;
                    case 4:
                        modifyProductPrice(conn, scanner);
                        break;
                    case 5:
                        aggregateAndDelete(conn, scanner);
                        break;
                    case 6:
                        System.out.println("Exiting application. Goodbye!");
                        break;
                    default:
                        System.out.println("Invalid choice! Please try again.");
                }
            } while (choice != 6);

        } catch (SQLException e) {
            System.err.println("Database Connection Error: " + e.getMessage());
        }
    }

    // (a) Insert Product Records using PreparedStatement and handle duplicate IDs
    private static void insertProducts(Connection conn, Scanner scanner) {
        String sql = "INSERT INTO Product (product_id, product_name, category, quantity, price) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            System.out.print("Enter number of products to insert (at least 3): ");
            int n = scanner.nextInt();

            for (int i = 0; i < n; i++) {
                System.out.println("\nEntering details for product " + (i + 1) + ":");
                System.out.print("Product ID: ");
                int id = scanner.nextInt();
                scanner.nextLine(); // consume newline
                System.out.print("Product Name: ");
                String name = scanner.nextLine();
                System.out.print("Category: ");
                String category = scanner.nextLine();
                System.out.print("Quantity: ");
                int qty = scanner.nextInt();
                System.out.print("Price: ");
                double price = scanner.nextDouble();

                pstmt.setInt(1, id);
                pstmt.setString(2, name);
                pstmt.setString(3, category);
                pstmt.setInt(4, qty);
                pstmt.setDouble(5, price);

                try {
                    pstmt.executeUpdate();
                    System.out.println("Product inserted successfully.");
                } catch (SQLException e) {
                    // Handling duplicate primary key exception (SQL State 23000 typically represents integrity constraint violation)
                    if (e.getSQLState().startsWith("23")) {
                        System.out.println("Error: Product ID " + id + " already exists. Insertion skipped.");
                    } else {
                        System.out.println("Error inserting record: " + e.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e.getMessage());
        }
    }

    // (b) Conditional Retrieval and Sorting (qty < 20, sorted by price DESC)
    private static void retrieveAndSortProducts(Connection conn, Scanner scanner) {
        scanner.nextLine(); // consume newline
        System.out.print("Enter category to search: ");
        String category = scanner.nextLine();

        String sql = "SELECT * FROM Product WHERE category = ? AND quantity < 20 ORDER BY price DESC";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\n--- Products in Category '" + category + "' (Quantity < 20, Sorted by Price DESC) ---");
            System.out.printf("%-10s %-20s %-15s %-10s %-10s\n", "ID", "Name", "Category", "Quantity", "Price");
            System.out.println("------------------------------------------------------------------");

            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%-10d %-20s %-15s %-10d %-10.2f\n",
                        rs.getInt("product_id"),
                        rs.getString("product_name"),
                        rs.getString("category"),
                        rs.getInt("quantity"),
                        rs.getDouble("price"));
            }
            if (!found) {
                System.out.println("No products found matching the criteria.");
            }
        } catch (SQLException e) {
            System.out.println("Query Error: " + e.getMessage());
        }
    }

    // (c) Update Product Stock by adding to existing quantity
    private static void updateStock(Connection conn, Scanner scanner) {
        System.out.print("Enter Product ID to update stock: ");
        int id = scanner.nextInt();
        System.out.print("Enter quantity to add: ");
        int addQty = scanner.nextInt();

        String checkSql = "SELECT product_id FROM Product WHERE product_id = ?";
        String updateSql = "UPDATE Product SET quantity = quantity + ? WHERE product_id = ?";

        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql);
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {

            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                updateStmt.setInt(1, addQty);
                updateStmt.setInt(2, id);
                updateStmt.executeUpdate();
                System.out.println("Stock updated successfully for Product ID: " + id);
            } else {
                System.out.println("Error: Product with ID " + id + " does not exist.");
            }
        } catch (SQLException e) {
            System.out.println("Update Error: " + e.getMessage());
        }
    }

    // (d) Search and Modify Product Details (update price if > 0)
    private static void modifyProductPrice(Connection conn, Scanner scanner) {
        System.out.print("Enter Product ID to modify price: ");
        int id = scanner.nextInt();
        System.out.print("Enter new price: ");
        double newPrice = scanner.nextDouble();

        if (newPrice <= 0) {
            System.out.println("Error: New price must be greater than zero.");
            return;
        }

        String selectSql = "SELECT * FROM Product WHERE product_id = ?";
        String updateSql = "UPDATE Product SET price = ? WHERE product_id = ?";

        try (PreparedStatement selectStmt = conn.prepareStatement(selectSql);
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {

            selectStmt.setInt(1, id);
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                // Product exists, update price
                updateStmt.setDouble(1, newPrice);
                updateStmt.setInt(2, id);
                updateStmt.executeUpdate();

                // Retrieve and display updated details
                ResultSet updatedRs = selectStmt.executeQuery();
                if (updatedRs.next()) {
                    System.out.println("\n--- Updated Product Details ---");
                    System.out.println("ID: " + updatedRs.getInt("product_id"));
                    System.out.println("Name: " + updatedRs.getString("product_name"));
                    System.out.println("Category: " + updatedRs.getString("category"));
                    System.out.println("Quantity: " + updatedRs.getInt("quantity"));
                    System.out.println("Price: " + updatedRs.getDouble("price"));
                }
            } else {
                System.out.println("Error: Product with ID " + id + " does not exist.");
            }
        } catch (SQLException e) {
            System.out.println("Error modifying product: " + e.getMessage());
        }
    }

    // (e) Aggregate Query and Conditional Deletion (delete only if quantity is 0)
    private static void aggregateAndDelete(Connection conn, Scanner scanner) {
        String aggSql = "SELECT SUM(quantity * price) AS total_inventory_value FROM Product";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(aggSql)) {

            if (rs.next()) {
                double totalValue = rs.getDouble("total_inventory_value");
                System.out.println("\nTotal Inventory Value: " + totalValue);
            }
        } catch (SQLException e) {
            System.out.println("Aggregate Query Error: " + e.getMessage());
        }

        System.out.print("Enter Product ID to delete: ");
        int id = scanner.nextInt();

        String checkSql = "SELECT quantity FROM Product WHERE product_id = ?";
        String deleteSql = "DELETE FROM Product WHERE product_id = ?";

        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql);
             PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {

            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                int qty = rs.getInt("quantity");
                if (qty == 0) {
                    deleteStmt.setInt(1, id);
                    deleteStmt.executeUpdate();
                    System.out.println("Product with ID " + id + " has been successfully deleted.");
                } else {
                    System.out.println("Deletion failed: Product quantity is " + qty + " (must be 0 to delete).");
                }
            } else {
                System.out.println("Error: Product with ID " + id + " does not exist.");
            }
        } catch (SQLException e) {
            System.out.println("Deletion Error: " + e.getMessage());
        }
    }
}