# Tightening audit

Preparation for the plain-English pass. **No tip text is changed here** — rewording orphans a
user's stored history and changes each tip's `WidgetStyle`, so it happens once, deliberately,
not in pieces. This is the reading half done in advance.

Scope is all nine pools, not just the practical ones the roadmap originally named.

## 39 tips cannot be touched at all

35 philosophy quotations and 4 attributed wellbeing jokes are quoted text. Shortening a
quotation makes it a paraphrase still presented as a quotation, which is the exact dishonesty
those pools exist to prevent. They are out of scope permanently, not just for this pass.

That leaves **277 eligible** of 316.

## Where the slack actually is

| Pool | n | mean chars | lines carrying a hedge |
| --- | --- | --- | --- |
| practical (6 pools) | 151 | 75–83 | 26% |
| tone (3 pools) | 165 | 61–63 | 6% |

The tone pools are already tight. They were written to be *said*; the practical pools were
written to be *faithful*, and fidelity to two citations inside ~90 characters is what produces
the register of the abstract they came from. So "all of them could do with tightening" is right
in spirit, and the yield is very uneven: roughly 20 characters and four times the hedging sit on
the practical side. Budget the effort there.

## The five patterns

**1. Stacked hedges.** One hedge is honest. Two is mush, and the second usually adds no caution
the first didn't.

- `A cooler bedroom supports better sleep. Around 18-20C (65-68F) is the usual advice.` (83)
  → `Bedrooms sleep better cool. 18-20C (65-68F) is the usual advice.` (63)
- `If you eat breakfast, pairing carbs with some protein can help keep energy steadier.` (84)
  → `If you eat breakfast, put protein with the carbs. Energy holds steadier.` (71)

**2. The gerund opener.** The practical pools start on a verbal noun far more than the tone ones
do, and it pushes the verb to the middle of the sentence.

- `Waking up at roughly the same time each day keeps your body's rhythm steady.` (76)
  → `Wake at the same time each day. That is what steadies the rhythm.` (64)
- `Keeping a nap under about 20 minutes helps you avoid waking up groggy.` (70)
  → `Nap under 20 minutes and you wake clear rather than groggy.` (58)

**3. The finding restated.** Named in the roadmap and still the worst of them: a result reported
rather than a thing a person says.

- `Mild dehydration dents focus and mood well before you actually feel thirsty.` (76)
  → `You lose focus before you feel thirsty.` (38)
- `Long sitting is linked to worse health outcomes, even if you exercise regularly.` (80)
  → `A long sit costs you even if you exercise.` (41)
- `Sustained poor posture is linked to more fatigue over the day, not just discomfort.` (83)
  → `Holding one posture all day is tiring, not just uncomfortable.` (61)

**4. Qualifiers doing nothing.** `quietly`, `properly`, `genuinely`, `actually`. Careful here:
some are voice, not filler. `When did you last *actually* drink something?` needs its "actually"
— it is the conversational shrug that makes the line a question rather than an instruction.
`A cluttered desk *quietly* competes for your attention` does not.

**5. Two clauses where one does.**

- `Screen-free breaks count twice: your eyes and your attention both get a rest.` (77)
  → `Screen-free breaks rest your eyes and your attention both.` (57)

The tone pools yield a handful of the same kind, no more:

- `Put on something you'd be slightly embarrassed to have playing when someone walks in.` (85)
  → drop `slightly`.
- `If there's a dog, a bird, or a cat within sight, that's a legitimate use of a minute.` (85)
  → `A dog, a bird or a cat within sight is a legitimate use of a minute.` (67)

## Sort every rewrite into one of two piles

This is the part that makes the pass safe, and it is why the audit is worth doing before the
rewrite rather than during it.

**Class A, safe compression.** Word-level. The claim, its strength and its hedging come out
unchanged. No need to open the citation. Patterns 2, 4 and 5 are almost entirely Class A.

**Class B, claim-touching.** Removing a hedge, changing a number, or turning an association into
a cause. Every one of these has to be re-read against both sources before it ships, because
**nothing in the build can tell a rewritten tip from one that inherited the wrong citation** —
`general.txt`'s own header says exactly this, and `TIP_SOURCES.md` records two corrections that
were already caught this way. Most of pattern 1 and all of pattern 3 are Class B.

`Long sitting is linked to worse health outcomes` → `A long sit costs you` is the shape to watch:
it reads better, it is shorter, and it has quietly promoted a correlation to a cost. Whether the
source supports that is a question, not an assumption.

## Sequencing

Do it in one pass, and not yet. Three of STATUS's unverified items — whether the night hours read
as calm, whether the reworked tone pools read better, whether the daylight palettes look right —
all depend on living with the current text. Rewriting it now destroys the evidence being
gathered. The natural window is during the 14-day closed test, when there is real usage to react
to and a version boundary to land it on.
