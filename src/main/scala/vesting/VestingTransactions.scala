package vesting

import scalus.uplc.builtin.Data
import scalus.cardano.address.Address
import scalus.cardano.ledger.*
import scalus.cardano.txbuilder.*
import scalus.cardano.onchain.plutus.v1.PubKeyHash
import scalus.uplc.PlutusV3

import java.time.Instant

/** Off-chain transaction builders for the vesting contract.
  *
  * `claim` and `cancel` set the validity bound the validator needs (lower for
  * `Claim`, upper for `Cancel`) and nothing else; the tests choose the times.
  */
case class VestingTransactions(
    env: CardanoInfo,
    contract: PlutusV3[Data => Unit]
) {
    val scriptAddress: Address = contract.address(env.network)

    private val builder = TxBuilder(env)

    /** Lock `value` at the script for `beneficiary`; `owner` may cancel until
      * `deadline`.
      */
    def lock(
        utxos: Utxos,
        value: Value,
        owner: AddrKeyHash,
        beneficiary: AddrKeyHash,
        deadline: Instant,
        sponsor: Address,
        signer: TransactionSigner
    ): Transaction = {
        val datum =
            VestingDatum(
              PubKeyHash(owner),
              PubKeyHash(beneficiary),
              deadline.toEpochMilli
            )
        builder
            .payTo(scriptAddress, value, datum)
            .complete(availableUtxos = utxos, sponsor = sponsor)
            .sign(signer)
            .transaction
    }

    /** Spend `lockedUtxo` with `Claim`, signed by `beneficiary`, valid from
      * `validFrom` (inclusive) on.
      */
    def claim(
        utxos: Utxos,
        lockedUtxo: Utxo,
        beneficiary: AddrKeyHash,
        payee: Address,
        validFrom: Instant,
        sponsor: Address,
        signer: TransactionSigner
    ): Transaction =
        unlock(
          VestingAction.Claim,
          utxos,
          lockedUtxo,
          beneficiary,
          payee,
          sponsor,
          signer
        )(
          _.validFrom(validFrom)
        )

    /** Spend `lockedUtxo` with `Cancel`, signed by `owner`, valid until
      * `validTo` (exclusive).
      */
    def cancel(
        utxos: Utxos,
        lockedUtxo: Utxo,
        owner: AddrKeyHash,
        payee: Address,
        validTo: Instant,
        sponsor: Address,
        signer: TransactionSigner
    ): Transaction =
        unlock(
          VestingAction.Cancel,
          utxos,
          lockedUtxo,
          owner,
          payee,
          sponsor,
          signer
        )(
          _.validTo(validTo)
        )

    private def unlock(
        action: VestingAction,
        utxos: Utxos,
        lockedUtxo: Utxo,
        signatory: AddrKeyHash,
        payee: Address,
        sponsor: Address,
        signer: TransactionSigner
    )(withValidity: TxBuilder => TxBuilder): Transaction =
        withValidity(
          builder
              .spend(lockedUtxo, action, contract)
              .requireSignature(signatory)
              .payTo(payee, lockedUtxo.output.value)
        )
            .complete(availableUtxos = utxos, sponsor = sponsor)
            .sign(signer)
            .transaction
}
