package vesting

import scalus.uplc.builtin.Data
import scalus.uplc.builtin.Data.toData
import scalus.cardano.onchain.plutus.v1.{
    Address,
    Credential,
    Interval,
    IntervalBound
}
import scalus.cardano.onchain.plutus.v3.*
import scalus.cardano.onchain.plutus.prelude.*
import scalus.cardano.onchain.plutus.prelude.Option.*
import scalus.testing.kit.TestUtil

/** Hand-made script contexts for the unit tests.
  *
  * Everything is deterministic: the same inputs give the same context, so a
  * failing test is reproducible.
  */
object VestingFixtures {
    val owner: PubKeyHash = TestUtil.mockPubKeyHash(1)
    val beneficiary: PubKeyHash = TestUtil.mockPubKeyHash(2)
    val stranger: PubKeyHash = TestUtil.mockPubKeyHash(3)
    val scriptHash: ValidatorHash = TestUtil.mockScriptHash(0)

    val deadline: PosixTime = BigInt(1_700_000_000_000L)
    val hour: PosixTime = BigInt(60 * 60 * 1000)

    val datum: VestingDatum = VestingDatum(owner, beneficiary, deadline)
    val locked: Lovelace = BigInt(10_000_000)
    val fee: Lovelace = BigInt(200_000)

    private val ownRef: TxOutRef = TestUtil.mockTxOutRef(0, 0)
    private val ownInput: TxInInfo = TxInInfo(
      outRef = ownRef,
      resolved = TxOut(
        address = Address(Credential.ScriptCredential(scriptHash), None),
        value = Value.lovelace(locked)
      )
    )

    // Validity ranges as the ledger builds them from a transaction:
    // the lower bound is inclusive, the upper bound is exclusive.

    /** `[from, +inf)`: what `TxBuilder.validFrom` alone produces. */
    def from(time: PosixTime): Interval = Interval.after(time)

    /** `(-inf, to)`: what `TxBuilder.validTo` alone produces. */
    def upTo(time: PosixTime): Interval = Interval.entirelyBefore(time)

    /** `[from, to)`: both bounds set. */
    def between(from: PosixTime, to: PosixTime): Interval =
        Interval(
          IntervalBound.finiteInclusive(from),
          IntervalBound.finiteExclusive(to)
        )

    /** No validity range at all: `(-inf, +inf)`. */
    val unbounded: Interval = Interval.always

    /** A spending script context for the locked output.
      *
      * @param action
      *   the redeemer
      * @param signedBy
      *   the transaction's signatories
      * @param validRange
      *   the transaction's validity range
      * @param datum
      *   the datum the script sees; `None` simulates a missing datum
      * @param payee
      *   who receives the locked amount (less the fee)
      */
    def spendContext(
        action: VestingAction,
        signedBy: List[PubKeyHash],
        validRange: Interval,
        datum: Option[Data] = Some(datum.toData),
        payee: PubKeyHash = beneficiary
    ): ScriptContext = ScriptContext(
      txInfo = TxInfo(
        inputs = List(ownInput),
        outputs = List(
          TxOut(
            address = Address(Credential.PubKeyCredential(payee), None),
            value = Value.lovelace(locked - fee)
          )
        ),
        fee = fee,
        validRange = validRange,
        signatories = signedBy,
        id = ownRef.id
      ),
      redeemer = action.toData,
      scriptInfo = ScriptInfo.SpendingScript(ownRef, datum)
    )
}
