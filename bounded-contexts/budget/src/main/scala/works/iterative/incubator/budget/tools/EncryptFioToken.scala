package works.iterative.incubator.budget.tools

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import java.util.Base64
import zio.*

/** Simple command-line tool to encrypt a FIO token in the same way the application does. This
  * allows administrators to encrypt tokens for direct database insertion.
  *
  * The tool takes an encryption key and a token as arguments, performs AES-256-CBC encryption, and
  * outputs the Base64-encoded result.
  *
  * Usage: sbtn "runMain works.iterative.incubator.budget.tools.EncryptFioToken <encryption-key>
  * <token>"
  */
object EncryptFioToken extends ZIOAppDefault:
    override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
        for
            args <- ZIOAppArgs.getArgs
            _ <- validateArgs(args)
            encryptionKey = args(0)
            token = args(1)
            encryptedToken <- encryptToken(encryptionKey, token)
            _ <- Console.printLine(s"Encrypted token: $encryptedToken")
        yield ()

    private def validateArgs(args: Chunk[String]): ZIO[Any, Throwable, Unit] =
        if args.length != 2 then
            ZIO.fail(new IllegalArgumentException(
                "Usage: sbtn \"runMain works.iterative.incubator.budget.tools.EncryptFioToken <encryption-key> <token>\""
            ))
        else
            ZIO.unit

    /** Encrypts a token using AES-256-CBC with PKCS5Padding. The encryption process:
      *   1. Pads or truncates the key to exactly 32 bytes 2. Generates a random 16-byte
      *      initialization vector (IV) 3. Encrypts the token using AES-256-CBC 4. Prepends the IV
      *      to the encrypted data 5. Base64 encodes the combined result
      *
      * @param rawKey
      *   The raw encryption key (will be padded/truncated to 32 bytes)
      * @param token
      *   The token to encrypt
      * @return
      *   The encrypted token as a Base64-encoded string
      */
    private def encryptToken(rawKey: String, token: String): ZIO[Any, Throwable, String] =
        ZIO.attemptBlocking {
            // Pad or truncate key to exactly 32 bytes (required for AES-256)
            val bytes = rawKey.getBytes("UTF-8")
            val key = if bytes.length < 32 then
                // Pad with zeros if too short
                bytes.padTo(32, 0.toByte)
            else if bytes.length > 32 then
                // Truncate if too long
                bytes.take(32)
            else
                bytes

            // Configure AES encryption
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val keySpec = new SecretKeySpec(key, "AES")

            // Generate a random IV (same as in FioTokenManager)
            val ivBytes = new Array[Byte](16)
            new SecureRandom().nextBytes(ivBytes)
            val ivSpec = new IvParameterSpec(ivBytes)

            // Initialize cipher for encryption
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

            // Encrypt the token
            val encrypted = cipher.doFinal(token.getBytes("UTF-8"))

            // Combine IV and encrypted data (same as in FioTokenManager)
            val combined = new Array[Byte](ivBytes.length + encrypted.length)
            java.lang.System.arraycopy(ivBytes, 0, combined, 0, ivBytes.length)
            java.lang.System.arraycopy(encrypted, 0, combined, ivBytes.length, encrypted.length)

            // Base64 encode the result
            Base64.getEncoder.encodeToString(combined)
        }.catchAll { error =>
            ZIO.fail(new RuntimeException(s"Failed to encrypt token: ${error.getMessage}", error))
        }
end EncryptFioToken

/** Alternative implementation that also allows decrypting tokens for verification.
  *
  * Usage:
  *   - Encrypt: sbtn "runMain works.iterative.incubator.budget.tools.FioTokenCrypto encrypt <key>
  *     <token>"
  *   - Decrypt: sbtn "runMain works.iterative.incubator.budget.tools.FioTokenCrypto decrypt <key>
  *     <encrypted-token>"
  */
object FioTokenCrypto extends ZIOAppDefault:
    override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
        for
            args <- ZIOAppArgs.getArgs
            _ <- validateArgs(args)
            command = args(0)
            key = args(1)
            token = args(2)
            result <-
                if command == "encrypt" then
                    encryptToken(key, token).map(encrypted => s"Encrypted token: $encrypted")
                else if command == "decrypt" then
                    decryptToken(key, token).map(decrypted => s"Decrypted token: $decrypted")
                else
                    ZIO.fail(new IllegalArgumentException(s"Unknown command: $command"))
            _ <- Console.printLine(result)
        yield ()

    private def validateArgs(args: Chunk[String]): ZIO[Any, Throwable, Unit] =
        if args.length != 3 then
            ZIO.fail(new IllegalArgumentException(
                "Usage: sbtn \"runMain works.iterative.incubator.budget.tools.FioTokenCrypto <encrypt|decrypt> <key> <token>\""
            ))
        else if args(0) != "encrypt" && args(0) != "decrypt" then
            ZIO.fail(new IllegalArgumentException(
                "First argument must be either 'encrypt' or 'decrypt'"
            ))
        else
            ZIO.unit

    private def encryptToken(rawKey: String, token: String): ZIO[Any, Throwable, String] =
        ZIO.attemptBlocking {
            // Pad or truncate key to exactly 32 bytes (required for AES-256)
            val bytes = rawKey.getBytes("UTF-8")
            val key = if bytes.length < 32 then
                bytes.padTo(32, 0.toByte)
            else if bytes.length > 32 then
                bytes.take(32)
            else
                bytes

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val keySpec = new SecretKeySpec(key, "AES")

            val ivBytes = new Array[Byte](16)
            new SecureRandom().nextBytes(ivBytes)
            val ivSpec = new IvParameterSpec(ivBytes)

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encrypted = cipher.doFinal(token.getBytes("UTF-8"))

            val combined = new Array[Byte](ivBytes.length + encrypted.length)
            java.lang.System.arraycopy(ivBytes, 0, combined, 0, ivBytes.length)
            java.lang.System.arraycopy(encrypted, 0, combined, ivBytes.length, encrypted.length)

            Base64.getEncoder.encodeToString(combined)
        }

    private def decryptToken(rawKey: String, encryptedToken: String): ZIO[Any, Throwable, String] =
        ZIO.attemptBlocking {
            // Pad or truncate key to exactly 32 bytes (required for AES-256)
            val bytes = rawKey.getBytes("UTF-8")
            val key = if bytes.length < 32 then
                bytes.padTo(32, 0.toByte)
            else if bytes.length > 32 then
                bytes.take(32)
            else
                bytes

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val keySpec = new SecretKeySpec(key, "AES")

            // Decode from Base64
            val combined = Base64.getDecoder.decode(encryptedToken)

            // Extract IV
            val ivBytes = combined.take(16)
            val ivSpec = new IvParameterSpec(ivBytes)

            // Extract encrypted data
            val encrypted = combined.drop(16)

            // Initialize the cipher for decryption
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)

            // Decrypt the token
            new String(cipher.doFinal(encrypted), "UTF-8")
        }
end FioTokenCrypto
