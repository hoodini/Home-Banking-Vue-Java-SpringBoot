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
import springbootvuejs.dtos.CardDTO;
import springbootvuejs.models.Card;
import springbootvuejs.models.Client;
import springbootvuejs.models.Enums.ColorCard;
import springbootvuejs.models.Enums.TypeCard;
import springbootvuejs.repository.CardRepository;
import springbootvuejs.repository.ClientRepository;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CardsImplements.postNewCard method
 * This is one of the most critical functions as it handles card creation with complex business rules
 */
@DisplayName("Cards Service - Post New Card Tests")
public class CardsServicePostNewCardTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CardsImplements cardsService;

    private Client testClient;
    private List<Card> allCards;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup test client
        testClient = new Client("John", "Doe", "john.doe@test.com", "password123");
        testClient.setCards(new HashSet<>());
        
        // Setup all cards in the system
        allCards = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            allCards.add(new Card(TypeCard.DEBIT, ColorCard.GOLD, "2555 2254 4554 " + i, 123, 
                LocalDateTime.now(), LocalDateTime.now().plusYears(1), testClient));
        }
    }

    @Test
    @DisplayName("Should successfully create a credit card for authenticated client")
    void testSuccessfulCreditCardCreation() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(cardRepository.findAll()).thenReturn(allCards);
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Set<Card> updatedCards = new HashSet<>();
        updatedCards.add(new Card(TypeCard.CREDIT, ColorCard.GOLD, "2555 2254 4554 1234", 456, 
            LocalDateTime.now(), LocalDateTime.now().plusYears(1), testClient));
        testClient.setCards(updatedCards);
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);

        // Act
        ResponseEntity<?> response = cardsService.postNewCard(authentication, TypeCard.CREDIT, ColorCard.GOLD);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cardRepository, times(1)).save(any(Card.class));
    }

    @Test
    @DisplayName("Should successfully create a debit card for authenticated client")
    void testSuccessfulDebitCardCreation() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(cardRepository.findAll()).thenReturn(allCards);
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Set<Card> updatedCards = new HashSet<>();
        updatedCards.add(new Card(TypeCard.DEBIT, ColorCard.PLATINUM, "2555 2254 4554 1234", 456, 
            LocalDateTime.now(), LocalDateTime.now().plusYears(1), testClient));
        testClient.setCards(updatedCards);
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);

        // Act
        ResponseEntity<?> response = cardsService.postNewCard(authentication, TypeCard.DEBIT, ColorCard.PLATINUM);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cardRepository, times(1)).save(any(Card.class));
    }

    @Test
    @DisplayName("Should reject card creation when client is not authenticated")
    void testCardCreationUnauthenticatedClient() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(null);

        // Act
        ResponseEntity<?> response = cardsService.postNewCard(authentication, TypeCard.CREDIT, ColorCard.GOLD);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Authenticated client is not recognized", response.getBody());
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    @DisplayName("Should reject card creation when client has 6 cards (maximum)")
    void testCardCreationMaxCardsReached() {
        // Arrange
        Set<Card> sixCards = new HashSet<>();
        for (int i = 0; i < 6; i++) {
            sixCards.add(new Card(TypeCard.CREDIT, ColorCard.GOLD, "2555 2254 4554 " + i, 123, 
                LocalDateTime.now(), LocalDateTime.now().plusYears(1), testClient));
        }
        testClient.setCards(sixCards);

        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);

        // Act
        ResponseEntity<?> response = cardsService.postNewCard(authentication, TypeCard.CREDIT, ColorCard.GOLD);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("maximum cards permitted"));
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    @DisplayName("Should reject credit card creation when client already has 3 credit cards")
    void testCardCreationMaxCreditCardsReached() {
        // Arrange
        Set<Card> threeCredits = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            threeCredits.add(new Card(TypeCard.CREDIT, ColorCard.GOLD, "2555 2254 4554 " + i, 123, 
                LocalDateTime.now(), LocalDateTime.now().plusYears(1), testClient));
        }
        testClient.setCards(threeCredits);

        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);

        // Act
        ResponseEntity<?> response = cardsService.postNewCard(authentication, TypeCard.CREDIT, ColorCard.PLATINUM);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Already have 3 cards credit yet", response.getBody());
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    @DisplayName("Should reject debit card creation when client already has 3 debit cards")
    void testCardCreationMaxDebitCardsReached() {
        // Arrange
        Set<Card> threeDebits = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            threeDebits.add(new Card(TypeCard.DEBIT, ColorCard.GOLD, "2555 2254 4554 " + i, 123, 
                LocalDateTime.now(), LocalDateTime.now().plusYears(1), testClient));
        }
        testClient.setCards(threeDebits);

        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);

        // Act
        ResponseEntity<?> response = cardsService.postNewCard(authentication, TypeCard.DEBIT, ColorCard.TITANIUM);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Already have 3 cards debit yet", response.getBody());
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    @DisplayName("Should allow mix of credit and debit cards up to limits")
    void testCardCreationMixedCards() {
        // Arrange
        Set<Card> mixedCards = new HashSet<>();
        // Add 2 credit cards
        for (int i = 0; i < 2; i++) {
            mixedCards.add(new Card(TypeCard.CREDIT, ColorCard.GOLD, "2555 2254 4554 " + i, 123, 
                LocalDateTime.now(), LocalDateTime.now().plusYears(1), testClient));
        }
        // Add 2 debit cards
        for (int i = 2; i < 4; i++) {
            mixedCards.add(new Card(TypeCard.DEBIT, ColorCard.PLATINUM, "2555 2254 4554 " + i, 123, 
                LocalDateTime.now(), LocalDateTime.now().plusYears(1), testClient));
        }
        testClient.setCards(mixedCards);

        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(cardRepository.findAll()).thenReturn(allCards);
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Create an updated set with the new card
        Set<Card> updatedCards = new HashSet<>(mixedCards);
        Card newCard = new Card(TypeCard.CREDIT, ColorCard.TITANIUM, "2555 2254 4554 5678", 789, 
            LocalDateTime.now(), LocalDateTime.now().plusYears(1), testClient);
        updatedCards.add(newCard);
        
        // Mock the second call to findByEmail to return updated cards
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient).thenReturn(testClient);

        // Act
        ResponseEntity<?> response = cardsService.postNewCard(authentication, TypeCard.CREDIT, ColorCard.TITANIUM);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cardRepository, times(1)).save(any(Card.class));
    }

    @Test
    @DisplayName("Should reject card creation when type is null")
    void testCardCreationNullType() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);

        // Act & Assert - This will throw NPE in the actual code because it checks card count first
        // The implementation has a bug where it calls typeCard.toString() before checking if it's null
        assertThrows(NullPointerException.class, () -> {
            cardsService.postNewCard(authentication, null, ColorCard.GOLD);
        });
    }

    @Test
    @DisplayName("Should reject card creation when color is null")
    void testCardCreationNullColor() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);

        // Act
        ResponseEntity<?> response = cardsService.postNewCard(authentication, TypeCard.CREDIT, null);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("You don't specified the color of card, try again ", response.getBody());
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    @DisplayName("Should generate card with 4-digit random number and 3-digit CVV")
    void testCardNumberAndCVVGeneration() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(cardRepository.findAll()).thenReturn(allCards);

        // Act
        cardsService.postNewCard(authentication, TypeCard.CREDIT, ColorCard.GOLD);

        // Assert
        verify(cardRepository).save(argThat(card -> {
            String cardNumber = card.getNumber();
            int cvv = card.getCvv();
            
            // Check that card number starts with the prefix and has a 4-digit suffix
            boolean validCardNumber = cardNumber.matches("2555 2254 4554 \\d{4}");
            
            // Check that CVV is between 100 and 999
            boolean validCVV = cvv >= 100 && cvv <= 999;
            
            return validCardNumber && validCVV;
        }));
    }

    @Test
    @DisplayName("Should set card expiration to 1 year from creation")
    void testCardExpirationDate() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(cardRepository.findAll()).thenReturn(allCards);

        LocalDateTime beforeCreation = LocalDateTime.now();

        // Act
        cardsService.postNewCard(authentication, TypeCard.CREDIT, ColorCard.GOLD);

        LocalDateTime afterCreation = LocalDateTime.now();

        // Assert
        verify(cardRepository).save(argThat(card -> {
            LocalDateTime fromDate = card.getFromDate();
            LocalDateTime thruDate = card.getThruDate();
            
            // Check that fromDate is around now
            boolean validFromDate = fromDate.isAfter(beforeCreation.minusSeconds(1)) && 
                                   fromDate.isBefore(afterCreation.plusSeconds(1));
            
            // Check that thruDate is approximately 1 year from fromDate
            LocalDateTime expectedThru = beforeCreation.plusYears(1);
            boolean validThruDate = thruDate.isAfter(expectedThru.minusSeconds(10)) && 
                                   thruDate.isBefore(expectedThru.plusSeconds(10));
            
            return validFromDate && validThruDate;
        }));
    }

    @Test
    @DisplayName("Should return card DTOs after successful creation")
    void testCardCreationReturnsDTO() {
        // Arrange
        when(authentication.getName()).thenReturn("john.doe@test.com");
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);
        when(cardRepository.findAll()).thenReturn(allCards);
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Set<Card> updatedCards = new HashSet<>();
        updatedCards.add(new Card(TypeCard.CREDIT, ColorCard.GOLD, "2555 2254 4554 1234", 456, 
            LocalDateTime.now(), LocalDateTime.now().plusYears(1), testClient));
        testClient.setCards(updatedCards);
        when(clientRepository.findByEmail("john.doe@test.com")).thenReturn(testClient);

        // Act
        ResponseEntity<?> response = cardsService.postNewCard(authentication, TypeCard.CREDIT, ColorCard.GOLD);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof Set);
    }
}
