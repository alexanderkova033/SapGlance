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
| ⏳ | 8. During the wait, run the plain-English pass | It wants a fortnight of real use to react to anyway, and a version boundary is the right place to land something that resets tip history. |

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

- [ ] **More card backgrounds still.** Fifteen now: Ember and Slate widened the night palette
      from four to six, and Linen and Mist took the daylight palettes from six to eight. The
      worst odds moved from 1-in-4 to 1-in-6, which is no longer the outlier it was. Where the
      next ones would help most is unchanged in shape though: night is still the narrowest, and
      it is the hour where the card is most likely to be the only lit thing in the room.
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
- [ ] **Grow the jokes group in `wellbeing`.** Started: the group exists with four sourced lines,
      and `WELLBEING` may now carry a single attribution the way a quoted `PHILOSOPHY` line does,
      since sourcing them and claiming them as ours are not compatible. The constraint that makes
      this slow was not costed in the original plan: public-domain humour short enough for a
      widget is almost all Victorian epigram, which is *witty* rather than *warm*, and
      wellbeing's voice is warm. Filling the group with Wilde and Bierce would quietly turn the
      pool acid, and no test catches that. Grow it on one question — does the line still sound
      like it belongs beside "check on the plant"? Jerome K. Jerome passes easily; Bierce is
      capped at one.
- [ ] **A plain-English pass over every pool**, not just the practical ones. The reading half is
      done: [docs/TIGHTENING_AUDIT.md](docs/TIGHTENING_AUDIT.md) has the patterns, worked
      rewrites, and the split between safe compression and rewrites that touch the claim and so
      need their sources re-read. Two things it settled: 39 tips are quotations and can never be
      reworded, and the slack is overwhelmingly on the practical side (mean 75-83 characters
      against the tone pools' 61-63, and four times the hedging) because fidelity to two
      citations inside ~90 characters is what produces that register. Do it in one pass, since
      rewording orphans stored history and restyles every card.
- [ ] **Make a cold tap feel faster.** ~1s is process start plus Glance session setup, not app
      code, and `warmUp()` already hides the catalog parse behind it. No cheap answer left.
- [ ] **Languages beyond `en`.** The UI half is nearly free; the content half is the project —
      316 tips behind a `Locale`-blind classpath lookup, identified by their text everywhere it
      matters, citing English-language sources.
- [ ] **iOS port**, gated on hardware. The offline guarantee doesn't translate literally (no iOS
      app can declaratively renounce network access) and WidgetKit has no background execution.
