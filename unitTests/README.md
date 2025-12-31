# Unit Tests for Home Banking Application

## Overview
This folder contains comprehensive unit tests for the **top 5 most critical functions** in the Home Banking application. These tests ensure the reliability and security of core banking operations.

## Critical Functions Tested

### 1. Money Transfer (OperationsServiceTransactionsTest.java)
**Function:** `OperationsServiceImplements.postTransactions()`

**Why it's critical:** Handles money transfers between accounts, affecting customer balances and financial transactions.

**Tests cover:**
- ✅ Successful money transfer between valid accounts
- ✅ Rejection when client is not authenticated
- ✅ Rejection with invalid amounts (zero or negative)
- ✅ Rejection when origin and destination are the same
- ✅ Rejection when destination account doesn't exist
- ✅ Rejection when origin account doesn't belong to client
- ✅ Rejection when insufficient funds
- ✅ Rejection when account fields are empty

**Total Test Cases:** 8

---

### 2. Loan Approval (OperationsServiceLoansTest.java)
**Function:** `OperationsServiceImplements.postLoans()`

**Why it's critical:** Manages loan approvals and fund disbursement, involving significant financial commitments.

**Tests cover:**
- ✅ Successful loan approval and disbursement
- ✅ Rejection when amount is below minimum ($5,000)
- ✅ Rejection when client is not authenticated
- ✅ Rejection when destination account doesn't exist
- ✅ Rejection when account doesn't belong to client
- ✅ Rejection when required fields are missing
- ✅ Rejection when loan type doesn't exist
- ✅ Rejection when loan name doesn't match
- ✅ Rejection when payment term is not available
- ✅ Rejection when amount exceeds maximum
- ✅ Verification of 20% interest calculation

**Total Test Cases:** 11

---

### 3. Account Creation (AccountServiceCreateAccountTest.java)
**Function:** `AccountServicesImplements.CreateNewAccount()`

**Why it's critical:** Creates new bank accounts for clients with business rule validations.

**Tests cover:**
- ✅ Successful account creation for authenticated client
- ✅ Rejection when client is not authenticated
- ✅ Rejection when client has 3 accounts (maximum per client)
- ✅ Rejection when system has 9,999 accounts (maximum total)
- ✅ Rejection when createNewAccount parameter is false
- ✅ Verification of initial balance (0.00)
- ✅ Verification of sequential account number generation
- ✅ Allow client with 2 accounts to create a third
- ✅ Verification of timestamp for creation date
- ✅ Verification that account DTOs are returned

**Total Test Cases:** 10

---

### 4. Card Creation (CardsServicePostNewCardTest.java)
**Function:** `CardsImplements.postNewCard()`

**Why it's critical:** Creates credit and debit cards with complex business rules and security requirements.

**Tests cover:**
- ✅ Successful credit card creation
- ✅ Successful debit card creation
- ✅ Rejection when client is not authenticated
- ✅ Rejection when client has 6 cards (maximum total)
- ✅ Rejection when client has 3 credit cards (maximum credit)
- ✅ Rejection when client has 3 debit cards (maximum debit)
- ✅ Allow mix of credit and debit cards within limits
- ✅ Rejection when card type is null
- ✅ Rejection when card color is null
- ✅ Verification of card number and CVV generation
- ✅ Verification of card expiration (1 year from creation)
- ✅ Verification that card DTOs are returned

**Total Test Cases:** 12

---

### 5. Client Registration (ClientsServiceRegisterNewClientTest.java)
**Function:** `ClientsImplements.registerNewClient()`

**Why it's critical:** Handles new client onboarding, account setup, and security (password encoding).

**Tests cover:**
- ✅ Successful client registration with valid data
- ✅ Rejection when first name is empty
- ✅ Rejection when last name is empty
- ✅ Rejection when email is empty
- ✅ Rejection when password is empty
- ✅ Rejection when all fields are empty
- ✅ Rejection when email is already in use
- ✅ Verification of password encoding
- ✅ Verification that initial account is created
- ✅ Verification that client details are saved correctly
- ✅ Handling when client save returns null
- ✅ Support for emails with special characters
- ✅ Support for names with special characters

**Total Test Cases:** 13

---

## Test Statistics

| Test Class | Function Tested | Test Cases | Lines of Code |
|------------|----------------|------------|---------------|
| OperationsServiceTransactionsTest | postTransactions | 8 | ~270 |
| OperationsServiceLoansTest | postLoans | 11 | ~350 |
| AccountServiceCreateAccountTest | CreateNewAccount | 10 | ~280 |
| CardsServicePostNewCardTest | postNewCard | 12 | ~375 |
| ClientsServiceRegisterNewClientTest | registerNewClient | 13 | ~345 |
| **TOTAL** | **5 Functions** | **54** | **~1,620** |

## Technology Stack

- **Testing Framework:** JUnit 5 (Jupiter)
- **Mocking Framework:** Mockito
- **Assertions:** JUnit Assertions
- **Spring Integration:** Spring Boot Test

## How to Run the Tests

These tests can be integrated into your existing Spring Boot test suite. To run them:

### Option 1: Copy to Standard Test Location
```bash
# Copy tests to the standard test directory
cp unitTests/*.java backend/src/test/java/springbootvuejs/services/imp/
```

### Option 2: Run from unitTests folder
The tests are ready to run and can be executed using Maven or your IDE:

```bash
# Using Maven (from backend directory)
mvn test

# Run specific test class
mvn test -Dtest=OperationsServiceTransactionsTest

# Run all tests matching a pattern
mvn test -Dtest=*ServiceTest
```

### Option 3: IDE Integration
- **IntelliJ IDEA:** Right-click on test class → Run
- **Eclipse:** Right-click on test class → Run As → JUnit Test
- **VS Code:** Use Java Test Runner extension

## Test Coverage

These unit tests provide comprehensive coverage of:
- ✅ **Happy path scenarios** - Valid operations succeed
- ✅ **Authentication & Authorization** - Unauthorized access is blocked
- ✅ **Data validation** - Invalid inputs are rejected
- ✅ **Business rules** - All business constraints are enforced
- ✅ **Edge cases** - Boundary conditions are tested
- ✅ **Security** - Password encoding, account ownership verification
- ✅ **Financial accuracy** - Correct calculations and balance updates

## Test Isolation

All tests are:
- **Fully isolated** - Using Mockito mocks for dependencies
- **Independent** - Can run in any order
- **Fast** - No database or external dependencies
- **Repeatable** - Consistent results on every run

## Best Practices Applied

1. **Descriptive test names** - Uses `@DisplayName` for clear test descriptions
2. **Arrange-Act-Assert pattern** - Clear test structure
3. **Comprehensive mocking** - All dependencies are mocked
4. **Assertion verification** - Multiple assertions per test when appropriate
5. **Mock verification** - Ensures correct interactions with dependencies
6. **BeforeEach setup** - Common test data initialization

## Maintenance Notes

- Tests use the actual service implementations
- Mock repositories to avoid database dependencies
- Tests should be updated when business rules change
- Keep tests synchronized with production code

## Future Enhancements

Consider adding:
- Integration tests for end-to-end workflows
- Performance tests for high-volume scenarios
- Security penetration tests
- Load testing for concurrent operations

---

**Created:** 2025-12-31  
**Purpose:** Critical function validation for Home Banking application  
**Maintained by:** Development Team
