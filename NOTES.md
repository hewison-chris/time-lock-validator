# Notes

Fill this in as you go. Short answers are fine; bullet points are fine.

## Time spent

About 2 hours

## What does not pass, and why

All pass

## Part C: size and cost

Script size is `VestingContract.compiled.script.script.size` (bytes, release build).
Budgets come from a successful run of the validator: `result.budget` in the unit
tests (the worked claim test already prints it with `info`), or
`tx.witnessSet.redeemers.get.value.totalExUnits` on a built transaction.

| Metric                | Value   |
|-----------------------|---------|
| Script size (bytes)   |309      |
| Claim: CPU steps      |9,451,699|
| Claim: memory units   |25,518   |
| Cancel: CPU steps     |9,565,362|
| Cancel: memory units  |25,750   |

## Part D: short answers

### 1. What was hardest to learn, and what did you read or try to get past it?

### 2. Two weaknesses, risks or limits of this contract design, and how you would address each

### 3. Guaranteeing that the beneficiary receives the locked amount

_How would you check it on-chain? What is a double satisfaction attack, and could it affect that check?_

## Part E (optional): payout check

_Implementation notes, or a written design if you ran out of time._

## What I would do next
