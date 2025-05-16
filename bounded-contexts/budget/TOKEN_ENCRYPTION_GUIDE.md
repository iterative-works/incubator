# FIO Bank Token Encryption Guide

This guide explains how to manually encrypt FIO Bank API tokens for direct insertion into the database using our provided utility tool.

## Background

The FIO Bank integration stores API tokens in an encrypted format using AES-256-CBC with PKCS5Padding. The encryption process:

1. Uses a 32-byte key for AES-256 encryption
2. Generates a random 16-byte Initialization Vector (IV)
3. Prepends the IV to the encrypted data
4. Base64 encodes the combined result

## Token Encryption Tool

We've created a simple command-line tool that follows the exact same encryption process as the application. This ensures compatibility with the application's decryption mechanism.

### Using the Tool

We provide a simple tool with both encryption and decryption capabilities:

```bash
# For encryption
sbtn "runMain works.iterative.incubator.budget.tools.FioTokenCrypto encrypt <key> <token>"

# For decryption (verification)
sbtn "runMain works.iterative.incubator.budget.tools.FioTokenCrypto decrypt <key> <encrypted-token>"
```

## Encryption Key

The encryption key is stored in the application's configuration. The default key in development is:

```
default-key-must-be-replaced-in-production
```

For production use, this should be a secure value at least 32 characters long. The system will pad or truncate it to exactly 32 bytes.

## Database Insertion

Once you have the encrypted token, you can insert it directly into the database:

```sql
INSERT INTO fio_accounts
(id, source_account_id, encrypted_token, created_at, updated_at)
VALUES
(nextval('fio_account_id_seq'), 'your-bank-id-your-account-id', 'your-encrypted-token', NOW(), NOW());
```

Replace:
- `your-bank-id-your-account-id` with your AccountId value, formatted as `bankId-bankAccountId`
- `your-encrypted-token` with the Base64 encoded string from the encryption tool

## Example

Here's a complete example:

1. Encrypt the token:
   ```bash
   sbtn "runMain works.iterative.incubator.budget.tools.FioTokenCrypto encrypt secret-key-for-development fio-token-12345678"
   ```

2. Copy the encrypted output:
   ```
   Encrypted token: A1b2C3d4E5f6G7h8I9j0K1l2M3n4O5p6Q7r8S9t0U1v2W3x4Y5z6==
   ```

3. Insert into database:
   ```sql
   INSERT INTO fio_accounts
   (id, source_account_id, encrypted_token, created_at, updated_at)
   VALUES
   (nextval('fio_account_id_seq'), 'fio-12345678', 'A1b2C3d4E5f6G7h8I9j0K1l2M3n4O5p6Q7r8S9t0U1v2W3x4Y5z6==', NOW(), NOW());
   ```

## Security Notes

- **Never commit encryption keys or raw tokens to version control**
- Delete any files containing raw tokens after use
- Use secure channels when sharing encryption keys
- Consider rotating encryption keys periodically

## Troubleshooting

If you encounter issues:

1. **Encryption Errors**: Ensure your key is properly formatted
2. **Database Errors**: Check the database schema and ensure the correct column types
3. **Decryption Errors**: Verify that the application is using the same encryption key as used during encryption

To verify your encryption worked correctly, you can use the decrypt function of the tool:

```bash
sbtn "runMain works.iterative.incubator.budget.tools.FioTokenCrypto decrypt <same-key> <encrypted-token>"
```

## Future Improvements

This manual process is intended as a temporary solution. A proper account management UI will be implemented in the future.
