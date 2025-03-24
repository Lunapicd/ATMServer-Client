import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class ATMClient extends JFrame {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    
    private JPanel cards;
    private CardLayout cardLayout;
    
    private JPanel welcomePanel;
    private JPanel pinPanel;
    private JPanel menuPanel;
    private JPanel balancePanel;
    private JPanel withdrawPanel;
    private JLabel statusLabel;
    private JPasswordField pinField;
    private JLabel balanceLabel;
    private JTextField withdrawAmountField;
    
    private String userId;
    
    public ATMClient() {
        super("ATM Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        
        // Initialize card layout
        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);
        
        // Create panels
        createWelcomePanel();
        createPinPanel();
        createMenuPanel();
        createBalancePanel();
        createWithdrawPanel();
        
        // Add panels to card layout
        cards.add(welcomePanel, "WELCOME");
        cards.add(pinPanel, "PIN");
        cards.add(menuPanel, "MENU");
        cards.add(balancePanel, "BALANCE");
        cards.add(withdrawPanel, "WITHDRAW");
        
        // Add status label
        statusLabel = new JLabel("Welcome to ATM", JLabel.CENTER);
        statusLabel.setForeground(Color.BLUE);
        
        // Set up main layout
        setLayout(new BorderLayout());
        add(cards, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        
        // Show welcome panel initially
        cardLayout.show(cards, "WELCOME");
    }
    
    private void createWelcomePanel() {
        welcomePanel = new JPanel(new BorderLayout());
        
        JLabel welcomeLabel = new JLabel("Welcome to the ATM", JLabel.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 20));
        
        JPanel inputPanel = new JPanel(new FlowLayout());
        JLabel userIdLabel = new JLabel("User ID:");
        JTextField userIdField = new JTextField(10);
        JButton insertCardButton = new JButton("Insert Card");
        
        insertCardButton.addActionListener(e -> {
            userId = userIdField.getText().trim();
            if (userId.isEmpty()) {
                statusLabel.setText("Please enter a user ID");
                return;
            }
            
            try {
                connectToServer();
                sendHelloMessage(userId);
                cardLayout.show(cards, "PIN");
                statusLabel.setText("Please enter your PIN");
            } catch (IOException ex) {
                statusLabel.setText("Error connecting to server: " + ex.getMessage());
            }
        });
        
        inputPanel.add(userIdLabel);
        inputPanel.add(userIdField);
        inputPanel.add(insertCardButton);
        
        welcomePanel.add(welcomeLabel, BorderLayout.NORTH);
        welcomePanel.add(inputPanel, BorderLayout.CENTER);
    }
    
    private void createPinPanel() {
        pinPanel = new JPanel();
        pinPanel.setLayout(new BoxLayout(pinPanel, BoxLayout.Y_AXIS));
        
        JLabel instructionLabel = new JLabel("Enter your PIN:");
        instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        instructionLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        pinField = new JPasswordField(4);
        pinField.setMaximumSize(new Dimension(100, 25));
        pinField.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JButton submitButton = new JButton("Submit");
        submitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        submitButton.addActionListener(e -> validatePin());
        
        // Add components with some spacing
        pinPanel.add(Box.createVerticalGlue());
        pinPanel.add(instructionLabel);
        pinPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        pinPanel.add(pinField);
        pinPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        pinPanel.add(submitButton);
        pinPanel.add(Box.createVerticalGlue());
    }
    
    private void createMenuPanel() {
        menuPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        menuPanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));
        
        JButton balanceButton = new JButton("Check Balance");
        JButton withdrawButton = new JButton("Withdraw Money");
        JButton exitButton = new JButton("Exit");
        
        balanceButton.addActionListener(e -> checkBalance());
        withdrawButton.addActionListener(e -> cardLayout.show(cards, "WITHDRAW"));
        exitButton.addActionListener(e -> exit());
        
        menuPanel.add(balanceButton);
        menuPanel.add(withdrawButton);
        menuPanel.add(exitButton);
    }
    
    private void createBalancePanel() {
        balancePanel = new JPanel();
        balancePanel.setLayout(new BoxLayout(balancePanel, BoxLayout.Y_AXIS));
        
        JLabel titleLabel = new JLabel("Your Balance");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        balanceLabel = new JLabel("");
        balanceLabel.setFont(new Font("Arial", Font.BOLD, 20));
        balanceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JButton backButton = new JButton("Back to Menu");
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.addActionListener(e -> cardLayout.show(cards, "MENU"));
        
        balancePanel.add(Box.createVerticalGlue());
        balancePanel.add(titleLabel);
        balancePanel.add(Box.createRigidArea(new Dimension(0, 20)));
        balancePanel.add(balanceLabel);
        balancePanel.add(Box.createRigidArea(new Dimension(0, 20)));
        balancePanel.add(backButton);
        balancePanel.add(Box.createVerticalGlue());
    }
    
    private void createWithdrawPanel() {
        withdrawPanel = new JPanel();
        withdrawPanel.setLayout(new BoxLayout(withdrawPanel, BoxLayout.Y_AXIS));
        
        JLabel titleLabel = new JLabel("Withdraw Money");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel amountLabel = new JLabel("Enter amount:");
        amountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        withdrawAmountField = new JTextField(10);
        withdrawAmountField.setMaximumSize(new Dimension(150, 25));
        withdrawAmountField.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton withdrawButton = new JButton("Withdraw");
        JButton cancelButton = new JButton("Cancel");
        
        withdrawButton.addActionListener(e -> withdraw());
        cancelButton.addActionListener(e -> cardLayout.show(cards, "MENU"));
        
        buttonPanel.add(withdrawButton);
        buttonPanel.add(cancelButton);
        
        withdrawPanel.add(Box.createVerticalGlue());
        withdrawPanel.add(titleLabel);
        withdrawPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        withdrawPanel.add(amountLabel);
        withdrawPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        withdrawPanel.add(withdrawAmountField);
        withdrawPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        withdrawPanel.add(buttonPanel);
        withdrawPanel.add(Box.createVerticalGlue());
    }
    
    private void connectToServer() throws IOException {
        socket = new Socket("localhost", 2525);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }
    
    private void sendHelloMessage(String userId) throws IOException {
        out.println("HELO " + userId);
        String response = in.readLine();
        if (!response.startsWith("500")) {
            throw new IOException("Unexpected response: " + response);
        }
    }
    
    private void validatePin() {
        String pin = new String(pinField.getPassword());
        if (pin.isEmpty()) {
            statusLabel.setText("Please enter your PIN");
            return;
        }
        
        try {
            out.println("PASS " + pin);
            String response = in.readLine();
            
            if (response.startsWith("525")) {
                cardLayout.show(cards, "MENU");
                statusLabel.setText("Authentication successful");
            } else if (response.startsWith("401")) {
                statusLabel.setText("Invalid PIN. Please try again.");
                pinField.setText("");
            } else {
                statusLabel.setText("Unexpected response: " + response);
            }
        } catch (IOException e) {
            statusLabel.setText("Error communicating with server: " + e.getMessage());
        }
    }
    
    private void checkBalance() {
        try {
            out.println("BALA");
            String response = in.readLine();
            
            if (response.startsWith("AMNT:")) {
                String amount = response.substring(5);
                balanceLabel.setText("$" + amount);
                cardLayout.show(cards, "BALANCE");
                statusLabel.setText("Balance retrieved successfully");
            } else {
                statusLabel.setText("Unexpected response: " + response);
            }
        } catch (IOException e) {
            statusLabel.setText("Error communicating with server: " + e.getMessage());
        }
    }
    
    private void withdraw() {
        String amountStr = withdrawAmountField.getText().trim();
        if (amountStr.isEmpty()) {
            statusLabel.setText("Please enter an amount");
            return;
        }
        
        try {
            double amount = Double.parseDouble(amountStr);
            out.println("WDRA " + amount);
            String response = in.readLine();
            
            if (response.startsWith("525")) {
                JOptionPane.showMessageDialog(this, 
                    "Successfully withdrew $" + amount, 
                    "Withdrawal Successful", 
                    JOptionPane.INFORMATION_MESSAGE);
                cardLayout.show(cards, "MENU");
                statusLabel.setText("Please take your cash");
                withdrawAmountField.setText("");
            } else if (response.startsWith("401")) {
                JOptionPane.showMessageDialog(this, 
                    "Insufficient funds", 
                    "Withdrawal Failed", 
                    JOptionPane.ERROR_MESSAGE);
                statusLabel.setText("Insufficient funds for withdrawal");
            } else {
                statusLabel.setText("Unexpected response: " + response);
            }
        } catch (NumberFormatException e) {
            statusLabel.setText("Please enter a valid amount");
        } catch (IOException e) {
            statusLabel.setText("Error communicating with server: " + e.getMessage());
        }
    }
    
    private void exit() {
        try {
            out.println("BYE");
            String response = in.readLine();
            if (response.equals("BYE")) {
                socket.close();
                cardLayout.show(cards, "WELCOME");
                statusLabel.setText("Session ended. Thank you!");
            }
        } catch (IOException e) {
            statusLabel.setText("Error communicating with server: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ATMClient().setVisible(true);
        });
    }
}