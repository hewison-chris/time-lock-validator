package vesting

import scalus.cardano.blueprint.{Blueprint, Contract}
import scalus.compiler.Options
import scalus.uplc.PlutusV3

/** The validator compiled to Plutus V3 (release build) plus its CIP-57
  * blueprint. `compiled.script.script.size` is the on-chain script size.
  */
object VestingContract extends Contract {
    private given Options = Options.release
    lazy val compiled = PlutusV3.compile(VestingValidator.validate)
    lazy val blueprint = Blueprint.plutusV3[VestingDatum, VestingAction](
      title = "Time-locked vesting",
      description =
          "Owner can cancel until the deadline; beneficiary can claim from the deadline on.",
      version = "1.0.0",
      license = Some("Apache License Version 2.0"),
      compiled = compiled
    )
}
