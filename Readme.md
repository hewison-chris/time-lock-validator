# Time-Locked Vesting Validator (Scalus + Scala 3)

A take-home assessment. **Start with [PROBLEM.md](PROBLEM.md)**: it describes the task,
what is provided, what you must write, and the deliverables. Write your answers in
[NOTES.md](NOTES.md).

## Prerequisites

- JDK 17 or newer
- [sbt](https://www.scala-sbt.org/) (or [Scala CLI](https://scala-cli.virtuslab.org/); both work)

The first run downloads dependencies and compiles, which can take a few minutes.

## Running

```sh
sbt test                                      # everything
sbt "testOnly vesting.VestingValidatorTest"   # unit tests (hand-made script contexts)
sbt "testOnly vesting.VestingEmulatorTest"    # end-to-end tests (in-memory emulator)
```

The same with Scala CLI: `scala-cli test .`, `scala-cli fmt .`.

Before you implement the validator, `sbt test` compiles and the worked tests fail; the
rest are `pending`. Your job is to turn everything green.

## Layout

```
src/main/scala/vesting/
  VestingValidator.scala      on-chain validator: datum, redeemer, entry point, errors. You implement `spend`.
  VestingContract.scala       given: compiles the validator to Plutus V3 (+ CIP-57 blueprint)
  VestingTransactions.scala   given: off-chain lock / claim / cancel transaction builders
src/test/scala/vesting/
  VestingFixtures.scala       given: hand-made script contexts for unit tests
  VestingValidatorTest.scala  2 worked tests, the rest pending. You write the rest.
  VestingEmulatorTest.scala   1 worked test, the rest pending. You write the rest.
NOTES.md                      you fill this in
PROBLEM.md                    the assessment
```

## Useful switches

```sh
SCALUS_PROFILE=1 sbt test     # write an execution-budget profile to target/profile.html
sbt blueprint                 # generate the CIP-57 blueprint under target/
```

## Learn more

- Scalus documentation: https://scalus.org/docs (start with "Validators in depth" and the HTLC tutorial)
- Example contracts and tests: https://github.com/scalus3/scalus/tree/master/scalus-examples
