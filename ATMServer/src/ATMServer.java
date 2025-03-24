import java.io.*;
import java.net.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ATMServer {
    private static final int PORT = 2525;
    private static final String DATA_FILE = "accounts.txt";
    private static final String LOG_FILE = "atm_server.log";
    
    private Map<String, Account> accounts;
    private PrintWriter logWriter;
    
    public ATMServer() {
        accounts = new HashMap<>();
        loadAccounts();
        initializeLogger();
    }
    
    private void loadAccounts() {
        try {
            File file = new File(DATA_FILE);
            if (!file.exists()) {
                // Create sample accounts if file doesn't exist
                createSampleAccounts();
                saveAccounts();
                return;
            }
            
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length == 3) {
                        String userId = parts[0];
                        String pin = parts[1];
                        double balance = Double.parseDouble(parts[2]);
                        accounts.put(userId, new Account(userId, pin, balance));
                    }
                }
            }
            log("Loaded " + accounts.size() + " accounts from file");
        } catch (IOException e) {
            System.err.println("Error loading accounts: " + e.getMessage());
        }
    }
    
    private void createSampleAccounts() {
        accounts.put("12345", new Account("12345", "1234", 1000.0));
        accounts.put("67890", new Account("67890", "5678", 2500.0));
        accounts.put("11111", new Account("11111", "1111", 5000.0));
        log("Created sample accounts");
    }
    
    private void saveAccounts() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(DATA_FILE))) {
            for (Account account : accounts.values()) {
                writer.println(account.getUserId() + "," + account.getPin() + "," + account.getBalance());
            }
            log("Saved accounts to file");
        } catch (IOException e) {
            System.err.println("Error saving accounts: " + e.getMessage());
            log("Error saving accounts: " + e.getMessage());
        }
    }
    
    private void initializeLogger() {
        try {
            logWriter = new PrintWriter(new FileWriter(LOG_FILE, true), true);
        } catch (IOException e) {
            System.err.println("Error initializing log file: " + e.getMessage());
        }
    }
    
    private void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String logMessage = timestamp + " - " + message;
        System.out.println(logMessage);
        if (logWriter != null) {
            logWriter.println(logMessage);
        }
    }
    
    public void start() {
        log("Server starting on port " + PORT);
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                log("New connection from " + clientSocket.getInetAddress());
                
                // Handle each client in a separate thread
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            log("Server error: " + e.getMessage());
        }
    }
    
    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String currentUserId = null;
        private boolean authenticated = false;
        
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                log("Error setting up client handler: " + e.getMessage());
            }
        }
        
        @Override
        public void run() {
            try {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    log("Received: " + inputLine);
                    processCommand(inputLine);
                }
            } catch (IOException e) {
                log("Error handling client: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                    log("Connection closed with client");
                } catch (IOException e) {
                    log("Error closing connection: " + e.getMessage());
                }
            }
        }
        
        private void processCommand(String command) {
            String[] parts = command.split(" ", 2);
            String cmd = parts[0].toUpperCase();
            
            switch (cmd) {
                case "HELO":
                    if (parts.length > 1) {
                        handleHello(parts[1]);
                    } else {
                        sendError("Missing user ID");
                    }
                    break;
                    
                case "PASS":
                    if (parts.length > 1) {
                        handlePassword(parts[1]);
                    } else {
                        sendError("Missing password");
                    }
                    break;
                    
                case "BALA":
                    if (authenticated) {
                        handleBalance();
                    } else {
                        sendError("Not authenticated");
                    }
                    break;
                    
                case "WDRA":
                    if (authenticated && parts.length > 1) {
                        handleWithdraw(parts[1]);
                    } else {
                        sendError("Not authenticated or missing amount");
                    }
                    break;
                    
                case "BYE":
                    handleBye();
                    break;
                    
                default:
                    sendError("Unknown command");
                    break;
            }
        }
        
        private void handleHello(String userId) {
            if (accounts.containsKey(userId)) {
                currentUserId = userId;
                out.println("500 sp AUTH REQUIRE");
                log("User ID valid: " + userId);
            } else {
                out.println("401 sp ERROR!");
                log("Invalid user ID attempted: " + userId);
            }
        }
        
        private void handlePassword(String password) {
            if (currentUserId != null) {
                Account account = accounts.get(currentUserId);
                if (account.getPin().equals(password)) {
                    authenticated = true;
                    out.println("525 sp OK!");
                    log("User " + currentUserId + " authenticated successfully");
                } else {
                    out.println("401 sp ERROR!");
                    log("Invalid PIN attempt for user " + currentUserId);
                }
            } else {
                out.println("401 sp ERROR!");
                log("PIN attempt without valid user ID");
            }
        }
        
        private void handleBalance() {
            Account account = accounts.get(currentUserId);
            out.println("AMNT:" + account.getBalance());
            log("Balance inquiry for user " + currentUserId + ": " + account.getBalance());
        }
        
        private void handleWithdraw(String amountStr) {
            try {
                double amount = Double.parseDouble(amountStr);
                Account account = accounts.get(currentUserId);
                
                if (account.getBalance() >= amount) {
                    account.withdraw(amount);
                    out.println("525 OK");
                    saveAccounts();  // Save updated account info
                    log("Withdrawal successful for user " + currentUserId + ": " + amount);
                } else {
                    out.println("401 sp ERROR!");
                    log("Insufficient funds for user " + currentUserId + ". Attempted: " + amount + ", Available: " + account.getBalance());
                }
            } catch (NumberFormatException e) {
                sendError("Invalid amount format");
            }
        }
        
        private void handleBye() {
            out.println("BYE");
            log("Session ended for user " + (currentUserId != null ? currentUserId : "unknown"));
            currentUserId = null;
            authenticated = false;
        }
        
        private void sendError(String message) {
            out.println("401 sp ERROR!");
            log("Error: " + message);
        }
    }
    
    public static void main(String[] args) {
        new ATMServer().start();
    }
}

class Account {
    private String userId;
    private String pin;
    private double balance;
    
    public Account(String userId, String pin, double balance) {
        this.userId = userId;
        this.pin = pin;
        this.balance = balance;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getPin() {
        return pin;
    }
    
    public double getBalance() {
        return balance;
    }
    
    public void withdraw(double amount) {
        if (amount > 0 && balance >= amount) {
            balance -= amount;
        }
    }
}
