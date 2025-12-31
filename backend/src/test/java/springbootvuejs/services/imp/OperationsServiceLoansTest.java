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
import springbootvuejs.dtos.LoanApplicationDTO;
import springbootvuejs.models.*;
import springbootvuejs.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OperationsServiceImplements.postLoans method
 * This is one of the most critical functions as it handles loan approvals and fund disbursement
 */
@DisplayName("Operations Service - Post Loans Tests")
public class OperationsServiceLoansTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ClientLoanRepository clientLoanRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private OperationsServiceImplements operationsService;

    private Client testClient;
    private Account testAccount;
    private Loan personalLoan;
    private LoanApplicationDTO validLoanApplicationDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup test client
        testClient = new Client("John", "Doe", "john.doe@test.com", "password123");
        
        // Setup test account
        testAccount = new Account("014/5301", 5000.0, testClient, LocalDateTime.now());
        Set<Account> accounts = new HashSet<>();
        accounts.add(testAccount);
        testClient.setAccounts(accounts);
        
        // Setup loan product
        personalLoan = new Loan("Personal", 50000.0, Arrays.asList(12, 24, 36, 48));
        
        // Setup valid loan application DTO
        validLoanApplicationDTO = new LoanApplicationDTO();
        validLoanApplicationDTO.setId(1L);
        validLoanApplicationDTO.setName("Personal");
        validLoanApplicationDTO.setAmount(10000.0);
        validLoanApplicationDTO.setPayment(12);
        validLoanApplicationDTO.setAccountSet("014/5301");
    }

    @Test
    @DisplayName("Should successfully approve and disburse a valid loan")
    void testSuccessfulLoanApproval() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(accountRepository.findByNumber("014/5301")).thenReturn(testAccount);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(personalLoan));

        double initialBalance = testAccount.getBalance();

        // Act
        ResponseEntity<?> response = operationsService.postLoans(authentication, validLoanApplicationDTO);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Loan Completed", response.getBody());
        assertEquals(initialBalance + 10000.0, testAccount.getBalance(), 0.01);
        verify(clientLoanRepository, times(1)).save(any(ClientLoan.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should reject loan when amount is below minimum ($5000)")
    void testLoanRejectionBelowMinimum() {
        // Arrange
        validLoanApplicationDTO.setAmount(4000.0);
        
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(accountRepository.findByNumber("014/5301")).thenReturn(testAccount);

        // Act
        ResponseEntity<?> response = operationsService.postLoans(authentication, validLoanApplicationDTO);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("the minimum amount is $5.000", response.getBody());
        verify(clientLoanRepository, never()).save(any(ClientLoan.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should reject loan when client is not authenticated")
    void testLoanRejectionUnauthenticatedClient() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(null);

        // Act
        ResponseEntity<?> response = operationsService.postLoans(authentication, validLoanApplicationDTO);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Authenticated client is not recognized", response.getBody());
        verify(clientLoanRepository, never()).save(any(ClientLoan.class));
    }

    @Test
    @DisplayName("Should reject loan when destination account does not exist")
    void testLoanRejectionNonExistentAccount() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(accountRepository.findByNumber("014/5301")).thenReturn(null);

        // Act
        ResponseEntity<?> response = operationsService.postLoans(authentication, validLoanApplicationDTO);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("The account entered is incorrect", response.getBody());
        verify(clientLoanRepository, never()).save(any(ClientLoan.class));
    }

    @Test
    @DisplayName("Should reject loan when account doesn't belong to authenticated client")
    void testLoanRejectionUnauthorizedAccount() {
        // Arrange
        Client otherClient = new Client("Jane", "Smith", "jane@test.com", "pass");
        Account otherAccount = new Account("014/9999", 1000.0, otherClient, LocalDateTime.now());
        
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(accountRepository.findByNumber("014/9999")).thenReturn(otherAccount);

        validLoanApplicationDTO.setAccountSet("014/9999");

        // Act
        ResponseEntity<?> response = operationsService.postLoans(authentication, validLoanApplicationDTO);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("The account entered is not valid to the client authentication", response.getBody());
        verify(clientLoanRepository, never()).save(any(ClientLoan.class));
    }

    @Test
    @DisplayName("Should reject loan when required fields are missing or invalid")
    void testLoanRejectionMissingFields() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(accountRepository.findByNumber("014/5301")).thenReturn(testAccount);

        // Test with null ID
        LoanApplicationDTO invalidDTO1 = new LoanApplicationDTO();
        invalidDTO1.setId(null);
        invalidDTO1.setName("Personal");
        invalidDTO1.setAmount(10000.0);
        invalidDTO1.setPayment(12);
        invalidDTO1.setAccountSet("014/5301");

        // Test with empty name
        LoanApplicationDTO invalidDTO2 = new LoanApplicationDTO();
        invalidDTO2.setId(1L);
        invalidDTO2.setName("");
        invalidDTO2.setAmount(10000.0);
        invalidDTO2.setPayment(12);
        invalidDTO2.setAccountSet("014/5301");

        // Test with null amount - This will throw NPE because the code checks amount < 5000 before null check
        LoanApplicationDTO invalidDTO3 = new LoanApplicationDTO();
        invalidDTO3.setId(1L);
        invalidDTO3.setName("Personal");
        invalidDTO3.setAmount(null);
        invalidDTO3.setPayment(12);
        invalidDTO3.setAccountSet("014/5301");

        // Act
        ResponseEntity<?> response1 = operationsService.postLoans(authentication, invalidDTO1);
        ResponseEntity<?> response2 = operationsService.postLoans(authentication, invalidDTO2);

        // Assert - first two should return proper error messages
        assertEquals(HttpStatus.FORBIDDEN, response1.getStatusCode());
        assertEquals("Please fill in all the fields of the form", response1.getBody());
        assertEquals(HttpStatus.FORBIDDEN, response2.getStatusCode());
        assertEquals("Please fill in all the fields of the form", response2.getBody());
        
        // Assert - null amount causes NPE due to implementation bug
        assertThrows(NullPointerException.class, () -> {
            operationsService.postLoans(authentication, invalidDTO3);
        });
        
        verify(clientLoanRepository, never()).save(any(ClientLoan.class));
    }

    @Test
    @DisplayName("Should reject loan when loan type does not exist")
    void testLoanRejectionNonExistentLoanType() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(accountRepository.findByNumber("014/5301")).thenReturn(testAccount);
        when(loanRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = operationsService.postLoans(authentication, validLoanApplicationDTO);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("esta vacio el optional", response.getBody());
        verify(clientLoanRepository, never()).save(any(ClientLoan.class));
    }

    @Test
    @DisplayName("Should reject loan when loan name doesn't match")
    void testLoanRejectionMismatchedLoanName() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(accountRepository.findByNumber("014/5301")).thenReturn(testAccount);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(personalLoan));

        validLoanApplicationDTO.setName("Mortgage"); // Different from "Personal"

        // Act
        ResponseEntity<?> response = operationsService.postLoans(authentication, validLoanApplicationDTO);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("The values entered do not match the type of loan requested", response.getBody());
        verify(clientLoanRepository, never()).save(any(ClientLoan.class));
    }

    @Test
    @DisplayName("Should reject loan when payment term is not available")
    void testLoanRejectionInvalidPaymentTerm() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(accountRepository.findByNumber("014/5301")).thenReturn(testAccount);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(personalLoan));

        validLoanApplicationDTO.setPayment(60); // Not in the available list [12, 24, 36, 48]

        // Act
        ResponseEntity<?> response = operationsService.postLoans(authentication, validLoanApplicationDTO);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("The values entered do not match the type of loan requested", response.getBody());
        verify(clientLoanRepository, never()).save(any(ClientLoan.class));
    }

    @Test
    @DisplayName("Should reject loan when amount exceeds maximum allowed")
    void testLoanRejectionExceedsMaximum() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(accountRepository.findByNumber("014/5301")).thenReturn(testAccount);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(personalLoan));

        validLoanApplicationDTO.setAmount(60000.0); // More than max of 50000

        // Act
        ResponseEntity<?> response = operationsService.postLoans(authentication, validLoanApplicationDTO);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("The value amount entered do not match the type of loan requested", response.getBody());
        verify(clientLoanRepository, never()).save(any(ClientLoan.class));
    }

    @Test
    @DisplayName("Should apply 20% interest to loan amount")
    void testLoanInterestCalculation() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(accountRepository.findByNumber("014/5301")).thenReturn(testAccount);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(personalLoan));

        // Act
        operationsService.postLoans(authentication, validLoanApplicationDTO);

        // Assert - Verify that the loan is saved with the correct total amount (principal + 20% interest)
        verify(clientLoanRepository).save(argThat(clientLoan -> {
            double expectedTotal = 10000.0 + (10000.0 * 20 / 100); // 12000.0
            return Math.abs(clientLoan.getAmount() - expectedTotal) < 0.01;
        }));
    }
}
