package vesting

import scalus.compiler.Compile
import scalus.uplc.builtin.Data
import scalus.uplc.builtin.Data.{FromData, ToData}
import scalus.cardano.onchain.plutus.v3.*
import scalus.cardano.onchain.plutus.prelude.*

/** What is stored with the locked output.
  *
  * @param owner
  *   may take the funds back until `deadline`
  * @param beneficiary
  *   may take the funds from `deadline` on
  * @param deadline
  *   POSIX time in milliseconds
  */
case class VestingDatum(
    owner: PubKeyHash,
    beneficiary: PubKeyHash,
    deadline: PosixTime
) derives FromData,
      ToData

/** What the spender supplies: which of the two paths they are taking. */
enum VestingAction derives FromData, ToData:
    case Claim
    case Cancel

/** Time-locked vesting: `owner` locks funds for `beneficiary`. Until `deadline`
  * the owner can cancel; from `deadline` on only the beneficiary can claim.
  *
  * The script cannot read the clock. It reads the interval in which the ledger
  * promises the transaction is valid (`tx.validRange`): the lower bound is
  * inclusive, the upper bound is exclusive.
  */
@Compile
object VestingValidator {

    /** Entry point. Only the spending purpose is valid. */
    inline def validate(scData: Data): Unit = {
        val ctx = scData.to[ScriptContext]
        ctx.scriptInfo match
            case ScriptInfo.SpendingScript(txOutRef, datum) =>
                spend(datum, ctx.redeemer, ctx.txInfo, txOutRef)
            case _ => fail(MustBeSpending)
    }

    /** Implement this. See PROBLEM.md, Part A.
      *
      * `datum` is the locked output's datum, if any; `redeemer` is what the
      * spender supplied; `tx` is the transaction as the script sees it.
      */
    inline def spend(
        datum: Option[Data],
        redeemer: Data,
        tx: TxInfo,
        ownRef: TxOutRef
    ): Unit = {
        val VestingDatum(owner, beneficiary, deadline) =
            datum.getOrFail(InvalidDatum).to[VestingDatum]
        redeemer.to[VestingAction] match
            case VestingAction.Claim =>
                require(
                  tx.validFromOrFail(NoValidityLowerBound) >= deadline,
                  TooEarlyToClaim
                )
                require(tx.isSignedBy(beneficiary), UnsignedBeneficiary)
            case VestingAction.Cancel =>
                require(
                  tx.validToOrFail(NoValidityUpperBound) <= deadline,
                  TooLateToCancel
                )
                require(tx.isSignedBy(owner), UnsignedOwner)
    }

    // Error messages. Use these so the tests can check the reason.
    inline val NotImplemented = "VestingValidator.spend is not implemented"
    inline val MustBeSpending = "Must be a spending script"
    inline val InvalidDatum = "Datum missing or not a VestingDatum"
    inline val NoValidityLowerBound = "Claim needs a validity range lower bound"
    inline val NoValidityUpperBound =
        "Cancel needs a validity range upper bound"
    inline val TooEarlyToClaim =
        "Claim: validity range starts before the deadline"
    inline val TooLateToCancel =
        "Cancel: validity range ends after the deadline"
    inline val UnsignedBeneficiary = "Claim: not signed by the beneficiary"
    inline val UnsignedOwner = "Cancel: not signed by the owner"
}
