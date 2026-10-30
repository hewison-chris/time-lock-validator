package vesting

import org.scalatest.funsuite.AnyFunSuite
import scalus.cardano.onchain.plutus.prelude.*
import scalus.cardano.onchain.plutus.prelude.Option.*
import scalus.cardano.onchain.plutus.v3.*
import scalus.testing.kit.ScalusTest
import scalus.uplc.eval.Result
import vesting.VestingAction.*
import vesting.VestingFixtures.*

/** Unit tests: run the compiled validator on hand-made script contexts.
  *
  * `run` returns a `Result`: check `isSuccess` / `isFailure`, `logs` for the
  * error message, and `budget` for the CPU / memory cost. `expectFailure`
  * asserts both that it failed and why.
  *
  * Two tests are worked. Replace every `pending` with a real test; see
  * PROBLEM.md, Part B, for the list.
  */
class VestingValidatorTest extends AnyFunSuite, ScalusTest {

    private val contract = VestingContract.compiled.withErrorTraces

    private def run(ctx: ScriptContext): Result =
        contract.program.runWithDebug(ctx)

    private def expectFailure(reason: String)(ctx: ScriptContext): Unit =
        checkResult(failure(reason), run(ctx))

    // 1. Claim by the beneficiary at the deadline: accepted.
    test("Claim: beneficiary, validity range starts at the deadline") {
        val result = run(spendContext(Claim, List(beneficiary), from(deadline)))
        assert(result.isSuccess, result.logs.mkString("\n"))
        info(s"budget: ${result.budget}")
    }

    // 2. Claim before the deadline: rejected, for the right reason.
    test("Claim: before the deadline is rejected") {
        expectFailure(VestingValidator.TooEarlyToClaim) {
            spendContext(Claim, List(beneficiary), from(deadline - 1))
        }
    }

    // 3. Claim signed by the wrong key / by nobody: rejected, for the right reason.
    test("Claim: signed by the wrong key is rejected")(
      expectFailure(VestingValidator.UnsignedBeneficiary) {
          spendContext(Claim, List(stranger), from(deadline))
      }
    )

    test("Claim: signed by nobody is rejected")(
      expectFailure(VestingValidator.UnsignedBeneficiary) {
          spendContext(Claim, List.Nil, from(deadline))
      }
    )

    // 4. Cancel by the owner before the deadline: accepted. Include the exact boundary.
    test("Cancel: owner, validity range ends before the deadline")({
        val result = run(
          spendContext(Cancel, List(owner), upTo(deadline - 1))
        )
        assert(result.isSuccess, result.logs.mkString("\n"))
    })

    test("Cancel: owner, validity range ends exactly at the deadline")({
        val result = run(
          spendContext(Cancel, List(owner), upTo(deadline))
        )
        assert(result.isSuccess, result.logs.mkString("\n"))
    })

    // 5. Cancel after the deadline: rejected, for the right reason.
    test("Cancel: validity range ends after the deadline is rejected")({
        expectFailure(VestingValidator.TooLateToCancel) {
            spendContext(Cancel, List(owner), upTo(deadline + 1))
        }
    })

    // 6. Cancel signed by the wrong key: rejected, for the right reason.
    test("Cancel: signed by the wrong key is rejected")({
        expectFailure(VestingValidator.UnsignedOwner) {
            spendContext(Cancel, List(stranger), upTo(deadline))
        }
    })

    // 7. A transaction with no validity range set (for both actions): rejected, for the right reason.
    test("Claim: no validity range is rejected")({
        expectFailure(VestingValidator.NoValidityLowerBound) {
            spendContext(Claim, List(beneficiary), unbounded)
        }
    })

    test("Cancel: no validity range is rejected")({
        expectFailure(VestingValidator.NoValidityUpperBound) {
            spendContext(Cancel, List(owner), unbounded)
        }
    })

    // 8. A validity range that straddles the deadline: rejected for both actions.
    test("Claim: validity range straddling the deadline is rejected")({
        expectFailure(VestingValidator.TooEarlyToClaim) {
            spendContext(
              Claim,
              List(beneficiary),
              between(deadline - 1, deadline + 1)
            )
        }
    })

    test("Cancel: validity range straddling the deadline is rejected")({
        expectFailure(VestingValidator.TooLateToCancel) {
            spendContext(
              Cancel,
              List(owner),
              between(deadline - 1, deadline + 1)
            )
        }
    })

    // 9. Missing datum: rejected.
    test("Missing datum is rejected")({
        expectFailure(VestingValidator.InvalidDatum) {
            spendContext(Claim, List(beneficiary), from(deadline), None)
        }
    })
}
