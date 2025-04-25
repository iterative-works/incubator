package works.iterative.incubator.budget.domain.model

import zio.test.*
import zio.test.Assertion.*

/** Tests for the ConfidenceScore value object */
object ConfidenceScoreSpec extends ZIOSpecDefault:
    def spec = suite("ConfidenceScore")(
        test("should be created with a valid score") {
            val score = ConfidenceScore(0.75)
            assert(score.value)(equalTo(0.75))
        },
        
        test("should clamp values below minimum to the minimum") {
            val score = ConfidenceScore(-0.5)
            assert(score.value)(equalTo(0.0))
        },
        
        test("should clamp values above maximum to the maximum") {
            val score = ConfidenceScore(1.5)
            assert(score.value)(equalTo(1.0))
        },
        
        test("should create a score via the create method when valid") {
            val scoreEither = ConfidenceScore.create(0.75)
            assert(scoreEither.isRight)(isTrue) &&
            assert(scoreEither.toOption.get.value)(equalTo(0.75))
        },
        
        test("should return an error via the create method when invalid") {
            val scoreEither = ConfidenceScore.create(1.5)
            assert(scoreEither.isLeft)(isTrue)
        },
        
        test("should correctly identify high confidence scores") {
            assert(ConfidenceScore(0.85).isHigh)(isTrue) &&
            assert(ConfidenceScore(0.75).isHigh)(isFalse)
        },
        
        test("should correctly identify medium confidence scores") {
            assert(ConfidenceScore(0.75).isMedium)(isTrue) &&
            assert(ConfidenceScore(0.45).isMedium)(isFalse) &&
            assert(ConfidenceScore(0.85).isMedium)(isFalse)
        },
        
        test("should correctly identify low confidence scores") {
            assert(ConfidenceScore(0.45).isLow)(isTrue) &&
            assert(ConfidenceScore(0.55).isLow)(isFalse)
        },
        
        test("should correctly determine if it exceeds a threshold") {
            assert(ConfidenceScore(0.75).exceeds(0.7))(isTrue) &&
            assert(ConfidenceScore(0.65).exceeds(0.7))(isFalse)
        },
        
        test("should provide correct constant values") {
            assert(ConfidenceScore.minimum.value)(equalTo(0.0)) &&
            assert(ConfidenceScore.maximum.value)(equalTo(1.0)) &&
            assert(ConfidenceScore.medium.value)(equalTo(0.5))
        }
    )
end ConfidenceScoreSpec