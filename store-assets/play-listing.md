# Play Store listing copy

Draft copy for the Play Console listing, positioned against the quote-widget apps this
competes with rather than against health trackers. Every number here was counted from the
repo on 2026-07-28 — re-check them if the catalog changes.

Play's limits: title 30 characters, short description 80, full description 4000.

---

## Title (29 / 30)

```
SapGlance: Quote & Tip Widget
```

"Widget" earns its place: it is both the form factor and the word people type. Naming quotes
*and* tips sets the expectation that this is a superset of a quotes app, not a different
thing someone has to be talked into.

## Short description (73 / 80)

```
Quotes and evidence-backed tips on your home screen. No ads, no tracking.
```

This is the line that shows in search results, so it carries the most weight after the title.
Worth A/B testing against a version that leads with the differentiator instead:

```
A quote or a useful tip, matched to the time of day. No ads, no tracking.
```

## Full description

```
Most quote widgets recycle the same unattributed lines and put an ad underneath. SapGlance
gives you something worth reading and asks nothing in return.

One card on your home screen. One line on it. Tap it for another.


TWO KINDS OF LINE, AND YOU PICK THE MIX

- Practical tips grounded in published research: light levels, sleep timing, breathing rate,
  screen breaks, room temperature. Each one carries its sources, and anything resting on
  contested evidence was left out rather than rounded up.

- Motivation, philosophy and wellbeing, including quotations from Marcus Aurelius, Spinoza,
  Thoreau, Montaigne, Frederick Douglass and George Eliot, checked word for word against the
  original texts instead of copied from the internet.

One slider moves you from mostly practical to mostly reflective. Put it wherever you like.


IT KNOWS WHAT TIME IT IS

Mornings lean motivational. Afternoons lean practical. Evenings turn toward wellbeing, and
late at night it winds down instead of shouting at you. A shuffled deck of quotes cannot do
that.


IT DOESN'T REPEAT ITSELF

371 tips, in English and Russian. The last 100 you have seen never come back, and the 60 before
those are weighted down, so the rotation keeps finding you something you have not read yet.


100% OFFLINE. ZERO DATA COLLECTED.

No account. No sign-up. No analytics, no crash reporter, no advertising SDK. SapGlance does
not request the INTERNET permission at all, which means it is technically incapable of
sending anything anywhere. That is a stronger guarantee than a privacy policy, because it
does not depend on trusting us. Your tips stay on your phone.


ELEVEN CARD STYLES

Forest, Ocean, Sunset, Midnight, Aurora, Dawn, Rain, Winter, Paper, Meadow and Blossom.
Place it on your home screen or your lock screen, resize it, and the card adapts to the room
you give it.


WHAT IT IS NOT

SapGlance does not count steps, read your heart rate or track your sleep, and it is not a
medical app. It shows you one good line at a time. That is the whole idea.


FREE, AND FREE OF THE USUAL THINGS

No ads. No subscription. No in-app purchases. No premium tier. Open source under the MIT
licence.
```

---

## Listing settings

- **Category: Personalization.** Where widget browsers look, where the competing quote
  widgets sit, and it avoids the Health apps declaration that a Health & Fitness listing
  would trigger. This app holds no health permissions and reads no health data, so that
  declaration would be friction for nothing.
- **Tags:** widget, quotes, motivation, wellbeing, personalization.
- **Content rating:** the IARC questionnaire should come back Everyone — no user content,
  no purchases, no ads, no data collection.
- **Data safety:** "no data collected, no data shared." The absent INTERNET permission is the
  evidence, and it is checkable by anyone who unzips the APK.
- **Ads:** none. Declare it, since the absence is part of the pitch.

## Still needed for the listing

- Feature graphic, 1024x500. Draft master is `play-feature-graphic-1024x500.svg`; Play needs
  PNG or JPEG, so it has to be exported at exactly 1024x500 before upload.
- At least two phone screenshots (see below).
- Privacy policy at a public URL. The Pages workflow is in `.github/workflows/pages.yml`; it
  cannot run until Settings > Pages > Source is set to "GitHub Actions".

## Taking the screenshots

Two constraints that are easy to get wrong and cost a review round trip each:

- **Not on your own home screen.** A listing is public. The obvious shot — the widget where it
  actually lives — also publishes your wallpaper, your app drawer, and anything else on that
  page. On the test device that currently includes a weather widget naming the city and a
  banking app. Use a clean home screen: plain wallpaper, no other widgets, no personal apps.
- **Play rejects anything longer than 2:1.** The rule is that the longest side may not be more
  than twice the shortest. The test device is 1080x2340, which is 2.17:1, so a raw `screencap`
  fails. Crop to 1080x2160 or shorter. 24-bit PNG with no alpha, or JPEG.

Worth showing, in rough priority: the widget at 2x2 on a home screen, the same widget at 4x4
(the layout genuinely differs, it is not one design stretched), one pale daytime card and one
deep night card so the palette range is visible, and the settings screen's "Why this tip?" card,
which is the feature nothing else in the category has.

Done so far, both cropped from 1080x2340 to 1080x2160 to clear the 2:1 rule:

- `play-screenshot-01-widget-2x2.png` — pale Meadow card, 2x2, a motivation line.
- `play-screenshot-02-widget-4x4.png` — deep Sunset card at 401x603dp, a philosophy quotation.
  Deliberately the opposite of the first on every axis a viewer can see: size, layout (the
  quote glyph only appears once the card is tall enough), and palette. Two screenshots that
  differ only in size would waste one of them.

- `play-screenshot-03-why-this-tip.png` — the settings card, showing a practical tip with
  "Backed by 2 sources" and both citations as tappable links, plus the variety control. This is
  the differentiator shot: every competing quote widget can show a nice card, none of them can
  show you where the claim came from.

That is the set. Three is above Play's minimum of two, and they cover the three things worth
knowing before installing: what it looks like small, what it looks like large, and that the
advice is sourced. Upload them in that order, since Play shows the first one largest.
