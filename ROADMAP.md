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

- [ ] **More card backgrounds still.** Nineteen now, up from eleven: night went four → eight,
      daylight six → ten, evening seven → nine. The card also refuses to draw the background it
      is replacing, so back-to-back repeats are gone outright rather than merely rarer.
      Arithmetic is no longer the argument for more. Range is, and the gaps that are left are
      narrower than the ones already filled: the dark set now covers sky, water, weather,
      forest, warmth, emptiness, another person's lights and bare ground, and the light set
      covers cool, cream, pink, bright green, quiet green, fog and violet. What is genuinely
      still missing is anything that is not a *landscape* — every card is a place. A card built
      from pattern or texture rather than a horizon would be the next real widening.
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
- [ ] **The Class B rewrites the plain-English pass declined.** The pass on 2026-07-31 did the
      audit's Class A in full and refused Class B in full, because Class B is the pile where
      shortening a line also moves the claim — "long sitting is linked to worse health outcomes"
      becoming "a long sit costs you" reads better, is shorter, and has quietly promoted a
      correlation to a cost. Those are still worth doing and are the last real prose win left in
      the catalog, but each one costs opening both of its citations and reading them, and there
      is no way to batch that: nothing in the build can tell a rewritten tip from one that
      inherited the wrong citation. Roughly twenty lines, mostly the "finding restated" pattern
      in `general` and `evening`. Do them a handful at a time, and record each in
      `TIP_SOURCES.md` the way the earlier corrections are recorded.
- [ ] **Widen `philosophy` past Europe.** The 07-31 pass paid the debt the 07-30 one left (German
      one line → three, rationalist one → two, plus Hume as a tradition the pool lacked) and
      left a narrower one behind, written into the pool header: apart from the Dhammapada the
      pool is European and Chinese, and there is one woman in it. Wollstonecraft was read for
      that pass and left out on length rather than on merit — the line worth having opens on a
      pronoun the widget has no room to supply. That is a sourcing problem with a solution.
- [ ] **Make a cold tap feel faster.** ~1s is process start plus Glance session setup, not app
      code, and `warmUp()` already hides the catalog parse behind it. No cheap answer left.
- [ ] **Languages beyond `en` and `ru`.** Russian shipped 2026-07-31 and the structure is now
      the cheap part: `SUPPORTED_LANGUAGES`, a `values-<lang>` strings file, a folder of nine
      tip files, and `TipCatalogTest` holds any new language to every invariant English has.
      What is *not* cheap, and did not get cheaper, is 371 lines of prose per language written
      to the pools' own rules. Two debts the Russian took on rather than paid, both written into
      the pool headers: a Russian reader's "why this tip?" shows English citations, and a
      translated philosophy quotation is a rendering of the cited English edition rather than a
      published Russian translation. Both are honest because they are stated, and both would be
      fixed by the same structural change: per-language `_sources.txt`. That is the thing to do
      before a third language, not after.
- [ ] **Get the Russian in front of someone who speaks it.** Written and reviewed by one person
      against the pools' rules, which catches register drift and does not catch the things a
      native reader would wince at. The specific worry is the tone pools: `motivation` has to
      push without turning into the barking Russian imperative, and `wellbeing` has to stay warm
      without sliding into the sympathetic register its first rule forbids.
- [ ] **iOS port**, gated on hardware. The offline guarantee doesn't translate literally (no iOS
      app can declaratively renounce network access) and WidgetKit has no background execution.
