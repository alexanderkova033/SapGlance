# Scientific sources for tip content

The tips shown in the widget (`core/src/main/resources/tips/*.txt`) are short, hedged
suggestions ("can help," "is linked to"), not medical claims. This document records the
research behind each one, grouped by theme, so the wording can be checked or updated as
evidence changes. It is documentation only: it isn't read by the app.

## How citation works here

Every tip carries **at least two independent sources**, not one. A single citation reads as
one study's opinion, and a short health claim aimed at a general audience deserves better
than that; two or three sources that independently agree are what make a claim worth showing
on someone's home screen. `Tip.MIN_SOURCES` is the floor and `TipCatalogTest` fails the build
if any tip drops below it, cites a non-HTTPS URL, or lists the same URL twice (which would
game the count without adding evidence).

**Sources deliberately repeat across tips.** A meta-analysis on sedentary time legitimately
backs every sitting-related tip, so it appears on all of them. What is not allowed is a tip
leaning on a single source, or the same source appearing twice within one tip.

Source-quality rules used when picking citations:

- **Prefer the primary study** (PubMed, the publisher's DOI page, PMC) over a press release
  or news write-up. Where a plain-language summary is genuinely useful to a reader who hits a
  paywall, it is included *in addition to* the primary source, never instead of it (e.g. the
  Stanford News piece alongside Oppezzo & Schwartz's APA paper).
- **No Wikipedia, no ResearchGate, no auto-generated aggregator pages** as citations. Those
  were used in an earlier pass and have all been replaced (see "Corrections" below).
- **Government, university, and professional-body guidance** (OSHA, NASA, Cornell Ergonomics,
  AOA, AASM) is acceptable for standard practice guidance that isn't a single-study finding,
  and is paired with a primary study wherever one exists.
- **Consumer health sites** (Sleep Foundation) appear only as a secondary source next to a
  peer-reviewed primary one, never alone.

## Corrections made in this pass

Checking whether each source actually supported its tip turned up four real problems:

1. **The snooze tip contradicted its own source.** The tip said repeated snoozing "can leave
   you groggier than getting up with the first alarm," cited to Sundelin et al. (2024). That
   study found the opposite: 30 minutes of snoozing improved or did not affect cognitive
   performance on rising, cost about 6 minutes of sleep, and helped avoid waking out of
   slow-wave sleep. The tip now says a short snooze is fine and that the research is kinder to
   it than its reputation.
2. **The post-lunch carb-crash tip was cited to Wikipedia** ("Reactive hypoglycemia") and
   overstated the evidence. Orr et al. (1997) found no significant sleep-latency difference
   between meal compositions; the better-supported claim is that a *large* lunch deepens a dip
   that is substantially circadian to begin with. Reworded and re-cited to Monk, Orr, and a
   2025 scoping review.
3. **The Monk citation pointed at the wrong paper.** It was labelled *Chronobiology
   International* but linked a Semantic Scholar record for the 2005 *Clinics in Sports
   Medicine* review. Both are now cited, correctly labelled, via PubMed.
4. **"Walking roughly doubled creative output" overstated Oppezzo & Schwartz.** The reported
   figure is a ~60% average increase in creative output (81% of participants improved on the
   alternate-uses test). The tip itself only ever said "more than sitting," which is fine; this
   document's summary was the thing that overreached, and is fixed below.

Sources dropped for quality and what replaced them: Wikipedia → Monk/Orr/*Nutrients*;
ResearchGate (Reinecke) → the Taylor & Francis DOI page; EngineeringToolBox illuminance table
→ a peer-reviewed ambient-light measurement study; ScienceDirect Topics (an auto-generated
aggregation page) → the 2025 *Endocrine Reviews* CAR review; IFIC, a food-industry-funded body
→ the Bath Breakfast Project RCTs; an Ovid deep link that may not resolve → the journal's own
LWW page plus an open-access PDF; the AOA's 5MB infographic PDF → the AOA's actual
computer-vision-syndrome page.

## Eyes and screens

**20-20-20 rule** (general.txt) — Endorsed by the American Academy of Ophthalmology and
American Optometric Association for reducing digital eye strain. Worth noting: the specific
20/20/20 numbers have never been validated as such, and results are mixed, which is why the
tip says "many eye doctors recommend" rather than claiming it's proven. Talens-Estarelles et
al. found personalized break reminders did improve dry-eye and eye-strain symptoms over two
weeks, with the benefit disappearing a week after the reminders stopped.
- [AOA: Computer vision syndrome](https://www.aoa.org/healthy-eyes/eye-and-vision-conditions/computer-vision-syndrome)
- Talens-Estarelles et al., *The effects of breaks on digital eye strain, dry eye and binocular
  vision*, Contact Lens & Anterior Eye, 2023.
  [ScienceDirect](https://www.sciencedirect.com/science/article/pii/S1367048422001990)
- [Optometry Times: Deconstructing the 20-20-20 rule](https://www.optometrytimes.com/view/deconstructing-20-20-20-rule-digital-eye-strain)

**Incomplete blinks, dry eye, and blink rate** (general.txt, afternoon.txt) — Portello et al.
found blink *completeness* correlated with dry-eye symptom severity more than blink rate
alone. Cognitive demand, not the screen itself, is what suppresses blink rate.
- Portello, Rosenfield & Chu, Optometry and Vision Science, 2013.
  [PubMed](https://pubmed.ncbi.nlm.nih.gov/23538437/)
- *Cognitive demand, digital screens and blink rate*, Computers in Human Behavior, 2015.
  [ScienceDirect](https://www.sciencedirect.com/science/article/abs/pii/S0747563215003829)

**Screen brightness, glare, and workstation setup** (general.txt) — Standard
occupational-health guidance rather than a single study: matching screen brightness to ambient
light avoids constant re-adaptation, and screen distance/eye level/chair/feet placement follow
the usual ergonomic recommendations.
- [UBC Visual Ergonomics: Solutions for Lighting & Eye Health](https://hr.ubc.ca/wellbeing-benefits/files/Visual-ergonomics-resources.pdf)
- [OSHA eTools: Computer Workstations](https://www.osha.gov/etools/computer-workstations/components/monitors)
- [Cornell University Ergonomics Web](https://ergo.human.cornell.edu/DEA6510/dea6512k/ergo12tips.html)

**Neutral wrist posture and carpal tunnel pressure** (general.txt) — Non-neutral wrist
postures (extension, ulnar deviation) measurably raise carpal tunnel pressure.
- Rempel et al., *Effect of Wrist Posture on Carpal Tunnel Pressure while Typing*.
  [PubMed](https://pubmed.ncbi.nlm.nih.gov/18383144/)

## Breathing, stress, and tension

**Slow breathing and vagal tone/HRV** (general.txt, afternoon.txt, evening.txt) — Slow-paced
breathing (≤6 breaths/min) reliably increases heart-rate variability and parasympathetic
activity, and is linked to reduced cortisol and anxiety. The longer-exhale detail comes from
Zaccaro et al.'s review of the psychophysiological correlates of slow breathing.
- Slow breathing and HRV, systematic review & meta-analysis, Neuroscience & Biobehavioral
  Reviews, 2022. [ScienceDirect](https://www.sciencedirect.com/science/article/abs/pii/S0149763422002007)
- Zaccaro et al., *How Breath-Control Can Change Your Life*, Frontiers in Human Neuroscience,
  2018. [PMC](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC6137615/)

**Cold water on the face / dive reflex** (morning.txt) — Facial cold-water immersion triggers
the trigeminal nerve, activating a vagally-mediated drop in heart rate: a real, measurable
autonomic response used clinically to downshift acute arousal.
- [StatPearls: Physiology, Diving Reflex](https://www.ncbi.nlm.nih.gov/books/NBK538245/)
- [PMC: Resting heart rate affects response to cold-water face immersion](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC10295257/)

**Cold showers and alertness** (morning.txt) — Cold exposure raises norepinephrine, driving
alertness. Framed cautiously: Buijze et al.'s large RCT found a 29% reduction in self-reported
sick leave but no difference in illness days, and the authors flag self-report bias.
- Buijze et al., *The Effect of Cold Showering on Health and Work*, PLoS ONE, 2016.
  [PubMed](https://pubmed.ncbi.nlm.nih.gov/27631616/)
- *Cardiovascular and mood responses to cold water immersion*, 2023.
  [ScienceDirect](https://www.sciencedirect.com/science/article/abs/pii/S0306456523002681)

**Stress and tension in the jaw/neck/shoulders** (general.txt) — EMG studies show sustained
low-level contraction in the trapezius and related muscles under psychological stress,
independent of physical workload. Awake bruxism (daytime jaw clenching) tracks with anxiety
scores, which is what the "unclench your jaw" tip rests on.
- Lundberg et al., *Psychophysiological stress responses, muscle tension, and neck and shoulder
  pain among supermarket cashiers*, 1999. [PubMed](https://pubmed.ncbi.nlm.nih.gov/10431284/)
- *Awake bruxism behaviors frequency in healthy young adults with different psychological
  scores*, 2024. [PubMed](https://pubmed.ncbi.nlm.nih.gov/38850025/)

**Music listened to deliberately for relaxation** (afternoon.txt) — de Witte et al.'s two
meta-analyses (104 RCTs, 9,617 participants) found music interventions significantly reduced
both physiological (d = .380) and psychological (d = .545) stress outcomes. The "deliberately,
not as background" qualifier is Linnemann et al.'s daily-life finding: measuring salivary
cortisol in everyday settings, the stress reduction appeared specifically when *relaxation* was
the stated reason for listening, not for music listening generally. The tip is worded to carry
that distinction rather than the broader "music reduces stress" claim.
- de Witte et al., *Effects of music interventions on stress-related outcomes*, Health
  Psychology Review, 2020.
  [Taylor & Francis](https://www.tandfonline.com/doi/full/10.1080/17437199.2019.1627897)
- Linnemann et al., *Music listening as a means of stress reduction in daily life*,
  Psychoneuroendocrinology, 2015.
  [ScienceDirect](https://www.sciencedirect.com/science/article/abs/pii/S0306453015002127)

**Cyclic sighing** (general.txt) — Balban et al.'s randomised controlled study compared three
5-minute daily breathwork practices against an equal period of mindfulness meditation over a
month. Exhale-focused cyclic sighing (a full inhale, a second short inhale on top, then a long
exhale) produced the greatest improvement in mood and the largest drop in respiratory rate,
beating the meditation arm. Paired with Zaccaro et al., already cited above for the
longer-exhale mechanism, which is what the technique is exploiting.
- Balban et al., *Brief structured respiration practices enhance mood and reduce physiological
  arousal*, Cell Reports Medicine, 2023. [PubMed](https://pubmed.ncbi.nlm.nih.gov/36630953/)
- Zaccaro et al., *How Breath-Control Can Change Your Life*, Frontiers in Human Neuroscience,
  2018. [PMC](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC6137615/)

## Sitting, movement, and posture

**Sedentary time as an independent health risk** (general.txt) — Biswas et al.'s meta-analysis
(47 studies) found sedentary time associated with higher all-cause mortality and
cardiovascular/diabetes risk *independent of* physical activity level.
- Biswas et al., Annals of Internal Medicine, 2015.
  [PubMed](https://pubmed.ncbi.nlm.nih.gov/25599350/) ·
  [Journal full text](https://www.acpjournals.org/doi/10.7326/M14-1651)

**Breaking up sitting with brief standing or walking** (general.txt, afternoon.txt) — Buffey et
al.'s meta-analysis supplies the specific numbers behind the "two minutes every half hour" tip:
standing breaks reduced postprandial glucose ~9.5% versus prolonged sitting, light walking
~17%, using 2-5 minute breaks every 20-30 minutes.
- Buffey et al., Sports Medicine, 2022. [PubMed](https://pubmed.ncbi.nlm.nih.gov/35147898/)

**Daily steps and longevity** (morning.txt) — Risk of premature death fell with step count and
plateaued around 6,000-8,000 steps/day for adults over 60 and 8,000-10,000 for those under, in
a 15-cohort meta-analysis (47,471 adults).
- Paluch et al., *Daily steps and all-cause mortality*, Lancet Public Health, 2022.
  [The Lancet](https://www.thelancet.com/journals/lanpub/article/PIIS2468-2667(21)00302-9/fulltext)

**Muscle-strengthening twice a week** (morning.txt) — The WHO's 2020 guideline makes
muscle-strengthening activity on 2+ days a week a *strong* recommendation (moderate-certainty
evidence), on top of the aerobic target. Momma et al.'s meta-analysis of 16 cohort studies
supplies the dose: a J-shaped curve with maximum risk reduction (~10-20% for all-cause
mortality, CVD, and total cancer) at roughly 30-60 minutes a week, with no clear additional
benefit beyond an hour. Both numbers are in the tip because the guideline alone reads as a much
larger commitment than the evidence actually asks for.
- Bull et al., *WHO 2020 guidelines on physical activity and sedentary behaviour*, British
  Journal of Sports Medicine, 2020.
  [PMC](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC7719906/)
- Momma et al., *Muscle-strengthening activities are associated with lower risk and mortality in
  major non-communicable diseases*, British Journal of Sports Medicine, 2022.
  [PubMed](https://pubmed.ncbi.nlm.nih.gov/35228201/)

**Short vigorous bursts, including stairs** (afternoon.txt) — Stamatakis et al. found that
3-4 daily bouts of 1-2 minutes of vigorous incidental activity, in people who do no leisure
exercise at all, were associated with 38-40% lower all-cause and cancer mortality and 48-49%
lower CVD mortality. Jenkins et al. supply the stair-specific mechanism: three 20-second bouts
of vigorous stair climbing, 3 days a week for 6 weeks, measurably raised peak oxygen uptake in
sedentary adults (the authors note the absolute increase was modest, which is why the tip
claims "real work" rather than a training programme).
- Stamatakis et al., *Association of wearable device-measured vigorous intermittent lifestyle
  physical activity with mortality*, Nature Medicine, 2022.
  [PubMed](https://pubmed.ncbi.nlm.nih.gov/36482104/)
- Jenkins et al., *Do stair climbing exercise "snacks" improve cardiorespiratory fitness?*,
  Applied Physiology, Nutrition, and Metabolism, 2019.
  [Canadian Science Publishing](https://cdnsciencepub.com/doi/10.1139/apnm-2018-0675)

**Sustained poor posture, discomfort, and changing position** (general.txt) — Office-worker
studies find perceived discomfort in the neck, shoulders, and low back rising significantly
within an hour of sitting, which is what the "your best posture is your next one" framing rests
on: varying position, rather than achieving one correct one.
- *Perceived musculoskeletal discomfort during prolonged sitting*, Applied Ergonomics, 2020.
  [ScienceDirect](https://www.sciencedirect.com/science/article/abs/pii/S0003687020301770)

## Attention, focus, and cognitive breaks

**Task-switching cost** (general.txt) — Switching between tasks reliably costs measurable time,
increasing with task complexity. Also backs the "break at a natural pause" tip: resuming from a
clean stopping point costs less than resuming mid-thought.
- Rubinstein, Meyer & Evans, J. Experimental Psychology: Human Perception and Performance, 2001.
  [PubMed](https://pubmed.ncbi.nlm.nih.gov/11518143/)

**Notifications and attention residue** (general.txt) — Interrupted work is done faster but with
more stress, and refocusing takes meaningfully longer than the interruption itself. The tip
avoids quoting an exact minute figure, since estimates vary widely across studies.
- Mark, Gudith & Klocke, *The Cost of Interrupted Work: More Speed and Stress*, CHI 2008.
  [ACM](https://dl.acm.org/doi/10.1145/1357054.1357072)

**Visual clutter competes for attention** (general.txt) — Multiple objects in the visual field
mutually suppress each other's neural representation in visual cortex: a real capacity limit,
not just a feeling of being overwhelmed.
- McMains & Kastner, J. Neuroscience, 2011. [PubMed](https://pubmed.ncbi.nlm.nih.gov/21228167/)
- *Neural evidence for distracter suppression during visual search in real-world scenes*,
  J. Neuroscience, 2012. [PubMed](https://pubmed.ncbi.nlm.nih.gov/22915122/)

**Brief mental breaks restore vigilance** (general.txt, afternoon.txt) — Ariga & Lleras'
explanation is the mechanism behind the "your brain stops tuning the task out" tip: the
vigilance decrement comes from *goal habituation*, and briefly deactivating and reactivating
the goal prevents it. That is why a break helps even when you aren't tired.
- Ariga & Lleras, *Brief and rare mental "breaks" keep you focused*, Cognition, 2011.
  [ScienceDirect](https://www.sciencedirect.com/science/article/abs/pii/S0010027710002994) ·
  [ScienceDaily summary](https://www.sciencedaily.com/releases/2011/02/110208131529.htm)

**Nature views and mental fatigue recovery** (general.txt, afternoon.txt) — Attention
Restoration Theory: directed attention is a depletable resource, and "soft fascination" lets it
recover. Ohly et al.'s systematic review is honest that support for the theory is mixed and
measurement is inconsistent, which is why the tip stays hedged.
- Ohly et al., *Attention Restoration Theory: A systematic review*, J. Toxicology and
  Environmental Health, Part B, 2016.
  [Taylor & Francis](https://www.tandfonline.com/doi/full/10.1080/10937404.2016.1196155) ·
  [ECEHH, University of Exeter summary](https://www.ecehh.org/research/attention-restoration-theory-a-systematic-review/)

**Indoor CO₂ and cognition** (general.txt) — Controlled-chamber study found decision-making
performance measurably worse at 1,000 ppm CO₂ and much worse at 2,500 ppm, versus a 600 ppm
baseline, independent of ventilation as a confound.
- Satish et al., *Is CO2 an Indoor Pollutant?*, Environmental Health Perspectives, 2012.
  [EHP](https://ehp.niehs.nih.gov/doi/full/10.1289/ehp.1104789) ·
  [PMC full text](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3548274/)

**Media multitasking and stress** (general.txt) — A German probability sample (1,557
respondents, ages 14-85) found communication load and internet multitasking positively
associated with perceived stress and indirectly with burnout, depression, and anxiety.
- Reinecke et al., *Digital Stress over the Life Span*, Media Psychology, 2017.
  [Taylor & Francis](https://www.tandfonline.com/doi/abs/10.1080/15213269.2015.1121832)

**Walking and creativity** (general.txt) — Walking (indoors or outdoors) increased creative
output by ~60% on average versus sitting in four controlled Stanford experiments, with a
residual boost after sitting back down.
- Oppezzo & Schwartz, *Give Your Ideas Some Legs*, J. Experimental Psychology: Learning,
  Memory, and Cognition, 2014.
  [APA PDF](https://www.apa.org/pubs/journals/releases/xlm-a0036577.pdf) ·
  [Stanford News](https://news.stanford.edu/stories/2014/04/walking-vs-sitting-042414)

**Writing a task down frees the mind holding it** (general.txt, morning.txt, evening.txt) —
Unfinished goals produce intrusive thoughts and impair unrelated tasks (the Zeigarnik effect),
and *making a specific plan* eliminates that interference even though the goal is still
unfinished. This is the mechanism behind both the "one top priority" and "jot down tomorrow's
task" tips.
- Masicampo & Baumeister, *Consider It Done!*, J. Personality and Social Psychology, 2011.
  [Author PDF](https://users.wfu.edu/masicaej/MasicampoBaumeister2011JPSP.pdf)

**Reading on paper versus on screen** (general.txt) — Two independent meta-analyses agree on a
small but consistent screen-inferiority effect for comprehension. Delgado et al. (54 studies)
found g = -0.21, present for expository text but not narrative, and significant specifically in
studies where participants read under time pressure, which is why the tip mentions being
rushed. Clinton (33 randomised studies) found ES = -0.25 and additionally that readers judge
their own comprehension more accurately on paper, i.e. screen readers tend to overestimate how
much they took in.
- Delgado et al., *Don't throw away your printed books: a meta-analysis on the effects of
  reading media on reading comprehension*, Educational Research Review, 2018.
  [ScienceDirect](https://www.sciencedirect.com/science/article/pii/S1747938X18300101)
- Clinton, *Reading from paper compared to screens: a systematic review and meta-analysis*,
  Journal of Research in Reading, 2019.
  [Wiley](https://onlinelibrary.wiley.com/doi/10.1111/1467-9817.12269)

**Blue-light filtering glasses and eye strain** (general.txt) — A deliberate myth-correction,
and note it does *not* contradict the evening blue-light tips: those are about melatonin and
sleep, this is about eye strain. The Cochrane review of 17 randomised trials found blue-light
filtering lenses probably make no difference to short-term eyestrain from computer work, and
rated the underlying evidence low to very low certainty. The American Academy of Ophthalmology
independently declines to recommend them, attributing screen discomfort to reduced blink rate
and dryness instead, which is the mechanism the existing blink and 20-20-20 tips already cover.
- Singh et al., *Blue-light filtering spectacle lenses for visual performance, sleep, and
  macular health in adults*, Cochrane Database of Systematic Reviews, 2023.
  [PubMed](https://pubmed.ncbi.nlm.nih.gov/37593770/)
- [American Academy of Ophthalmology: Should You Be Worried About Blue Light?](https://www.aao.org/eye-health/tips-prevention/should-you-be-worried-about-blue-light)

**Background speech versus steady noise** (general.txt) — The irrelevant speech effect: it is
the *changing-state* and intelligible character of speech that disrupts verbal working memory,
not loudness, which is why overheard conversation costs more than continuous noise at the same
level and why performance losses appear at levels as low as 35 dB(A). Hongisto's model relates
speech intelligibility (STI) to measured performance loss; the Scientific Reports study is a
recent independent replication of the serial-recall disruption in both children and adults.
- Hongisto, *A model predicting the effect of speech of varying intelligibility on work
  performance*, Indoor Air, 2005. [PubMed](https://pubmed.ncbi.nlm.nih.gov/16268835/)
- *Impact of irrelevant speech and non-speech sounds on serial recall of verbal and spatial
  items in children and adults*, Scientific Reports, 2025.
  [Scientific Reports](https://www.nature.com/articles/s41598-025-85855-w)

**Indoor temperature and office task performance** (general.txt) — Seppänen, Fisk and Lei
reanalysed studies using objective performance measures (text processing, simple calculations,
call-centre throughput) and derived a curve peaking near 21.6 °C, with measurable decline in
both directions; the relationship subsequently informed ASHRAE guidance. A later meta-analysis
of 35 studies covers the same question independently. The tip rounds to "21 to 22C" rather than
quoting 21.6 °C, since a decimal implies more precision than a person can act on.
- Seppänen, Fisk & Lei, *Effect of temperature on task performance in office environment*,
  Lawrence Berkeley National Laboratory, 2006.
  [eScholarship](https://escholarship.org/uc/item/45g4n3rv)
- *Meta-analysis of 35 studies examining the effect of indoor temperature on office work
  performance*, Building and Environment, 2021.
  [ScienceDirect](https://www.sciencedirect.com/science/article/pii/S036013232100439X)

## Circadian rhythm and mornings

**Morning/outdoor light and circadian entrainment** (morning.txt, general.txt) — Light is the
primary entrainment cue for the circadian clock, and even brief morning bright-light exposure
produces a meaningful phase advance. Outdoor daylight is roughly an order of magnitude brighter
than typical indoor lighting even under overcast skies (~1,000-2,000+ lux outdoors versus
~300-500 lux at eye level indoors), which is the basis for the "even a grey day" tips.
- [PMC: Circadian phase advance from morning light](https://pmc.ncbi.nlm.nih.gov/articles/PMC7029701/)
- *Ambient light level varies with different locations and environmental conditions*, PMC, 2021.
  [PMC](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC8263252/)

**Natural light and faster melatonin suppression** (morning.txt) — Higher-illuminance,
blue-white light produces measurably greater melatonin suppression, part of why outdoor light
helps you feel awake sooner than the same time spent indoors.
- [PMC: Effects of day-time exposure to light intensity on melatonin suppression](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC4491270/)

**Cortisol awakening response** (morning.txt) — Cortisol rises by 50% or more in the first
30-45 minutes after waking, as a normal functional part of preparing for the day rather than a
stress signal in itself.
- *Cortisol Awakening Response: Regulation and Functional Significance*, Endocrine Reviews, 2025.
  [Oxford Academic](https://academic.oup.com/edrv/article/46/1/43/7739741)
- *The circadian system modulates the cortisol awakening response in humans*, 2022.
  [PMC](https://pmc.ncbi.nlm.nih.gov/articles/PMC9669756/)

**Wake time as the stronger circadian anchor, and sleep regularity** (morning.txt, evening.txt,
sleep_late.txt) — Morning light exposure follows a fixed wake time, making consistent wake time
a stronger anchor than bedtime. A large UK cohort found regularity of sleep timing a stronger
predictor of all-cause mortality than sleep duration.
- [Sleep Regularity Index cohort study, SLEEP, 2024](https://academic.oup.com/sleep/article/47/1/zsad253/7280269)
- *Sleep regularity is a stronger predictor of mortality risk than sleep duration*, SLEEP, 2024.
  [PMC](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC10782501/)

**Snoozing** (morning.txt) — Contrary to the popular claim, Sundelin et al. found 30 minutes of
snoozing improved or did not affect cognitive performance directly on rising, cost about 6
minutes of sleep, and prevented awakenings out of slow-wave sleep, with no clear effect on
cortisol, mood, or sleep architecture. The tip was rewritten to match (see "Corrections").
- Sundelin et al., *Is snoozing losing?*, J. Sleep Research, 2024.
  [PubMed](https://pubmed.ncbi.nlm.nih.gov/37849039/) ·
  [Wiley full text](https://onlinelibrary.wiley.com/doi/10.1111/jsr.14054)

**Breakfast-skipping and metabolism** (morning.txt) — The Bath Breakfast Project's randomized
controlled trials found resting metabolic rate stable within 11 kcal/day between daily-breakfast
and extended-fasting groups, contradicting the "kick-starts your metabolism" claim. Physical
activity thermogenesis *did* differ, which is a separate matter from metabolic rate.
- Betts et al., Am. J. Clinical Nutrition, 2014 (lean adults).
  [PMC](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC4095658/)
- Chowdhury et al., Am. J. Clinical Nutrition, 2016 (adults with obesity).
  [PubMed](https://pubmed.ncbi.nlm.nih.gov/26864365/)

**Protein at breakfast and glucose stability** (morning.txt) — A high-protein breakfast
suppressed postprandial glucose after breakfast and at subsequent meals in a crossover CGM
study (a "second-meal effect").
- Xiao et al., *Effect of a High Protein Diet at Breakfast on Postprandial Glucose Level at
  Dinner Time*, Nutrients, 2023. [PMC](https://pmc.ncbi.nlm.nih.gov/articles/PMC9824806)

**Morning exercise, alertness, and mood** (morning.txt) — A 103-study, 4,671-participant
meta-analysis found general mood, anxiety, and depressive symptoms all improved from
pre- to post-exercise after a *single* bout, with small-to-moderate effect sizes and
substantial heterogeneity (hence the tip's "intensity is optional" framing rather than a
promise).
- Weinstein et al., *Affective Responses to Acute Exercise*, Psychosomatic Medicine, 2024.
  [LWW](https://journals.lww.com/bsam/fulltext/2024/07000/affective_responses_to_acute_exercise__a.2.aspx) ·
  [Open-access PDF](https://research.tilburguniversity.edu/files/118328904/Affective_Responses_to_Acute_Exercise.pdf)

**Sleep inertia** (morning.txt) — The grogginess on waking is a named, well-characterised state
with a known time course, which is the point of the tip: it is normal and it passes. Tassi &
Muzet's review puts typical dissipation at 15-30 minutes; Hilditch & McHill's more recent
review agrees on that as the usual range while noting impairment can be detectable for longer
on some measures. The tip gives the 15-30 minute figure both reviews share, and deliberately
does not promise total clearance by then.
- Hilditch & McHill, *Sleep inertia: current insights*, Nature and Science of Sleep, 2019.
  [PubMed](https://pubmed.ncbi.nlm.nih.gov/31692489/)
- Tassi & Muzet, *Sleep inertia*, Sleep Medicine Reviews, 2000.
  [PubMed](https://pubmed.ncbi.nlm.nih.gov/12531174/)

**Social jetlag** (morning.txt) — Wittmann, Roenneberg and colleagues coined the term for the
discrepancy between sleep timing on work days and free days, measured as the difference in
mid-sleep point, and argued it functions as a chronic, self-inflicted form of jetlag.
Roenneberg et al.'s follow-up found social jetlag associated with higher BMI. This is
deliberately a different claim from the existing "consistent wake time" tips: those are about
holding one time, this names what the weekend shift actually is and why the body treats it as
travel.
- Wittmann et al., *Social jetlag: misalignment of biological and social time*, Chronobiology
  International, 2006. [PubMed](https://pubmed.ncbi.nlm.nih.gov/16687322/)
- Roenneberg et al., *Social jetlag and obesity*, Current Biology, 2012.
  [PubMed](https://pubmed.ncbi.nlm.nih.gov/22578422/)

## Afternoons

**The early-afternoon dip is partly circadian** (afternoon.txt) — Monk's work shows the dip
occurs even without lunch and tracks a secondary (~12-hour harmonic) trough in the circadian
alertness rhythm, not just digestion.
- Monk, *The Post-Lunch Dip in Performance*, Clinics in Sports Medicine, 2005.
  [PubMed](https://pubmed.ncbi.nlm.nih.gov/15892914/)
- Monk et al., *Circadian determinants of the postlunch dip in performance*, Chronobiology
  International, 1996. [PubMed](https://pubmed.ncbi.nlm.nih.gov/8877121/)

**Meal size and the afternoon dip** (afternoon.txt) — Deliberately hedged. Orr et al. found no
significant sleep-latency difference between meal compositions, so the tip claims only that a
big heavy lunch *deepens* a dip that exists anyway; Monk notes the dip is exacerbated by meal
size and carbohydrate content, and a 2025 scoping review finds subjective sleepiness rises
after high-carbohydrate or high-fat meals but not balanced or protein-rich ones. The earlier
"reactive hypoglycemia" framing, cited to Wikipedia, has been dropped.
- Orr et al., *Meal composition and its effect on postprandial sleepiness*, Physiology &
  Behavior, 1997. [PubMed](https://pubmed.ncbi.nlm.nih.gov/9284488/)
- *The Influence of Food Intake and Blood Glucose on Postprandial Sleepiness and Work
  Productivity: A Scoping Review*, Nutrients, 2025.
  [PMC](https://pmc.ncbi.nlm.nih.gov/articles/PMC12566848/)

**Bright light and afternoon alertness** (afternoon.txt) — Light has a direct alerting effect
independent of vision, used experimentally to offset the post-lunch dip.
- *Effects of light intervention on alertness and mental performance during the post-lunch dip*,
  2018. [PubMed](https://pubmed.ncbi.nlm.nih.gov/30369519/)

**Caffeine half-life and afternoon cups** (afternoon.txt, evening.txt) — Caffeine's mean
half-life is roughly 4-5 hours in healthy adults, with wide individual variation (~1.5-9.5h,
largely CYP1A2-driven). 400mg taken 6 hours before bed still significantly disrupted sleep in a
controlled trial.
- Drake et al., *Caffeine Effects on Sleep Taken 0, 3, or 6 Hours before Going to Bed*,
  J. Clinical Sleep Medicine, 2013. [JCSM](https://jcsm.aasm.org/doi/10.5664/jcsm.3170)
- *Pharmacology of caffeine and its effects on the human body*, 2024.
  [ScienceDirect](https://www.sciencedirect.com/science/article/pii/S2772417424000104)

**Nap length and timing** (afternoon.txt) — In NASA's cockpit-rest study, pilots given a
40-minute rest opportunity fell asleep in ~5.6 minutes and slept ~25.8 minutes, with improved
physiological alertness and performance through descent and landing versus a no-rest group.
Keeping a nap short stays out of slow-wave sleep and avoids the worst sleep inertia; naps later
in the day reduce sleep drive and make falling asleep at night harder.
- Rosekind et al., *Crew Factors in Flight Operations 9: Effects of Planned Cockpit Rest*,
  NASA Technical Memorandum 108839. [NTRS](https://ntrs.nasa.gov/citations/19950006379)
- Rosekind et al., *Alertness management: strategic naps in operational settings*, J. Sleep
  Research, 1995. [Wiley](https://onlinelibrary.wiley.com/doi/abs/10.1111/j.1365-2869.1995.tb00229.x)
- [Sleep Foundation: Does Napping Impact Sleep at Night?](https://www.sleepfoundation.org/how-sleep-works/does-napping-impact-sleep-at-night)

**The caffeine nap** (afternoon.txt) — Caffeine takes roughly 20-30 minutes to reach effect, so
drinking it immediately before a short nap means it arrives as you wake, and the two act
together rather than competing. Reyner & Horne's driving-simulator study is the classic
demonstration: the combination cut sleepiness-related incidents to 9% of placebo, versus 34%
for caffeine alone, and worked even when participants only dozed rather than properly slept.
Hayashi et al. independently compared post-nap countermeasures. Note this coexists with the
existing "caffeine has a ~5 hour half-life" and "no naps after 3pm" tips rather than
contradicting them: it is a same-afternoon tactic, and both constraints still apply.
- Reyner & Horne, *Suppression of sleepiness in drivers: combination of caffeine with a short
  nap*, Psychophysiology, 1997. [PubMed](https://pubmed.ncbi.nlm.nih.gov/9401427/)
- Hayashi et al., *The alerting effects of caffeine, bright light and face washing after a
  short daytime nap*, Clinical Neurophysiology, 2003.
  [ScienceDirect](https://www.sciencedirect.com/science/article/abs/pii/S1388245703002554)

**The synchrony effect, and its reversal for insight** (afternoon.txt) — A genuinely
counterintuitive finding worth a tip. Wieth & Zacks had participants solve insight and analytic
problems at their optimal and non-optimal times of day. Analytic performance showed no
consistent time-of-day effect, but *insight* problem solving was consistently better at the
non-optimal time, the proposed mechanism being that reduced inhibitory control when tired
widens the associative search. A 2025 systematic review covers the broader synchrony
literature. The tip is careful to say "open-ended problems", not "all creative work".
- Wieth & Zacks, *Time of day effects on problem solving: when the non-optimal is optimal*,
  Thinking & Reasoning, 2011.
  [Taylor & Francis](https://www.tandfonline.com/doi/abs/10.1080/13546783.2011.625663)
- *Chronotype and synchrony effects in human cognitive performance: a systematic review*,
  Chronobiology International, 2025.
  [Taylor & Francis](https://www.tandfonline.com/doi/full/10.1080/07420528.2025.2490495)

## Evenings and sleep

**Blue-enriched light and melatonin suppression** (evening.txt, sleep_early.txt) — Reading on a
light-emitting device before bed (vs. a printed book) suppressed melatonin, delayed the
circadian clock, and reduced next-morning alertness in a controlled two-week crossover study.
- Chang et al., PNAS, 2015. [PNAS](https://www.pnas.org/doi/10.1073/pnas.1418490112) ·
  [Penn State summary](https://www.psu.edu/news/research/story/light-emitting-e-readers-detrimentally-shift-circadian-clock)

**Even dim light suppresses melatonin** (evening.txt) — Ordinary room light before bedtime, not
just bright or blue light, measurably suppressed melatonin onset and shortened its duration.
- Gooley et al., *Exposure to Room Light before Bedtime Suppresses Melatonin Onset*, J. Clinical
  Endocrinology & Metabolism, 2011. [PubMed](https://pubmed.ncbi.nlm.nih.gov/21193540/)

**Warm bath/shower before bed** (evening.txt) — Meta-analysis of 17 studies (13 pooled
quantitatively): bathing in 40-42.5°C water 1-2 hours before bed shortened sleep-onset latency
by ~10 minutes on average. Mechanism: warming the skin increases peripheral blood flow, which
speeds heat loss and helps core temperature drop, and that drop is itself a sleep-onset signal.
- Haghayegh et al., Sleep Medicine Reviews, 2019.
  [PubMed](https://pubmed.ncbi.nlm.nih.gov/31102877/) ·
  [UT Austin summary](https://news.utexas.edu/2019/07/19/take-a-warm-bath-1-2-hours-before-bedtime-to-get-better-sleep-researchers-find/)

**Bedroom temperature** (evening.txt) — Heat and cold exposure both increase wakefulness and
reduce REM and slow-wave sleep; the commonly recommended 18-20°C (65-68°F) range is consistent
with the natural pre-sleep drop in core body temperature.
- Okamoto-Mizuno & Mizuno, *Effects of thermal environment on sleep and circadian rhythm*,
  J. Physiological Anthropology, 2012. [PubMed](https://pubmed.ncbi.nlm.nih.gov/22738673/)
- [Sleep Foundation: The Best Temperature for Sleep](https://www.sleepfoundation.org/bedroom-environment/best-temperature-for-sleep)

**Alcohol and sleep fragmentation** (evening.txt) — Alcohol shortens sleep-onset time via
sedation but reduces total REM sleep and increases wakefulness and fragmentation in the second
half of the night, consistently across reviews and meta-analyses.
- *The effect of alcohol on subsequent sleep in healthy adults*, meta-analysis, Sleep Medicine
  Reviews, 2024. [ScienceDirect](https://www.sciencedirect.com/science/article/pii/S1087079224001345)
- Ebrahim et al., *Alcohol and Sleep I: Effects on Normal Sleep*, Alcoholism: Clinical and
  Experimental Research, 2013. [Wiley](https://onlinelibrary.wiley.com/doi/abs/10.1111/acer.12006)

**Eating close to bedtime** (evening.txt) — Eating within 3 hours of bed was associated with
more nocturnal awakenings in a 793-participant study, independent of ethnicity and BMI.
Interventional evidence is more mixed than observational, so the tip stays hedged.
- Chung et al., *Does the Proximity of Meals to Bedtime Influence the Sleep of Young Adults?*,
  Nutrients, 2020. [PMC](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC7215804/)
- [AJMC: Eating or drinking up to one hour before bedtime may impair sleep quality](https://www.ajmc.com/view/eating-or-drinking-up-to-one-hour-before-bedtime-may-impair-sleep-quality)

**Short walk after a meal** (evening.txt) — A 10-minute walk immediately after a glucose load
lowered peak glucose (164 vs 182 mg/dL) and 2-hour AUC versus resting, and did so about as well
as a 30-minute walk, in a randomized crossover trial.
- Hashimoto et al., *Positive impact of a 10-min walk immediately after glucose intake on
  postprandial glucose levels*, Scientific Reports, 2025.
  [Nature](https://www.nature.com/articles/s41598-025-07312-y)

**Writing tomorrow's to-do list before bed** (evening.txt) — In a polysomnography study, people
who spent 5 minutes writing a specific to-do list fell asleep ~9 minutes faster than those who
wrote about completed tasks, with more specific lists correlating with faster onset still.
- Scullin et al., *The Effects of Bedtime Writing on Difficulty Falling Asleep*, J. Experimental
  Psychology: General, 2018. [PubMed](https://pubmed.ncbi.nlm.nih.gov/29058942/) ·
  [Baylor University](https://news.web.baylor.edu/news/story/2018/can-writing-your-dos-help-you-doze-baylor-study-suggests-jotting-down-tasks-can)

**Gratitude and sleep quality** (evening.txt) — Higher trait gratitude was associated with
better sleep quality, longer duration, and faster onset, mediated by fewer negative pre-sleep
thoughts; a two-week randomized gratitude intervention improved sleep quality alongside
well-being and blood pressure.
- Wood, Joseph, Lloyd & Atkins, J. Psychosomatic Research, 2009.
  [PubMed](https://pubmed.ncbi.nlm.nih.gov/19073292/)
- Jackowska et al., *The impact of a brief gratitude intervention on subjective well-being,
  biology and sleep*, 2016. [PubMed](https://pubmed.ncbi.nlm.nih.gov/25736389/)

**Keeping the bed for sleep only, and getting up when sleep won't come** (evening.txt) —
Stimulus control trains the bed as a specific cue for sleep by removing incompatible
activities, including lying awake. The AASM's 2021 clinical practice guideline gives a
conditional recommendation for stimulus control as a single-component therapy, and a strong
recommendation for multicomponent CBT-I that includes it.
- *Behavioral and psychological treatments for chronic insomnia disorder in adults: an AASM
  clinical practice guideline*, J. Clinical Sleep Medicine, 2021.
  [JCSM](https://jcsm.aasm.org/doi/10.5664/jcsm.8986)
- [Stanford Health Care: Stimulus Control and CBT-I](https://stanfordhealthcare.org/medical-treatments/c/cognitive-behavioral-therapy-insomnia/procedures/stimulus-control.html)

**Evening exercise and sleep** (evening.txt) — Worth stating plainly because the two best
reviews disagree at the margin, and the tip is worded to survive both. Stutz et al.'s
meta-analysis (23 studies) found evening exercise did *not* harm sleep and mildly improved it
(slow-wave sleep +1.3pp, stage 1 sleep -0.9pp), with one exception: vigorous exercise ending
less than an hour before bed, with heart rate still elevated. Frimpong et al. drew the line
more conservatively at two hours for high-intensity exercise specifically. The tip therefore
says "an hour or two", which is the range both reviews support, rather than picking a side.
- Stutz et al., *Effects of Evening Exercise on Sleep in Healthy Participants*, Sports
  Medicine, 2019. [Springer](https://link.springer.com/article/10.1007/s40279-018-1015-0)
- Frimpong et al., *The effects of evening high-intensity exercise on sleep in healthy adults*,
  Sleep Medicine Reviews, 2021.
  [ScienceDirect](https://www.sciencedirect.com/science/article/abs/pii/S1087079221001209)

**Night mode versus screen brightness** (evening.txt) — Nagare et al. tested Apple's Night
Shift directly and concluded that "changing the spectral composition of self-luminous displays
without changing their brightness settings may be insufficient for preventing impacts on
melatonin suppression"; their own recommendation is to dim the device and cut exposure
duration. Phillips et al. independently establish why brightness dominates: 50% melatonin
suppression occurred below 30 lux, at or under ordinary indoor evening light, with a
greater-than-50-fold spread in sensitivity between individuals. Note this sits alongside, not
against, the existing "blue-enriched light suppresses melatonin more than warm light of the
same brightness" tip: wavelength matters *at a fixed brightness*, and a filter alone does not
fix a bright screen.
- Nagare, Plitnick & Figueiro, *Does the iPad Night Shift mode reduce melatonin suppression?*,
  Lighting Research & Technology, 2019.
  [PMC](https://pmc.ncbi.nlm.nih.gov/articles/PMC6561503/)
- Phillips et al., *High sensitivity and interindividual variability in the response of the
  human circadian system to evening light*, PNAS, 2019.
  [PNAS](https://www.pnas.org/doi/10.1073/pnas.1901824116)

**Short sleep and susceptibility to colds** (evening.txt) — Two experimental viral-challenge
studies, not self-report surveys: participants were quarantined and deliberately exposed to a
cold virus. Cohen et al. found those sleeping under 7 hours were 2.94x more likely to develop a
cold than those sleeping 8+ hours; Prather et al. replicated this with wrist actigraphy rather
than sleep diaries, removing the recall-bias objection, and found the same ~2.9x figure. The
tip says "in two virus-exposure studies" precisely because the study design is what makes the
claim strong.
- Prather et al., *Behaviorally Assessed Sleep and Susceptibility to the Common Cold*, Sleep,
  2015. [PubMed](https://pubmed.ncbi.nlm.nih.gov/26118561/)
- Cohen et al., *Sleep Habits and Susceptibility to the Common Cold*, Archives of Internal
  Medicine, 2009. [PubMed](https://pubmed.ncbi.nlm.nih.gov/19139325/)

**Light during sleep, not just before it** (evening.txt) — Distinct from every other evening
light tip in the catalog, which are all about the hours *before* bed and about melatonin. Mason
et al. randomised healthy adults to one night under 100 lux overhead room light versus dim
(<3 lx) light and found higher nighttime heart rate, reduced heart-rate variability, and
increased next-morning insulin resistance, after a single night. Obayashi et al.'s HEIJO-KYO
cohort measured actual bedroom light intensity in 528 older adults' homes and found light at
night associated with obesity and dyslipidemia independent of melatonin excretion. The "dimmer
than most hallways" comparison is there because 100 lux sounds like a lot and is not.
- Mason et al., *Light exposure during sleep impairs cardiometabolic function*, PNAS, 2022.
  [PubMed](https://pubmed.ncbi.nlm.nih.gov/35286195/)
- Obayashi et al., *Exposure to light at night, nocturnal urinary melatonin excretion, and
  obesity/dyslipidemia in the elderly*, Journal of Clinical Endocrinology & Metabolism, 2013.
  [PubMed](https://pubmed.ncbi.nlm.nih.gov/23118419/)

**Bedroom noise** (evening.txt) — The WHO's guidance for good-quality sleep is under 30 dB(A)
of continuous noise indoors. The reason it earns a tip is the part people underestimate: noise
produces measurable sleep fragmentation, cortical arousals and stage shifts, without
necessarily producing a remembered awakening, so "it doesn't wake me" is not evidence it is
harmless. Basner & McGuire's systematic review, commissioned for the WHO's environmental noise
guidelines, is the evidence base for the exposure-response relationship.
- [WHO Night Noise Guidelines for Europe](https://www.who.int/europe/publications/i/item/9789289041737)
- Basner & McGuire, *WHO environmental noise guidelines for the European region: a systematic
  review on environmental noise and effects on sleep*, International Journal of Environmental
  Research and Public Health, 2018.
  [PMC](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC5877064/)

## Nature, connection, and mood

**Time outdoors** (general.txt) — White et al. (n = 19,806) found no health or wellbeing
benefit for 1-119 minutes of nature contact per week, and a consistent benefit at 120+ minutes,
with the threshold holding whether the time came in one visit or several and even for people
living in low-greenspace areas. That "in one go or spread across the week" detail is in the tip
because it is the study's actual finding, and it is the part that makes the number achievable.
Twohig-Bennett & Jones' meta-analysis (143 studies) independently ties greenspace exposure to
lower diastolic blood pressure, salivary cortisol, and all-cause mortality; the authors note
heterogeneity and variable study quality, so the tip says "tracks with" rather than "causes".
- White et al., *Spending at least 120 minutes a week in nature is associated with good health
  and wellbeing*, Scientific Reports, 2019.
  [Scientific Reports](https://www.nature.com/articles/s41598-019-44097-3)
- Twohig-Bennett & Jones, *The health benefits of the great outdoors*, Environmental Research,
  2018. [ScienceDirect](https://www.sciencedirect.com/science/article/pii/S0013935118303323)

**Social connection and mortality** (general.txt) — Holt-Lunstad et al.'s 2010 meta-analysis
(148 studies, 308,849 people, average 7.5-year follow-up) found a 50% greater likelihood of
survival for people with adequate social relationships, an effect the authors compare directly
to smoking cessation and rate above obesity and physical inactivity. Their 2015 follow-up
approaches it from the other side, finding social isolation (OR 1.29), loneliness (1.26), and
living alone (1.32) each independently raise mortality risk. The smoking comparison in the tip
is the authors' own framing, not this project's editorialising.
- Holt-Lunstad, Smith & Layton, *Social Relationships and Mortality Risk*, PLoS Medicine, 2010.
  [PLoS Medicine](https://journals.plos.org/plosmedicine/article?id=10.1371/journal.pmed.1000316)
- Holt-Lunstad et al., *Loneliness and Social Isolation as Risk Factors for Mortality*,
  Perspectives on Psychological Science, 2015.
  [PubMed](https://pubmed.ncbi.nlm.nih.gov/25910392/)

**Acts of kindness and the giver's own mood** (general.txt) — Curry et al.'s meta-analysis of
27 experimental studies (N = 4,045) found a small-to-medium effect of performing kind acts on
the actor's own wellbeing (δ = 0.28). Hui et al.'s much larger correlational meta-analysis
(K = 201, N = 198,213) found a modest overall link (r = .13), stronger for informal helping
than formal volunteering. Both effects are real and both are small, which is why the tip
promises "a small, reliable lift" rather than anything more.
- Curry et al., *Happy to help? A systematic review and meta-analysis of the effects of
  performing acts of kindness on the well-being of the actor*, Journal of Experimental Social
  Psychology, 2018.
  [Oxford Research Archive](https://ora.ox.ac.uk/objects/uuid:4701365b-5760-4ece-a390-857a5f7b3c0c)
- Hui et al., *Rewards of kindness? A meta-analysis of the link between prosociality and
  well-being*, Psychological Bulletin, 2020.
  [PubMed](https://pubmed.ncbi.nlm.nih.gov/32881540/)

## Food and fibre

**Dietary fibre intake** (general.txt) — Reynolds et al.'s series of meta-analyses for the WHO
(185 prospective studies and 58 trials, ~135 million person-years) found risk reduction across
critical outcomes greatest at 25-29 g of fibre a day, with dose-response data suggesting more
than 30 g confers further benefit. The "most people fall short" half of the tip comes from NHS
guidance, which puts UK average intake at about 20 g against a 30 g recommendation. Government
guidance is used here as the population-intake source alongside the primary meta-analysis, per
the source-quality rules above.
- Reynolds et al., *Carbohydrate quality and human health: a series of systematic reviews and
  meta-analyses*, The Lancet, 2019. [PubMed](https://pubmed.ncbi.nlm.nih.gov/30638909/)
- [NHS: How to get more fibre into your diet](https://www.nhs.uk/live-well/eat-well/digestive-health/how-to-get-more-fibre-into-your-diet/)

## Hydration

**Mild dehydration and cognition/mood** (general.txt, morning.txt, afternoon.txt) — At ~1-2%
body-mass fluid loss, well short of thirst-driven urgency, both mood and concentration
measurably worsen. The effect is present in both sexes, though the specific symptoms differ:
the companion women's study found larger mood and perceived-task-difficulty effects, the men's
study more memory degradation.
- Ganio et al., *Mild dehydration impairs cognitive performance and mood of men*, British
  Journal of Nutrition, 2011.
  [Cambridge Core](https://www.cambridge.org/core/journals/british-journal-of-nutrition/article/mild-dehydration-impairs-cognitive-performance-and-mood-of-men/3388AB36B8DF73E844C9AD19271A75BF)
- Armstrong et al., *Mild Dehydration Affects Mood in Healthy Young Women*, Journal of
  Nutrition, 2012. [J. Nutrition](https://jn.nutrition.org/article/S0022-3166(22)02889-9/fulltext)
