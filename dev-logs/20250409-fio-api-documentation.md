# Fio Bank API Documentation - 2025-04-09

## Overview

For the Fio Bank integration implementation, we need to obtain example responses for testing. We have the following resources:

1. We already have example JSON response files in the codebase:
   - `/ynab-importer/infrastructure/src/test/resources/example_fio.json`

2. Official API documentation:
   - `/ynab-importer/doc/API_Bankovnictvi.pdf` 
   - `/ynab-importer/doc/API_Bankovnictvi.txt`

## API Access Methods

Based on the existing implementation and documentation, there are multiple ways to access Fio Bank data:

1. **Date Range Endpoint**: `/periods/${token}/${dateFrom}/${dateTo}/transactions.{format}`
   - Used for retrieving transactions within a specific date range
   - Implementation exists in `FioClient.fetchTransactions()`

2. **New Transactions Endpoint**: `/by-id/${token}/${lastId}/transactions.{format}`
   - Used for retrieving new transactions since a specific transaction ID
   - Implementation exists in `FioClient.fetchNewTransactions()`

3. **Last N Days Endpoint**: Not currently implemented, but mentioned in the documentation

## Example Response Structure

The example Fio API response available in our codebase has the following structure:

```json
{
  "accountStatement": {
    "info": {
      "accountId": "2200000001",
      "bankId": "2010",
      "currency": "CZK",
      "iban": "CZ5420100000002200000001",
      "bic": "FIOBCZPPXXX",
      "openingBalance": 257.69,
      "closingBalance": 47257.69,
      "dateStart": "2025-03-14+0100",
      "dateEnd": "2025-03-15+0100",
      "idFrom": 26962199069,
      "idTo": 26962448650
    },
    "transactionList": {
      "transaction": [
        {
          "column22": { "value": 26962199069, "name": "ID pohybu", "id": 22 },
          "column0": { "value": "2025-03-14+0100", "name": "Datum", "id": 0 },
          "column1": { "value": 50000.0, "name": "Objem", "id": 1 },
          "column14": { "value": "CZK", "name": "Měna", "id": 14 },
          "column2": { "value": "2800000002", "name": "Protiúčet", "id": 2 },
          "column10": { "value": "Novák, Jan", "name": "Název protiúčtu", "id": 10 },
          "column3": { "value": "2010", "name": "Kód banky", "id": 3 },
          "column12": { "value": "Fio banka, a.s.", "name": "Název banky", "id": 12 },
          "column8": { "value": "Příjem převodem uvnitř banky", "name": "Typ", "id": 8 }
          // Other fields omitted for brevity
        },
        // More transactions...
      ]
    }
  }
}
```

## Column Mapping Reference

Based on the example and code implementation, the Fio Bank API uses numbered columns for different transaction attributes:

| Column ID | Field Name | Description | Type |
|-----------|------------|-------------|------|
| 0 | Datum | Transaction date | String (YYYY-MM-DD+HHMM) |
| 1 | Objem | Transaction amount | Number |
| 2 | Protiúčet | Counter account number | String |
| 3 | Kód banky | Bank code | String |
| 4 | KS | Constant symbol | String |
| 5 | VS | Variable symbol | String |
| 6 | SS | Specific symbol | String |
| 7 | Uživatelská identifikace | User identification | String |
| 8 | Typ | Transaction type | String |
| 10 | Název protiúčtu | Counter account name | String |
| 12 | Název banky | Bank name | String |
| 14 | Měna | Currency | String |
| 17 | ID pokynu | Instruction ID | Number |
| 22 | ID pohybu | Transaction ID | Number |
| 25 | Komentář | Comment | String |

## Plan for Test Data Collection

For implementing tests, we should:

1. **Use Existing Example**:
   - Use the existing `example_fio.json` file for basic test cases
   - Create variations with different transaction types and edge cases

2. **Sandbox Testing**:
   - If available, use Fio Bank's sandbox environment for testing
   - Create a test dataset that covers all transaction types and edge cases

3. **Documentation Analysis**:
   - Extract more example responses from the API documentation
   - Focus on different transaction types and error scenarios

4. **Error Response Collection**:
   - Document and create test cases for various error responses:
     - Authentication errors
     - Invalid parameters
     - Rate limiting errors
     - Server errors

## Obtaining Additional Test Data

There are several approaches to obtain additional test data:

1. **Conditional Integration Tests**:
   - Similar to YNAB tests, create integration tests that conditionally run when Fio API credentials are available
   - Add logging to capture and save real API responses for later use in unit tests

2. **Documentation Extraction**:
   - Extract more example responses from the Fio Bank API documentation
   - Convert examples to JSON format suitable for our testing

3. **Mock Generation**:
   - Generate mock responses based on the documented structure
   - Ensure coverage of various transaction types and edge cases

4. **Request from Fio Bank**:
   - Request additional example responses from Fio Bank support
   - Focus on scenarios not covered by existing examples