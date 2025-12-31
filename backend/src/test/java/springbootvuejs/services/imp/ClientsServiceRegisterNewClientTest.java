package springbootvuejs.services.imp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import springbootvuejs.models.Account;
import springbootvuejs.models.Client;
import springbootvuejs.repository.AccountRepository;
import springbootvuejs.repository.ClientRepository;
import springbootvuejs.services.ServiceAccounts;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ClientsImplements.registerNewClient method
 * This is one of the most critical functions as it handles new client registration and initial account setup
 */
@DisplayName("Clients Service - Register New Client Tests")
public class ClientsServiceRegisterNewClientTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ServiceAccounts serviceAccounts;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ClientsImplements clientsService;

    private List<Account> existingAccounts;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup existing accounts
        existingAccounts = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Client tempClient = new Client("Test", "User" + i, "test" + i + "@test.com", "pass");
            existingAccounts.add(new Account("014/530" + i, 1000.0, tempClient, LocalDateTime.now()));
        }
    }

    @Test
    @DisplayName("Should successfully register a new client with valid data")
    void testSuccessfulClientRegistration() {
        // Arrange
        String firstName = "John";
        String lastName = "Doe";
        String email = "john.doe@test.com";
        String password = "SecurePass123";

        Client savedClient = new Client(firstName, lastName, email, "encoded_password");
        
        when(clientRepository.findByEmail(email)).thenReturn(null);
        when(passwordEncoder.encode(password)).thenReturn("encoded_password");
        when(clientRepository.save(any(Client.class))).thenReturn(savedClient);
        when(serviceAccounts.registerNewAccount(any(Client.class))).thenReturn(true);

        // Act
        ResponseEntity<Object> response = clientsService.registerNewClient(firstName, lastName, email, password);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(clientRepository, times(1)).save(any(Client.class));
        verify(passwordEncoder, times(1)).encode(password);
        verify(serviceAccounts, times(1)).registerNewAccount(any(Client.class));
    }

    @Test
    @DisplayName("Should reject registration when first name is empty")
    void testRegistrationWithEmptyFirstName() {
        // Arrange
        String firstName = "";
        String lastName = "Doe";
        String email = "john.doe@test.com";
        String password = "SecurePass123";

        // Act
        ResponseEntity<Object> response = clientsService.registerNewClient(firstName, lastName, email, password);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Missing data", response.getBody());
        verify(clientRepository, never()).save(any(Client.class));
        verify(serviceAccounts, never()).registerNewAccount(any(Client.class));
    }

    @Test
    @DisplayName("Should reject registration when last name is empty")
    void testRegistrationWithEmptyLastName() {
        // Arrange
        String firstName = "John";
        String lastName = "";
        String email = "john.doe@test.com";
        String password = "SecurePass123";

        // Act
        ResponseEntity<Object> response = clientsService.registerNewClient(firstName, lastName, email, password);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Missing data", response.getBody());
        verify(clientRepository, never()).save(any(Client.class));
        verify(serviceAccounts, never()).registerNewAccount(any(Client.class));
    }

    @Test
    @DisplayName("Should reject registration when email is empty")
    void testRegistrationWithEmptyEmail() {
        // Arrange
        String firstName = "John";
        String lastName = "Doe";
        String email = "";
        String password = "SecurePass123";

        // Act
        ResponseEntity<Object> response = clientsService.registerNewClient(firstName, lastName, email, password);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Missing data", response.getBody());
        verify(clientRepository, never()).save(any(Client.class));
        verify(serviceAccounts, never()).registerNewAccount(any(Client.class));
    }

    @Test
    @DisplayName("Should reject registration when password is empty")
    void testRegistrationWithEmptyPassword() {
        // Arrange
        String firstName = "John";
        String lastName = "Doe";
        String email = "john.doe@test.com";
        String password = "";

        // Act
        ResponseEntity<Object> response = clientsService.registerNewClient(firstName, lastName, email, password);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Missing data", response.getBody());
        verify(clientRepository, never()).save(any(Client.class));
        verify(serviceAccounts, never()).registerNewAccount(any(Client.class));
    }

    @Test
    @DisplayName("Should reject registration when all fields are empty")
    void testRegistrationWithAllEmptyFields() {
        // Arrange
        String firstName = "";
        String lastName = "";
        String email = "";
        String password = "";

        // Act
        ResponseEntity<Object> response = clientsService.registerNewClient(firstName, lastName, email, password);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Missing data", response.getBody());
        verify(clientRepository, never()).save(any(Client.class));
        verify(serviceAccounts, never()).registerNewAccount(any(Client.class));
    }

    @Test
    @DisplayName("Should reject registration when email is already in use")
    void testRegistrationWithDuplicateEmail() {
        // Arrange
        String firstName = "John";
        String lastName = "Doe";
        String email = "existing@test.com";
        String password = "SecurePass123";

        Client existingClient = new Client("Jane", "Smith", email, "old_password");
        when(clientRepository.findByEmail(email)).thenReturn(existingClient);

        // Act
        ResponseEntity<Object> response = clientsService.registerNewClient(firstName, lastName, email, password);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Name already in use", response.getBody());
        verify(clientRepository, never()).save(any(Client.class));
        verify(serviceAccounts, never()).registerNewAccount(any(Client.class));
    }

    @Test
    @DisplayName("Should encode password before saving client")
    void testPasswordEncoding() {
        // Arrange
        String firstName = "John";
        String lastName = "Doe";
        String email = "john.doe@test.com";
        String password = "PlainTextPassword";
        String encodedPassword = "EncodedSecurePassword";

        Client savedClient = new Client(firstName, lastName, email, encodedPassword);
        
        when(clientRepository.findByEmail(email)).thenReturn(null);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(clientRepository.save(any(Client.class))).thenReturn(savedClient);
        when(serviceAccounts.registerNewAccount(any(Client.class))).thenReturn(true);

        // Act
        clientsService.registerNewClient(firstName, lastName, email, password);

        // Assert
        verify(passwordEncoder, times(1)).encode(password);
        verify(clientRepository).save(argThat(client -> 
            client.getPassword().equals(encodedPassword)
        ));
    }

    @Test
    @DisplayName("Should create initial account for new client")
    void testInitialAccountCreation() {
        // Arrange
        String firstName = "John";
        String lastName = "Doe";
        String email = "john.doe@test.com";
        String password = "SecurePass123";

        Client savedClient = new Client(firstName, lastName, email, "encoded_password");
        
        when(clientRepository.findByEmail(email)).thenReturn(null);
        when(passwordEncoder.encode(password)).thenReturn("encoded_password");
        when(clientRepository.save(any(Client.class))).thenReturn(savedClient);
        when(serviceAccounts.registerNewAccount(any(Client.class))).thenReturn(true);

        // Act
        clientsService.registerNewClient(firstName, lastName, email, password);

        // Assert
        verify(serviceAccounts, times(1)).registerNewAccount(any(Client.class));
    }

    @Test
    @DisplayName("Should save client with correct details")
    void testClientDetailsAreSavedCorrectly() {
        // Arrange
        String firstName = "John";
        String lastName = "Doe";
        String email = "john.doe@test.com";
        String password = "SecurePass123";

        Client savedClient = new Client(firstName, lastName, email, "encoded_password");
        
        when(clientRepository.findByEmail(email)).thenReturn(null);
        when(passwordEncoder.encode(password)).thenReturn("encoded_password");
        when(clientRepository.save(any(Client.class))).thenReturn(savedClient);
        when(serviceAccounts.registerNewAccount(any(Client.class))).thenReturn(true);

        // Act
        clientsService.registerNewClient(firstName, lastName, email, password);

        // Assert
        verify(clientRepository).save(argThat(client -> 
            client.getFirstName().equals(firstName) &&
            client.getLastName().equals(lastName) &&
            client.getEmail().equals(email)
        ));
    }

    @Test
    @DisplayName("Should handle case where client save returns null")
    void testClientSaveReturnsNull() {
        // Arrange
        String firstName = "John";
        String lastName = "Doe";
        String email = "john.doe@test.com";
        String password = "SecurePass123";

        when(clientRepository.findByEmail(email)).thenReturn(null);
        when(passwordEncoder.encode(password)).thenReturn("encoded_password");
        when(clientRepository.save(any(Client.class))).thenReturn(null);

        // Act
        ResponseEntity<Object> response = clientsService.registerNewClient(firstName, lastName, email, password);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("This Client is not Autorized", response.getBody());
        verify(serviceAccounts, never()).registerNewAccount(any(Client.class));
    }

    @Test
    @DisplayName("Should create client even if email has special characters")
    void testRegistrationWithSpecialCharactersInEmail() {
        // Arrange
        String firstName = "John";
        String lastName = "Doe";
        String email = "john.doe+test@example.co.uk";
        String password = "SecurePass123";

        Client savedClient = new Client(firstName, lastName, email, "encoded_password");
        
        when(clientRepository.findByEmail(email)).thenReturn(null);
        when(passwordEncoder.encode(password)).thenReturn("encoded_password");
        when(clientRepository.save(any(Client.class))).thenReturn(savedClient);
        when(serviceAccounts.registerNewAccount(any(Client.class))).thenReturn(true);

        // Act
        ResponseEntity<Object> response = clientsService.registerNewClient(firstName, lastName, email, password);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(clientRepository, times(1)).save(any(Client.class));
    }

    @Test
    @DisplayName("Should handle names with spaces and special characters")
    void testRegistrationWithSpecialCharactersInName() {
        // Arrange
        String firstName = "Jean-Pierre";
        String lastName = "O'Connor";
        String email = "jp.oconnor@test.com";
        String password = "SecurePass123";

        Client savedClient = new Client(firstName, lastName, email, "encoded_password");
        
        when(clientRepository.findByEmail(email)).thenReturn(null);
        when(passwordEncoder.encode(password)).thenReturn("encoded_password");
        when(clientRepository.save(any(Client.class))).thenReturn(savedClient);
        when(serviceAccounts.registerNewAccount(any(Client.class))).thenReturn(true);

        // Act
        ResponseEntity<Object> response = clientsService.registerNewClient(firstName, lastName, email, password);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(clientRepository, times(1)).save(any(Client.class));
    }
}
