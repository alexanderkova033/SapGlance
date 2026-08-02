# Tightening audit

Preparation for the plain-English pass. **No tip text was changed here** — rewording orphans a
user's stored history and changes each tip's `WidgetStyle`, so it happens once, deliberately,
not in pieces. This was the reading half, done in advance.

**The pass ran on 2026-07-31.** What it did with each of the findings below is at the bottom,
under "What the pass actually did". Read that before treating any worked rewrite here as
shipped, because several of them are not — the pass took Class A and declined Class B, and some
of the most quotable rewrites in this document are Class B.

Scope is all nine pools, not just the practical ones the roadmap originally named.

## 39 tips cannot be touched at all

35 philosophy quotations and 4 attributed wellbeing jokes are quoted text. Shortening a
quotation makes it a paraphrase still presented as a quotation, which is the exact dishonesty
those pools exist to prevent. They are out of scope permanently, not just for this pass.

That leaves **277 eligible** of 316.

*(Both figures are as audited. The 07-31 pass added quotations as well as originals, so the
untouchable set is now 46 — 39 philosophy and 7 wellbeing — and the eligible set is 325 of 371.
The ratio barely moved; the rule did not move at all.)*

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

The plan was: do it in one pass, and not yet. Three of STATUS's unverified items — whether the
night hours read as calm, whether the reworked tone pools read better, whether the daylight
palettes look right — all depend on living with the current text, and rewriting it destroys the
evidence being gathered. The natural window looked like the 14-day closed test, when there is
real usage to react to and a version boundary to land it on.

**It ran earlier than that, on 2026-07-31, and the trade is worth stating plainly.** What was
given up is real: the reworded text has never been lived with, so items 2, 3, 4 and now 8 in
STATUS are all still open and all still want the same weeks of wall time. What was bought is
that the closed test now starts *from* the reworded catalog rather than replacing it midway, so
the tip history the rewrite orphans belongs to nobody yet, and the twelve testers read one
version of the app rather than two.

## What the pass actually did

**Class A, in full.** 44 practical lines: `general` 23, `morning` 11, `afternoon` 5, `evening`
4, `sleep_early` 1, `sleep_late` 0. That distribution is itself a result — the two night pools
were written last and at the tone pools' length, so the patterns found almost nothing in them,
and `general` carries half the slack in the catalog. Gerund openers became
verbs, stacked hedges lost the second hedge, filler adverbs went, and the two-clauses-where-one-
does cases were merged. Worked examples from this document that shipped as written: the
`18-20C` line ("Bedrooms sleep better cool"), the screen-free-breaks line, the dropped
`slightly` on the embarrassing-song line, and the dog/bird/cat line.

**Class B, in full — declined.** Not deferred by accident: refused on purpose, and the reason is
the one this document already gives. Removing a hedge, changing a number or turning an
association into a cause needs both citations re-read, one tip at a time, and nothing in the
build can tell a rewritten tip from one that inherited the wrong citation. Doing twenty of those
from memory in the same sitting as forty safe ones is exactly how a catalog acquires a claim its
sources do not make. So the rule for the whole pass was: **no rewrite may change the claim, its
strength, or its hedging.**

That rule is why several rewrites here shipped in a weaker form than this document proposes:

| This document proposed | What shipped | Why |
| --- | --- | --- |
| `Mild dehydration dents focus and mood well before you actually feel thirsty.` → `You lose focus before you feel thirsty.` | `Mild dehydration dents focus and mood before you feel thirsty.` | The short version drops the subject. Ganio is about *mild dehydration*, not about focus in general. |
| `Long sitting is linked to worse health outcomes…` → `A long sit costs you even if you exercise.` | `Long sitting is linked to worse health, even if you exercise regularly.` | "Linked to" is the claim. "Costs you" is a different one. |
| `Sustained poor posture is linked to more fatigue…` → `Holding one posture all day is tiring, not just uncomfortable.` | `Poor posture held all day is linked to fatigue, not just discomfort.` | Same: the association survives, the causal reading does not. |
| `Keeping a nap under about 20 minutes helps you avoid waking up groggy.` → `Nap under 20 minutes and you wake clear rather than groggy.` | `Keep a nap under about 20 minutes and you are less likely to wake up groggy.` | The short version drops both "about" and "helps", promising an outcome the source hedges. |
| `If you eat breakfast, pairing carbs with some protein can help keep energy steadier.` → `…put protein with the carbs. Energy holds steadier.` | `If you eat breakfast, put protein with the carbs. Energy can hold steadier.` | Gerund fixed, "can" kept. |

The remaining Class B lines are on the roadmap as their own item. Roughly twenty, concentrated
in `general` and `evening`, almost all of them the "finding restated" pattern.

### Class B, worked properly on 2026-07-31

The estimate of "roughly twenty" was wrong in an interesting direction. Opening the citations
one at a time turned up **four** changes worth making out of ~20 candidates, because most of
those hedges were not timidity — they were accurate, and the shorter line would have been the
false one.

**Three strengthened, because the hedge understated experimental evidence.** All three were
being described as associations when the cited study had actually *manipulated* the thing:

| Was | Now | What the source turned out to be |
| --- | --- | --- |
| `Stuffy, high-CO2 rooms are linked to slower decisions.` | `Take a room to 1,000 ppm CO2 and decision scores fall.` | Satish is a controlled within-subject experiment: 600 / 1,000 / 2,500 ppm, with significant decrements on six of nine decision-making scales at 1,000. "Linked to" was wrong, and the number was sitting there unused. |
| `A warm shower or bath ... may help you fall asleep faster.` | `A warm bath 1 to 2 hours before bed shortens sleep onset. Ten minutes is enough.` | Haghayegh is a systematic review and meta-analysis, and both the timing and the ten-minute minimum are in the abstract. "May help" was a hedge on a meta-analysis. |
| `Turning to check the clock predicts a longer wait.` | `Clock-watching lengthens the wait and makes you overestimate it.` | Tang et al. *instructed* participants to monitor or not monitor a clock. "Predicts" describes an observational finding that was not what happened. |

**One weakened, because the claim exceeded its sources.** `A short walk or fresh air often helps
more than another coffee` asserted a head-to-head comparison that neither citation runs — one is
a post-lunch-dip review, the other an exercise-and-affect meta-analysis. It now says
`Try a short walk or fresh air before reaching for another coffee`, which is an ordering
suggestion rather than a comparative efficacy claim. **Note the asymmetry this exposes: a
rewrite that *removes* a claim is safe without re-reading the source; only strengthenings need
the paper open.** That is worth knowing, because it is the half of Class B that can be done
cheaply.

**The rest were declined, and the reasons matter more than the changes.**

- `Long sitting is linked to worse health, even if you exercise regularly.` Biswas is a
  meta-analysis of 44 prospective cohorts. Its own conclusion is "independently associated ...
  regardless of physical activity". The current wording is already both accurate and plain.
- `Light while you sleep ... tracks with heart rate, insulin and lipids.` Mason is experimental
  and would license a firmer verb — but only for heart rate and insulin. The *lipids* half rests
  on Obayashi, which is observational, and firming the line would also have made it restate
  `evening`'s 100-lux line, which `sleep_late`'s header forbids.
- `Poor posture held all day is linked to fatigue, not just discomfort.` **Could not be
  checked** at the time: the Applied Ergonomics paper returns 403 and no open-access copy is
  cited. Under the rule, an unopenable source means the claim is not touched. This is the rule
  working rather than a gap. **Settled 2026-08-03 — see below, and it was not a hedging problem
  at all.**
- The other ~15 — media multitasking, two hours outdoors, social ties, sleep regularity, eating
  before bed, blink completeness, social jetlag and the rest — all rest on observational or
  cross-sectional work. "Linked to" and "tracks with" are the honest verbs and they stayed.

The lesson for a future pass: **Class B is not a backlog of timid sentences waiting to be
sharpened.** It is a mixed pile, and roughly four in five of the hedges in it are load-bearing.
Budget the time for reading rather than for writing.

**The tone pools yielded almost nothing, as predicted.** Seven lines in `wellbeing`, zero in
`motivation`, zero among philosophy's originals. The table above the fold said this would happen
— those pools sit at a 61-63 character mean with a quarter of the practical hedging — and it is
worth recording that the prediction held, because the instinct that "all of them could do with
tightening" was measurably wrong about two thirds of the catalog.

**One rule was added during the pass, for the borderline adverbs.** Pattern 4 warns that some
qualifiers are voice rather than filler and names `actually` in the hydration question as a
keeper. The working test that settled the rest: **an adverb stays if removing it changes what
the reader does.** `A cluttered desk quietly competes` lost its `quietly`; `walk to the next
room at a completely different speed` kept its `completely`; `when did you last actually drink
something` kept its `actually`.

## The last Class B line, settled 2026-08-03

`Poor posture held all day is linked to fatigue, not just discomfort.` was the one line the
2026-07-31 pass could not check, and it was left on the roadmap as "find an open version, or
re-source the tip, or leave it hedged forever". **Leaving it hedged turned out not to be
available, because the problem was never the hedge.**

The paper is Waongenngarm et al., *Perceived musculoskeletal discomfort and its association with
postural shifts during 4-h prolonged sitting in office workers*, Applied Ergonomics 89 (2020),
[PMID 32755740](https://pubmed.ncbi.nlm.nih.gov/32755740/). Its full text is still 403 on
ScienceDirect. **Its abstract is not, and the abstract is enough**: the study measured perceived
discomfort on Borg CR-10 and postural shifts from seat-pressure data. Fatigue is not an outcome.
It was never in the paper. So the tip's whole point — *not just discomfort* — was the half its
own citation did not support, and the second citation was a Cornell workstation-setup page that
makes no claim about posture, discomfort or fatigue at all.

**Method note, and the reason this took one fetch rather than none.** The rule that stopped the
last pass was "an unopenable source means the claim is not touched", and it is a good rule. What
it missed is that *paywalled* and *unreadable* are different: a PubMed abstract is free for
essentially every paper behind a publisher paywall, and an abstract settles what a study
*measured* even when it cannot settle effect sizes. Check the abstract before recording a source
as unopenable.

**Trying to keep the claim, and failing honestly.** There is real evidence that slumped sitting
fatigues trunk muscles: Waongenngarm et al. 2015,
[PMC4792914](https://pmc.ncbi.nlm.nih.gov/articles/PMC4792914/), put 30 office workers in three
postures for an hour and found EMG median-frequency decline in internal oblique / transversus
abdominis **in slumped sitting only**. One study, two deep abdominal muscles, EMG rather than
felt tiredness. The obvious second citation, Jung et al., *Medicina* 2020,
[PMC7822118](https://pmc.ncbi.nlm.nih.gov/articles/PMC7822118/), looked for the same effect and
**found no significant difference in median frequency**. Citing it as support would have been a
misrepresentation of a negative result. One study does not clear this pool's two-independent-
citations bar, so the claim went rather than the hedge.

**What replaced it, from the same paper that killed it.** Waongenngarm 2015's other finding is
the interesting one and inverts the tip's own premise: *"Regardless of the sitting posture, Body
Perceived Discomfort scores in the neck, shoulder, upper back, low back, and buttock
significantly increased after 1 hour of sitting"* — upright and forward-leaning included. The
line is now `Discomfort rose after an hour of sitting in every posture tested, upright included.`,
cited to that study and to the 2020 one, which found the same rise continuing across four hours.
It is a myth-buster carrying a real number, it agrees with `Your best posture is your next one`
two lines away, and it is the rare rewrite that is both *plainer* and *better supported* than
what it replaced.

**One incidental win.** The paywalled ScienceDirect URL was cited by five tips. All five now
point at the PubMed record instead — same paper, same claim, openable by anyone who taps
"why this tip?".
