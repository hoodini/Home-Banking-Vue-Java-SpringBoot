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
import springbootvuejs.dtos.AccountDTO;
import springbootvuejs.models.Account;
import springbootvuejs.models.Client;
import springbootvuejs.repository.AccountRepository;
import springbootvuejs.repository.ClientRepository;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AccountServicesImplements.CreateNewAccount method
 * This is one of the most critical functions as it handles account creation with business validations
 */
@DisplayName("Account Service - Create New Account Tests")
public class AccountServiceCreateAccountTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AccountServicesImplements accountService;

    private Client testClient;
    private List<Account> allAccounts;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup test client
        testClient = new Client("John", "Doe", "john.doe@test.com", "password123");
        
        // Setup existing accounts for the client (starting with 0)
        testClient.setAccounts(new HashSet<>());
        
        // Setup all accounts in the system (simulate existing accounts)
        allAccounts = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Account acc = new Account("014/530" + i, 1000.0, testClient, LocalDateTime.now());
            allAccounts.add(acc);
        }
    }

    @Test
    @DisplayName("Should successfully create a new account for authenticated client")
    void testSuccessfulAccountCreation() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(accountRepository.findAll()).thenReturn(allAccounts);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Set<Account> updatedAccounts = new HashSet<>();
        updatedAccounts.add(new Account("014/5306", 0.0, testClient, LocalDateTime.now()));
        testClient.setAccounts(updatedAccounts);
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);

        // Act
        ResponseEntity<?> response = accountService.CreateNewAccount(authentication, true);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should reject account creation when client is not authenticated")
    void testAccountCreationUnauthenticatedClient() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(null);

        // Act
        ResponseEntity<?> response = accountService.CreateNewAccount(authentication, true);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Authenticated client is not recognized", response.getBody());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should reject account creation when client already has 3 accounts")
    void testAccountCreationMaxAccountsReached() {
        // Arrange
        Set<Account> threeAccounts = new HashSet<>();
        for (int i = 1; i <= 3; i++) {
            threeAccounts.add(new Account("014/530" + i, 1000.0, testClient, LocalDateTime.now()));
        }
        testClient.setAccounts(threeAccounts);

        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);

        // Act
        ResponseEntity<?> response = accountService.CreateNewAccount(authentication, true);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("You have a maximum accounts permitted", response.getBody());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should reject account creation when total system accounts exceed 9999")
    void testAccountCreationSystemMaxReached() {
        // Arrange
        List<Account> maxAccounts = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            maxAccounts.add(new Account("014/530" + i, 0.0, testClient, LocalDateTime.now()));
        }

        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(accountRepository.findAll()).thenReturn(maxAccounts);

        // Act
        ResponseEntity<?> response = accountService.CreateNewAccount(authentication, true);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("maximum of all accounts published ", response.getBody());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should reject account creation when createNewAccount parameter is false")
    void testAccountCreationFalseParameter() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(accountRepository.findAll()).thenReturn(allAccounts);

        // Act
        ResponseEntity<?> response = accountService.CreateNewAccount(authentication, false);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Param for create is not received ?", response.getBody());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should create account with correct initial balance of 0.00")
    void testAccountCreationInitialBalance() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(accountRepository.findAll()).thenReturn(allAccounts);

        // Act
        accountService.CreateNewAccount(authentication, true);

        // Assert
        verify(accountRepository).save(argThat(account -> 
            Math.abs(account.getBalance() - 0.0) < 0.01
        ));
    }

    @Test
    @DisplayName("Should generate sequential account numbers based on total accounts")
    void testAccountNumberGeneration() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(accountRepository.findAll()).thenReturn(allAccounts); // 5 accounts

        // Act
        accountService.CreateNewAccount(authentication, true);

        // Assert
        verify(accountRepository).save(argThat(account -> 
            account.getNumber().equals("014/5306") // Should be 5 + 1 = 6
        ));
    }

    @Test
    @DisplayName("Should allow client with 2 accounts to create a third one")
    void testAccountCreationWithTwoExistingAccounts() {
        // Arrange
        Set<Account> twoAccounts = new HashSet<>();
        for (int i = 1; i <= 2; i++) {
            twoAccounts.add(new Account("014/530" + i, 1000.0, testClient, LocalDateTime.now()));
        }
        testClient.setAccounts(twoAccounts);

        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(accountRepository.findAll()).thenReturn(allAccounts);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Create updated accounts set with the new account
        Set<Account> updatedAccounts = new HashSet<>(twoAccounts);
        Account newAccount = new Account("014/5306", 0.0, testClient, LocalDateTime.now());
        updatedAccounts.add(newAccount);
        
        // Mock second call to return updated client
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient).thenReturn(testClient);

        // Act
        ResponseEntity<?> response = accountService.CreateNewAccount(authentication, true);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should set current timestamp for account creation date")
    void testAccountCreationTimestamp() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(accountRepository.findAll()).thenReturn(allAccounts);

        LocalDateTime beforeCreation = LocalDateTime.now().minusSeconds(1);

        // Act
        accountService.CreateNewAccount(authentication, true);

        LocalDateTime afterCreation = LocalDateTime.now().plusSeconds(1);

        // Assert
        verify(accountRepository).save(argThat(account -> {
            LocalDateTime creationDate = account.getCreationDate();
            return creationDate.isAfter(beforeCreation) && creationDate.isBefore(afterCreation);
        }));
    }

    @Test
    @DisplayName("Should return account DTOs after successful creation")
    void testAccountCreationReturnsDTO() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(accountRepository.findAll()).thenReturn(allAccounts);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Set<Account> updatedAccounts = new HashSet<>();
        updatedAccounts.add(new Account("014/5306", 0.0, testClient, LocalDateTime.now()));
        testClient.setAccounts(updatedAccounts);
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);

        // Act
        ResponseEntity<?> response = accountService.CreateNewAccount(authentication, true);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof Set);
    }
}
