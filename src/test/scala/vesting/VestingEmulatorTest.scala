package vesting

import org.scalatest.funsuite.AnyFunSuite
import scalus.cardano.ledger.*
import scalus.cardano.node.Emulator
import scalus.testing.kit.Party
import scalus.testing.kit.Party.{Alice, Bob, Eve}
import scalus.testing.kit.{ScalusTest, TestUtil}
import scalus.utils.await

import java.time.Instant

/** End-to-end tests: real transactions against the in-memory emulator.
  *
  * Alice is the owner, Bob the beneficiary, Eve a stranger. The validator is
  * evaluated while a transaction is built, so a rejected spend throws during
  * the build; `assertScriptFail(reason) { buildTx }` (from `ScalusTest`)
  * asserts that it failed for `reason`.
  *
  * One test is worked. Replace every `pending` with a real test; see
  * PROBLEM.md, Part B, for the list.
  */
class VestingEmulatorTest extends AnyFunSuite, ScalusTest {

    private given env: CardanoInfo = TestUtil.testEnvironment
    private val contract = VestingContract.compiled.withErrorTraces
    private val txs = VestingTransactions(env, contract)

    // Time model: the deadline is slot 100. The emulator starts at slot 0;
    // `provider.setSlot` moves it. A transaction is only accepted while the
    // current slot is inside its validity range.
    private val deadlineSlot: SlotNo = 100
    private val beforeSlot: SlotNo = deadlineSlot - 10
    private val afterSlot: SlotNo = deadlineSlot + 10
    private val deadline: Instant = env.slotConfig.slotToInstant(deadlineSlot)
    private val beforeDeadline: Instant =
        env.slotConfig.slotToInstant(beforeSlot)
    private val afterDeadline: Instant = env.slotConfig.slotToInstant(afterSlot)

    private val lockedValue = Value.ada(10)

    private def newEmulator: Emulator =
        Emulator.withAddresses(Seq(Alice.address, Bob.address, Eve.address))

    private def utxosOf(provider: Emulator, party: Party): Utxos =
        provider.findUtxos(party.address).await().toOption.get

    /** Alice locks 10 ada for Bob with the deadline at slot 100. */
    private def lock(provider: Emulator): Utxo = {
        val lockTx = txs.lock(
          utxos = utxosOf(provider, Alice),
          value = lockedValue,
          owner = Alice.addrKeyHash,
          beneficiary = Bob.addrKeyHash,
          deadline = deadline,
          sponsor = Alice.address,
          signer = Alice.signer
        )
        assert(provider.submit(lockTx).await().isRight)
        Utxo(lockTx.utxos.find { case (_, out) =>
            out.address == txs.scriptAddress
        }.get)
    }

    // 10. Owner locks, beneficiary claims after the deadline.
    test("owner locks, beneficiary claims after the deadline") {
        val provider = newEmulator
        val lockedUtxo = lock(provider)
        provider.setSlot(afterSlot)

        val claimTx = txs.claim(
          utxos = utxosOf(provider, Bob),
          lockedUtxo = lockedUtxo,
          beneficiary = Bob.addrKeyHash,
          payee = Bob.address,
          validFrom = afterDeadline,
          sponsor = Bob.address,
          signer = Bob.signer
        )
        val result = provider.submit(claimTx).await()
        assert(result.isRight, s"Emulator rejected the claim: $result")
        assert(
          provider.findUtxos(txs.scriptAddress).await().toOption.get.isEmpty
        )
    }

    // 11. Owner locks, owner cancels before the deadline.
    test("owner locks, owner cancels before the deadline")(pending)

    // 12. The rejection cases. Assert the reason, not just that it failed.
    test("claim too early is rejected")(pending)

    test("claim by a stranger is rejected")(pending)

    test("cancel too late is rejected")(pending)

    test("cancel by a stranger is rejected")(pending)
}
