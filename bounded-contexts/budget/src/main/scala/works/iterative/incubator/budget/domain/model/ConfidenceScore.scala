package works.iterative.incubator.budget.domain.model

/** Represents the confidence in a categorization decision
  *
  * This value object ensures that confidence scores are always within the valid range of 0.0 to 1.0,
  * where:
  * - 0.0 represents no confidence
  * - 1.0 represents absolute confidence
  *
  * @param value The confidence score, between 0.0 and 1.0 inclusive
  * 
  * Classification: Domain Value Object
  */
case class ConfidenceScore private (value: Double):
    /** Check if this confidence score exceeds a threshold */
    def exceeds(threshold: Double): Boolean = value > threshold
    
    /** Check if this confidence score is considered high (>= 0.8) */
    def isHigh: Boolean = value >= 0.8
    
    /** Check if this confidence score is considered medium (>= 0.5 and < 0.8) */
    def isMedium: Boolean = value >= 0.5 && value < 0.8
    
    /** Check if this confidence score is considered low (< 0.5) */
    def isLow: Boolean = value < 0.5

end ConfidenceScore

/** Companion object for creating validated ConfidenceScore instances */
object ConfidenceScore:
    /** Default threshold for a score to be considered reliable */
    val ReliableThreshold: Double = 0.7

    /** The minimum valid confidence score */
    val Min: Double = 0.0
    
    /** The maximum valid confidence score */
    val Max: Double = 1.0
    
    /** Create a new ConfidenceScore instance, ensuring the value is within valid bounds.
      *
      * @param value The raw score value, will be clamped to [0.0, 1.0]
      * @return A validated ConfidenceScore instance
      */
    def apply(value: Double): ConfidenceScore =
        val normalizedValue = Math.max(Min, Math.min(Max, value))
        new ConfidenceScore(normalizedValue)
    
    /** Create a new ConfidenceScore from a value that might be outside the valid range.
      *
      * @param value The raw score value
      * @return Either a validation error or a valid ConfidenceScore
      */
    def create(value: Double): Either[String, ConfidenceScore] =
        if (value < Min || value > Max)
            Left(s"Confidence score must be between $Min and $Max, got $value")
        else
            Right(new ConfidenceScore(value))
    
    /** Create a minimum confidence score (0.0) */
    val minimum: ConfidenceScore = ConfidenceScore(Min)
    
    /** Create a maximum confidence score (1.0) */
    val maximum: ConfidenceScore = ConfidenceScore(Max)
    
    /** Create a medium confidence score (0.5) */
    val medium: ConfidenceScore = ConfidenceScore(0.5)
end ConfidenceScore