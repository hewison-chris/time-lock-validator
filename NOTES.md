# Notes

Fill this in as you go. Short answers are fine; bullet points are fine.

## Time spent

_How long you spent, and where you stopped if you hit the 2-hour limit._

## What does not pass, and why

_Leave empty if `sbt test` is green._

## Part C: size and cost

Script size is `VestingContract.compiled.script.script.size` (bytes, release build).
Budgets come from a successful run of the validator: `result.budget` in the unit
tests (the worked claim test already prints it with `info`), or
`tx.witnessSet.redeemers.get.value.totalExUnits` on a built transaction.

| Metric                | Value |
|-----------------------|-------|
| Script size (bytes)   |       |
| Claim: CPU steps      |       |
| Claim: memory units   |       |
| Cancel: CPU steps     |       |
| Cancel: memory units  |       |

## Part D: short answers

### 1. What was hardest to learn, and what did you read or try to get past it?

### 2. Two weaknesses, risks or limits of this contract design, and how you would address each

### 3. Guaranteeing that the beneficiary receives the locked amount

_How would you check it on-chain? What is a double satisfaction attack, and could it affect that check?_

## Part E (optional): payout check

_Implementation notes, or a written design if you ran out of time._

## What I would do next
