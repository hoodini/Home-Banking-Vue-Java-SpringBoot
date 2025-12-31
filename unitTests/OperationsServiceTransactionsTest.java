package springbootvuejs.services.imp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import springbootvuejs.models.Account;
import springbootvuejs.models.Client;
import springbootvuejs.models.Transaction;
import springbootvuejs.models.Enums.TransactionType;
import springbootvuejs.repository.AccountRepository;
import springbootvuejs.repository.ClientRepository;
import springbootvuejs.repository.TransactionRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OperationsServiceImplements.postTransactions method
 * This is one of the most critical functions as it handles money transfers between accounts
 */
@DisplayName("Operations Service - Post Transactions Tests")
public class OperationsServiceTransactionsTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private OperationsServiceImplements operationsService;

    private Client testClient;
    private Account originAccount;
    private Account destinationAccount;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup test client
        testClient = new Client("John", "Doe", "john.doe@test.com", "password123");
        
        // Setup origin account
        originAccount = new Account("014/5301", 1000.0, testClient, LocalDateTime.now());
        
        // Setup destination account
        Client destClient = new Client("Jane", "Smith", "jane.smith@test.com", "password456");
        destinationAccount = new Account("014/5302", 500.0, destClient, LocalDateTime.now());
        
        // Add origin account to test client's accounts
        Set<Account> accounts = new HashSet<>();
        accounts.add(originAccount);
        testClient.setAccounts(accounts);
    }

    @Test
    @DisplayName("Should successfully transfer money between valid accounts")
    void testSuccessfulTransaction() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(accountRepository.findByNumber("014/5301")).thenReturn(originAccount);
        when(accountRepository.findByNumber("014/5302")).thenReturn(destinationAccount);

        double transferAmount = 200.0;
        String description = "Test transfer";

        // Act
        ResponseEntity<?> response = operationsService.postTransactions(
            authentication, 
            transferAmount, 
            description, 
            "014/5301", 
            "014/5302"
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Successful transfer ", response.getBody());
        assertEquals(800.0, originAccount.getBalance(), 0.01);
        assertEquals(700.0, destinationAccount.getBalance(), 0.01);
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should reject transaction when client is not authenticated")
    void testTransactionWithUnauthenticatedClient() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(null);

        // Act
        ResponseEntity<?> response = operationsService.postTransactions(
            authentication, 
            100.0, 
            "Test", 
            "014/5301", 
            "014/5302"
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Authenticated client is not recognized", response.getBody());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should reject transaction with invalid amount (zero or negative)")
    void testTransactionWithInvalidAmount() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);

        // Act - Test with zero amount
        ResponseEntity<?> response1 = operationsService.postTransactions(
            authentication, 
            0.0, 
            "Test", 
            "014/5301", 
            "014/5302"
        );

        // Act - Test with negative amount
        ResponseEntity<?> response2 = operationsService.postTransactions(
            authentication, 
            -100.0, 
            "Test", 
            "014/5301", 
            "014/5302"
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response1.getStatusCode());
        assertEquals("Invalid amount, please try again", response1.getBody());
        assertEquals(HttpStatus.FORBIDDEN, response2.getStatusCode());
        assertEquals("Invalid amount, please try again", response2.getBody());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should reject transaction when origin and destination accounts are the same")
    void testTransactionWithSameOriginAndDestination() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);

        // Act
        ResponseEntity<?> response = operationsService.postTransactions(
            authentication, 
            100.0, 
            "Test", 
            "014/5301", 
            "014/5301"
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Operation invalid between account origin and account destiny - Validations 1", response.getBody());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should reject transaction when destination account does not exist")
    void testTransactionWithNonExistentDestinationAccount() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(accountRepository.findByNumber("014/5301")).thenReturn(originAccount);
        when(accountRepository.findByNumber("014/9999")).thenReturn(null);

        // Act
        ResponseEntity<?> response = operationsService.postTransactions(
            authentication, 
            100.0, 
            "Test", 
            "014/5301", 
            "014/9999"
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("the destination account cannot be found, please try again", response.getBody());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should reject transaction when origin account doesn't belong to client")
    void testTransactionWithUnauthorizedOriginAccount() {
        // Arrange
        Client otherClient = new Client("Other", "User", "other@test.com", "pass");
        Account otherAccount = new Account("014/9999", 2000.0, otherClient, LocalDateTime.now());
        
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(accountRepository.findByNumber("014/9999")).thenReturn(otherAccount);
        when(accountRepository.findByNumber("014/5302")).thenReturn(destinationAccount);

        // Act
        ResponseEntity<?> response = operationsService.postTransactions(
            authentication, 
            100.0, 
            "Test", 
            "014/9999", 
            "014/5302"
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Operation invalid between account origin and account destiny - the accounts are equals", response.getBody());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should reject transaction when insufficient funds in origin account")
    void testTransactionWithInsufficientFunds() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(accountRepository.findByNumber("014/5301")).thenReturn(originAccount);
        when(accountRepository.findByNumber("014/5302")).thenReturn(destinationAccount);

        double transferAmount = 2000.0; // More than the balance

        // Act
        ResponseEntity<?> response = operationsService.postTransactions(
            authentication, 
            transferAmount, 
            "Test", 
            "014/5301", 
            "014/5302"
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Invalid amount"));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should reject transaction when account fields are empty")
    void testTransactionWithEmptyAccountFields() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);

        // Act
        ResponseEntity<?> response = operationsService.postTransactions(
            authentication, 
            100.0, 
            "Test", 
            "", 
            "014/5302"
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Please fill in all the fields of the form", response.getBody());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}
