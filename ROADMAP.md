# Roadmap

Where SapGlance is going, and what is standing between it and the Play Store. Completed work
lives in the git history rather than here. Current state — what is verified, what is risky — is
in [STATUS.md](STATUS.md).

## Getting to release, in order

**The code is not what is blocking.** A personal Play account created after Nov 2023 must run a
closed test with **12 testers opted in for 14 consecutive days** before production, and that
clock only starts at the first upload. The clock is the schedule; everything below is ordered by
what unblocks what.

| | Step | |
| --- | --- | --- |
| ✅ | 1. Back up the signing key | Done 2026-07-31, off this machine. Unverified from here: open the copy once with `keytool -list -v -keystore <copy>` and the risk is genuinely closed. `keystore/healthwidget-release.jks` is useless without `keystore/keystore.properties`, so both had to travel. Losing it means the listing can never be updated again, only replaced under a new `applicationId`. |
| ⏳ | 2. The Play Console account | **The only real blocker left.** See below. |
| ✅ | 3. Merge to `main` and push | CI and the Pages workflow only run there. |
| ✅ | 4. Turn on Pages | Policy live at https://alexanderkova033.github.io/SapGlance/ |
| ✅ | 5. Screenshots | Three, in `store-assets/`. |
| ⏳ | 6. Export the feature graphic | `store-assets/play-feature-graphic-1024x500.svg` → PNG at exactly 1024x500. Minutes. |
| ⏳ | 7. Upload, recruit 12 testers, wait 14 days | Recruiting is the long pole: realistically three weeks from first upload. |
| ✅ | 8. The plain-English pass | Done 2026-07-31, ahead of the closed test rather than during it. That trades away the fortnight of real use it was meant to react to; what it buys is that the *first* build 12 testers ever see is the reworded one, so the tip history it resets is nobody's. Catalog went 316 → 371 in the same pass. |

### Step 2: the account

**The binding constraint isn't technical.** The developer is under 18; a Play Console account
requires the holder to be 18+, enforced at signup and again by the payments profile. The only
legitimate route is a **parent or guardian owning the account**, with the developer added as a
user with release permissions. That means the $25 fee, an identity check, and the guardian's
name and address shown publicly on the listing.

What makes it an easy ask: no data collection (structurally — no `INTERNET` permission), no ads,
no payments, no accounts, no user content, no medical claims.

F-Droid and GitHub Releases were considered and rejected on reach, and are worth revisiting only
if the guardian route fails.

### Still outstanding for the listing

Everything else is ready: signing config, the 512px icon, the drafted listing copy, three
screenshots, `targetSdk 36` (which clears the Aug 2026 requirement), `versionName 1.0.0`, and
the live privacy-policy URL. What is missing is the account and the feature graphic as a PNG.

## Open work

- [ ] **Read the reworked Russian.** All nine pools were passed over on 2026-08-02 and 08-03
      after the verdict that the catalog read as unintentionally funny; the four recurring faults
      and the rules that came out of them are in `ru/general.txt`'s header, and `motivation` is
      the pool that was rebuilt rather than repaired. What is *not* done is an independent read:
      the same person wrote both versions, so "better" currently rests on one round of feedback
      applied by the author of the problem. The gendered past tense is closed and checkable; the
      register is not. Worth reading `motivation` and `wellbeing` first, since those are the two
      pools where Russian pulls hardest away from the English — into barking and into sympathy
      respectively, both of which their own headers forbid.

- [ ] **More card backgrounds still.** Nineteen now, up from eleven: night went four → eight,
      daylight six → ten, evening seven → nine. The card also refuses to draw the background it
      is replacing, so back-to-back repeats are gone outright rather than merely rarer.
      Arithmetic is no longer the argument for more. **Composition is.** The first eight new
      styles were judged "almost the same and quite boring" and the source says why: every path
      in every one of them started at `M0,150`, the bottom-left corner. They were all a band
      across the bottom, recoloured. The two originals that read as distinct, Blossom and Paper,
      are the two whose paths start anywhere else.
      So the rule for the next one is: **decide the composition first and the hue last.** Hue
      barely survives the 0.30 white or 0.42 black scrim the card draws over it; where the shapes
      sit does. Lilac is now a halo, Sage a diagonal frond, Linen a weave — the last of those is
      also the first card in the set that is not a landscape at all. Five cards still share the
      bottom-band motif (Meadow, Mist, Winter, Canyon, Harbour), which is four too many.
      Adding one is three edits and each is enforced rather than remembered: a `WidgetStyle`
      entry (the constructor makes you declare `isLight`), a drawable in `:app`'s exhaustive
      `when` (compile error if missing), and membership of at least one palette (a test fails if
      a style is unreachable, so new artwork cannot ship dead). Ink is derived from `isLight`, so
      there is no second colour choice to get wrong. The one thing no check covers: the art has
      to survive the whole-card scrim it will be drawn under, 0.42 black on a dark style and 0.30
      white on a pale one, and still read as artwork rather than as texture.
      Worth knowing before drawing another: the set needs range, not more of its centre. Ember
      is the only warm dark card and Slate the only card with no subject at all, and both exist
      because six cool cards with something to look at had made the night rotation feel narrower
      than its count.
- [x] ~~**The Class B rewrites nobody can open the sources for.**~~ Closed 2026-08-03, and the
      answer was none of the three the item offered. "Poor posture held all day is linked to
      fatigue" was not a hedging problem: the paper's abstract — free on PubMed even though the
      full text is still 403 — shows it measured discomfort and postural shifts, and **never
      measured fatigue at all**, while the second citation was a workstation-setup page making no
      claim of any kind. An attempt to keep the claim on better evidence failed honestly: one
      study supports slumped-sitting muscle fatigue and a second looking for the same effect
      found nothing, which does not clear the two-citation bar. The line was re-sourced to what
      its own paper does show, and inverts its old premise: *discomfort rose after an hour in
      every posture tested, upright included*. Full reasoning in
      [docs/TIGHTENING_AUDIT.md](docs/TIGHTENING_AUDIT.md).
      **The transferable lesson: paywalled is not unreadable.** A PubMed abstract is free for
      almost every paywalled paper and settles what a study *measured*, which is most of what a
      citation check needs. Check the abstract before recording a source as unopenable. Five tips
      cited that paywalled URL and all five now point at the open PubMed record.
- [ ] **Widen `philosophy` further still.** Done twice. On 2026-07-31: Zhuangzi widened the
      Taoist section, and Saadi (Persian), Tagore (Indian) and Anna Julia Cooper (a second woman,
      and a nineteenth-century American voice next to Douglass) were traditions the pool did not
      have at all. On 2026-08-02 the three gaps that pass named were all closed, and its
      diagnosis — a public-domain problem rather than a values one — held up: Ptahhotep (Egyptian,
      and now the oldest text in the file by two millennia), an Akan proverb from Rattray, two
      lines from Ohiyesa, and Bertrand Russell, whose *Conquest of Happiness* only entered the US
      public domain this January and reached Gutenberg in February. That last one is the useful
      lesson: the twentieth-century gap was a copyright boundary, and it moves a year every year,
      so it is worth re-checking rather than treating as permanent.
      On 2026-08-03 the two gaps that left were closed by one book. Virginia Woolf's *A Room of
      One's Own* is 1929, so it is a woman and the near side of 1926 at once, and it only became
      quotable when it entered the US public domain in January 2025. Elizabeth Cady Stanton's
      *Solitude of Self* came with it as a fourth woman and a second nineteenth-century American
      voice — and it argues *against* the pool's existing solitude lines rather than with them,
      which is worth more than another voice agreeing.
      **The pattern is now the useful part: twice running, a gap turned out to be a copyright
      date rather than a judgement.** When this pool looks narrow, check what came out of
      copyright this January before concluding anything about the pool.
      What is left is narrower still. The last hundred years is two books deep rather than one,
      which is not yet a range: Woolf and Russell wrote in the same decade, in the same country,
      at each other's distance from the same literary establishment. And Wollstonecraft remains
      the known near-miss: the line worth having opens on a pronoun the widget cannot supply.
      One caveat is recorded in the pool header rather than here because it is a judgement someone
      may want to revisit: the standard public-domain Akan collection is Rattray's, whose full
      1916 subtitle is offensive, and a reader who taps "why this tip?" and follows the link lands
      on it.
- [ ] **Languages beyond `en` and `ru`.** Russian shipped 2026-07-31 and the structure is now
      the cheap part: `SUPPORTED_LANGUAGES`, a `values-<lang>` strings file, a folder of nine
      tip files, and `TipCatalogTest` holds any new language to every invariant English has.
      What is *not* cheap, and did not get cheaper, is 385 lines of prose per language written
      to the pools' own rules. Two debts the Russian took on rather than paid, both written into
      the pool headers: a Russian reader's "why this tip?" shows English citations, and a
      translated philosophy quotation is a rendering of the cited English edition rather than a
      published Russian translation. Both are honest because they are stated, and both would be
      fixed by the same structural change: per-language `_sources.txt`. That is the thing to do
      before a third language, not after.
- [ ] **iOS port.** Costed properly on 2026-07-31: **[docs/IOS_PORT.md](docs/IOS_PORT.md)**.
      The short version is that the code is the easy part — `:core` touches exactly three
      non-multiplatform things and is 1-2 days from compiling against an iOS target — and that
      this should not be decided on effort. Two of the things the README leads with do not
      survive the platform: the offline guarantee stops being structural, because there is no
      `INTERNET` permission to omit and no entitlement to renounce. (The other one, the
      90-minute screen-on rule, stopped being a problem on 2026-07-31 — it was replaced by a
      fixed four-times-a-day schedule, which WidgetKit's timeline model expresses natively. The
      iOS doc is updated but that section is now the *easy* half of the port.) Also worth knowing early: Apple Developer Program enrollment
      has the same 18+ requirement as Play, so it is a *second* ask of the same guardian, plus
      $99/year and a Mac. Recommendation in the doc: not before v1 is live on Android, and
      settle the two promises on paper before anyone opens Xcode.
