package works.iterative.incubator.budget.tools

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import java.util.Base64
import zio.*

/** Implementation of token encryption that allows also for decrypting tokens for verification.
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
