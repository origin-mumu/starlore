<script setup lang="ts">
import {
  ref,
  computed,
  watch,
  onMounted,
  onUnmounted,
  nextTick,
} from 'vue'
import { ChevronLeft, ChevronRight } from '@lucide/vue'
import CarouselCard from './CarouselCard.vue'
import { DELIVERABLE_CARDS, type DeliverableCardItem } from './carouselShots'

/* SHOTS was Bencho's own pictures, which are not licensed
   to travel. Point this at yours. */
type ShotItem = DeliverableCardItem

const props = withDefaults(
  defineProps<{
    shots?: ShotItem[]
    /* how far out to the sides the ring reaches, in px */
    orbit?: number
    /* how much smaller the back of the ring is, 0..100 */
    depth?: number
    corner?: number
    /* how much they drift at rest, 0..100 — 0 is a still ring,
       which is a real setting and not a broken one */
    float?: number
    /* how deep the pointer presses, 0..100 */
    sink?: number
    /* how quickly a swipe settles, 0..100 */
    settle?: number
    /* ── seconds for one whole turn of the ring ──────────────
       0 is off, and off is the default: on the bench this ring
       is a thing you push, and one that also drifts round on its
       own would be answering a gesture nobody made. It is here
       for the places the carousel is a PICTURE rather than a
       control — the Pro sheet, today — where it has to carry
       itself because nobody is going to touch it. */
    spin?: number
  }>(),
  {
    orbit: 330,
    depth: 75,
    corner: 20,
    float: 0,
    sink: 50,
    settle: 50,
    spin: 0,
  },
)

const shotsList = computed<ShotItem[]>(() =>
  props.shots && props.shots.length > 0 ? props.shots : DELIVERABLE_CARDS,
)

/* ══ Carousel ═════════════════════════════════════════════
   Cards on a turntable that drift at rest, give under the
   pointer, and can be swiped round.

   NOTHING EVER LEAVES THE FRAME, and that is the whole
   arrangement. This was a row twice — cards in a line, with
   the ones at the ends either dissolving into a mask or being
   cut by the edge of a box — and both are answers to the same
   question: what happens to a card when it runs out of block?
   A ring does not ask it. The cards go ROUND: out to one
   side, back and small, round to the other side, forward
   again. The furthest a card ever gets is the radius, which
   is a number this component chose, so there is no edge to
   treat and no box to put it in.

   IT IS THEREFORE ENDLESS. A row has a first card and a last
   one and has to decide what to do at each; a ring has
   neither, so the swipe never runs out and never rubber-bands.
   Swipe once and the front card shrinks and goes to the back
   while the next one comes forward — which is the motion he
   described, and it falls out of the geometry rather than
   being animated on top of it.

   THE ANGLE IS NOT TAKEN MODULO ANYTHING. `turn` counts up
   and down without limit and the angle is `(i - turn) * step`.
   Wrapping it to 0..360 would send a card the long way round
   the moment it crossed the seam, which is the one visible bug
   this arrangement can have — the same note the Pro sheet's
   reel carries, for the same reason.

   THREE MOTIONS, THREE ELEMENTS, ONE TRANSFORM EACH. The slot
   carries where the card is on the ring, the floater carries
   the drift, the card carries the tilt. They are nested rather
   than composed into one string because they are owned by
   three different things — the drag, a CSS animation and a
   pair of springs — and a single element cannot be written by
   three authors without one of them losing. */

const clamp = (v: number, lo: number, hi: number) => Math.min(hi, Math.max(lo, v))
const mix = (a: number, b: number, t: number) => a + (b - a) * t

/* the card is the PICTURES' own ratio, near enough — 160x226
   is 0.708 against their 2:3, so `cover` trims about six per
   cent off the height. These are photographs with nothing at
   their top or bottom edge, and the card reading as a card
   rather than as a slat is worth the last few rows. */
const CARD_W = 230
const CARD_H = 370

/* ── the frame, and it is only a frame ─────────────────────
   Sized to hold 5 cards visible across the ring */
const STAGE_W = 920
const STAGE_H = 460

const DEPTH_MAX = 150

/* ── how far back the ring leans ───────────────────────────
   A card at the back sits this much higher than one at the
   front. */
const LEAN = 26

/* how many px of drag turn the ring one position */
const PULL = 180

/* how far ahead of the release the throw looks, in ms of
   travel. It is what makes a short fast flick move a card:
   without it a swipe is judged on distance alone and a quick
   one that barely moved counts for nothing. */
const TOSS = 150
/* and how many it may skip. A flick can carry two; past that
   the ring blurs and you have lost your place on it. */
const MOST = 2

const BASE = 620

/* ── the pictures ARE the count ────────────────────────────
   It was a 3..6 knob while the cards were coloured faces,
   which is a knob you can only have when the cards carry
   nothing: with photographs, one more card than pictures has
   to be one of them a second time, and the same picture twice
   on one ring reads as a bug rather than as a setting. So the
   number of cards is `SHOTS.length` and nothing else —
   add a file, run the script, and the ring has another
   position. Five today.

   Five is also the better ring. With four, one card sits at
   180 degrees, which is directly behind the front one and out
   of sight; at 72-degree steps nothing is ever exactly
   behind anything, so every card is at least partly visible
   at rest and the ring reads as a ring rather than as three
   cards and a rumour.

   They are inlined by scripts/carousel.sh — drop new files in
   src/assets/carousel and run it. The order is the filenames
   sorted, so renaming one moves it. */
const N = computed(() => shotsList.value.length)

/* ── which one is at the front when it opens ───────────────
   BY NAME, not by index. The order is whatever the filenames
   sort to, so an index here would be a number that silently
   means a different picture the day somebody adds a file —
   and "the card it opens on" is a decision, not an accident
   of the alphabet. Falls back to the middle of the ring if
   the name is not there, which is where it used to open. */
const FRONT = 'agent'
const getOpensOn = () => {
  const i = shotsList.value.findIndex((s) => s.name === FRONT)
  return i < 0 ? Math.floor((N.value - 1) / 2) : i
}

/* ── every card sits at its own angle ──────────────────────
   Numbers that are not a pattern: no two the same, no
   symmetry to spot, and they do not alternate. A ring where
   the angles went -4, +4, -4, +4 is an arrangement somebody
   made; numbers that are merely different are a handful of
   photographs somebody put down.

   There is one per picture. It has to be — `i % ANGLE.length`
   would wrap a fifth card onto the first card's angle, and
   two of five sharing an angle is the pattern this list
   exists to avoid.

   They can be this generous again now the cards are on a
   ring. On a row an angle cost width — a turned card sweeps
   its corner sideways into its neighbour's air — and the
   spread knob's floor had to pay for it. Cards at different
   depths are allowed to overlap; that is what depth looks
   like. */
const ANGLE = [0, 0, 0, 0, 0]

/* the drift's periods */
const PERIOD = [4.7, 5.9, 6.7, 5.3, 7.1, 6.1]

type Spot = { x: number; y: number; s: number; z: number; op: number }

/* ── where a card sits, given where the ring is ────────────
   Smooth, upright coverflow without weird tilt or wobble */
const spotOf = (i: number, turnPos: number, orbitDist: number, depthVal: number): Spot => {
  const count = N.value || 1
  const raw = i - turnPos
  const rem = ((raw % count) + count) % count
  const d = rem > count / 2 ? rem - count : rem
  const absD = Math.abs(d)
  const sign = d < 0 ? -1 : 1

  // Continuous smooth coverflow distribution
  const x = sign * (Math.min(absD, 1) * 230 + Math.max(0, absD - 1) * 190)
  const s = Math.max(0.72, 1 - absD * 0.12)
  const z = Math.round(100 - absD * 25)
  // Smooth fade at the boundary so wrapping occurs invisibly
  const op = Math.max(0.3, 1 - Math.pow(Math.min(absD, 2.5) / 2.5, 2) * 0.65)

  return {
    x,
    y: 0,
    s,
    z,
    op,
  }
}

const write = (el: HTMLElement, sp: Spot, _angle: number) => {
  el.style.transform = `translate(-50%, -50%) translate(${sp.x.toFixed(2)}px, ${sp.y.toFixed(2)}px) scale(${sp.s.toFixed(4)})`
  el.style.zIndex = String(sp.z)
  el.style.opacity = String(sp.op.toFixed(3))
}

const out = (t: number) => 1 - (1 - t) ** 4

/* Read once, the way the wheel and the pill nav do. A
   preference, not a live input. */
const stillness = () =>
  typeof window !== 'undefined' &&
  !!window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

/* How long it takes, slower to faster, as a multiplier on
   whatever the component's own tuned duration is. 0 is a
   little over half again as slow, 100 is two and a half times
   as fast, 50 is exactly 1. */
const rate = (speed: number) => 1.6 - (speed / 100) * 1.2

const still = stillness()

const slots = ref<(HTMLDivElement | null)[]>([])

/* ── where the ring is, and it is a REF ──────────────────
   A continuous position in card-steps: 0 puts the first
   card at the front, 1.5 is halfway between the second and
   third. It changes every frame of a drag, which is exactly
   the value that must not be state — React renders when a
   knob moves, the ring is painted.

   It is NOT clamped and NOT wrapped. See the note at the
   top: this is the number a modulo would ruin. */
const turn = ref(getOpensOn())
let rafId = 0
let spinRafId = 0

const drag = ref<{
  x0: number
  t0: number
  last: number
  t: number
  vx: number
  moved: boolean
} | null>(null)
const held = ref(false)

const paint = () => {
  slots.value.forEach((el, i) => {
    if (el) {
      write(
        el,
        spotOf(i, turn.value, props.orbit, props.depth),
        ANGLE[i % ANGLE.length],
      )
    }
  })
}

/* ── the settle ──────────────────────────────────────────
   Quart-out from wherever the ring currently is to a whole
   position. It reads its start from the live value rather
   than from where the drag began, so a second swipe during
   one turns the ring further instead of snapping it back. */
const glide = (to: number) => {
  cancelAnimationFrame(rafId)
  const from = turn.value
  if (still || from === to) {
    turn.value = to
    paint()
    return
  }
  const ms = BASE * rate(clamp(props.settle, 0, 100))
  const t0 = performance.now()
  const tick = (now: number) => {
    const p = Math.min(1, (now - t0) / ms)
    turn.value = mix(from, to, out(p))
    paint()
    if (p < 1) {
      rafId = requestAnimationFrame(tick)
    }
  }
  rafId = requestAnimationFrame(tick)
}

const go = (d: number) => glide(Math.round(turn.value) + d)

const down = (e: PointerEvent) => {
  cancelAnimationFrame(rafId)
  drag.value = {
    x0: e.clientX,
    t0: turn.value,
    last: e.clientX,
    t: e.timeStamp,
    vx: 0,
    moved: false,
  }
  held.value = true
  try {
    ;(e.currentTarget as HTMLElement)?.setPointerCapture(e.pointerId)
  } catch {
    /* not a live pointer */
  }
}

const move = (e: PointerEvent) => {
  const g = drag.value
  if (!g) return
  const dx = e.clientX - g.x0
  if (!g.moved && Math.abs(dx) > 3) g.moved = true

  /* px per ms, smoothed against the previous reading so one
     jittery frame cannot fake a flick — and measured from
     the LAST position rather than from the grab, because the
     speed at release is the only part of a swipe that says
     how far it meant to go */
  const dt = Math.max(1, e.timeStamp - g.t)
  g.vx = (g.vx + (e.clientX - g.last) / dt) / 2
  g.last = e.clientX
  g.t = e.timeStamp

  /* ── no rubber band, because there is no end ─────────
     A row had to give at its first and last card, or it read
     as broken input. A ring has neither, so the drag is a
     plain one-to-one and keeps going as long as you do. */
  turn.value = g.t0 - dx / PULL
  paint()
}

const up = () => {
  const g = drag.value
  if (!g) return
  drag.value = null
  held.value = false
  /* where it would come to rest if it kept going, capped so
     a hard flick cannot spin the ring past where you can
     follow it */
  const carry = clamp((-g.vx * TOSS) / PULL, -MOST, MOST)
  const to = Math.round(turn.value + carry)
  if (g.moved) glide(to)
}

const key = (e: KeyboardEvent) => {
  const d = e.key === 'ArrowRight' ? 1 : e.key === 'ArrowLeft' ? -1 : 0
  if (!d) return
  e.preventDefault()
  go(d)
}

const r = computed(() => clamp(props.corner, 0, 40))

/* ── the ring turning itself ─────────────────────────────
   Its own frame loop and its own handle, deliberately not
   `raf` — that one belongs to the settle after a swipe, and
   the two sharing it would mean whichever started last
   cancelled the other.

   It advances `turn` by time rather than stepping between
   whole positions: a ring that clicks from card to card is
   reading as a slideshow, and the whole point of this one is
   that it is a continuous ring you are looking at side on.

   Held pauses it. Nothing on the Pro sheet can grab it —
   that reel is pointer-events: none — but the pause costs a
   line and means the prop is safe anywhere. */
const startSpin = () => {
  cancelAnimationFrame(spinRafId)
  if (!props.spin || still || held.value) return
  let prev = 0
  const step = (t: number) => {
    /* N cards over `spin` seconds is one whole revolution */
    if (prev) turn.value += ((t - prev) / 1000) * (N.value / props.spin)
    prev = t
    paint()
    spinRafId = requestAnimationFrame(step)
  }
  spinRafId = requestAnimationFrame(step)
}

watch(
  () => [props.orbit, props.depth],
  () => {
    paint()
  },
)

watch(
  () => [props.spin, held.value],
  () => {
    startSpin()
  },
)

onMounted(() => {
  nextTick(() => {
    paint()
    startSpin()
  })
})

const activeIndex = computed(() => {
  const count = N.value || 1
  return ((Math.round(turn.value) % count) + count) % count
})

const goToIndex = (targetIdx: number) => {
  const count = N.value || 1
  const current = ((Math.round(turn.value) % count) + count) % count
  let diff = targetIdx - current
  if (diff > count / 2) diff -= count
  if (diff < -count / 2) diff += count
  glide(Math.round(turn.value) + diff)
}

const clickCard = (i: number) => {
  if (drag.value?.moved) return
  const count = N.value || 1
  const raw = i - turn.value
  const rem = ((raw % count) + count) % count
  const d = rem > count / 2 ? rem - count : rem
  if (Math.abs(d) > 0.1) {
    glide(Math.round(turn.value + d))
  }
}

onUnmounted(() => {
  cancelAnimationFrame(rafId)
  cancelAnimationFrame(spinRafId)
})
</script>

<template>
  <div class="car-wrapper">
    <div class="car" :style="{ width: `${STAGE_W}px`, height: `${STAGE_H}px` }">
      <div
        class="car-track"
        :data-held="held"
        role="group"
        aria-label="Card carousel"
        aria-roledescription="carousel"
        tabindex="0"
        @keydown="key"
        @pointerdown="down"
        @pointermove="move"
        @pointerup="up"
        @pointercancel="up"
      >
        <div
          v-for="(shot, i) in shotsList"
          :key="shot.name"
          :ref="(el) => { if (el) slots[i] = el as HTMLDivElement }"
          class="car-slot"
          :style="{ width: `${CARD_W}px`, height: `${CARD_H}px` }"
          @click="clickCard(i)"
        >
          <div class="car-float">
            <CarouselCard
              :item="shot"
              :index="i"
              :corner="r"
              :sink="sink"
              :off="held || still"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- Minimal Controls: Prev, Dots, Next -->
    <div class="car-nav-dock">
      <button class="nav-dock-btn" aria-label="上一项" @click="go(-1)">
        <ChevronLeft :size="18" />
      </button>

      <div class="nav-dock-dots">
        <button
          v-for="(shot, idx) in shotsList"
          :key="shot.name"
          class="dock-dot"
          :class="{ 'dock-dot--active': activeIndex === idx }"
          :aria-label="`查看 ${shot.title}`"
          @click="goToIndex(idx)"
        />
      </div>

      <button class="nav-dock-btn" aria-label="下一项" @click="go(1)">
        <ChevronRight :size="18" />
      </button>
    </div>
  </div>
</template>

<style>
/* ══ Carousel ═══════════════════════════════════════════════
   Sleek, upright Coverflow Carousel with tactile navigation */
.car-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  position: relative;
  width: 100%;
}

.car {
  position: relative;
  --bg: var(--canvas, #EDF6FC);
  --font-ui: 'Inter', 'Noto Sans SC', system-ui, sans-serif;
  --ink-rgb: 26, 20, 16;
  --shadow-rgb: 10, 5, 31;
  font-family: var(--font-ui);
  user-select: none;
}

:global([data-theme="dark"]) .car {
  --bg: var(--canvas, #0A051F);
  --ink-rgb: 230, 232, 232;
  --shadow-rgb: 0, 0, 0;
}

.car-track {
  position: absolute;
  inset: 0;
  touch-action: pan-y;
  cursor: grab;
}

.car-track[data-held="true"] {
  cursor: grabbing;
}

.car-track:focus-visible {
  outline: none;
  box-shadow: 0 0 0 2px var(--bg), 0 0 0 4px rgba(var(--ink-rgb), 0.4);
}

.car-slot {
  position: absolute;
  left: 50%;
  top: 50%;
  transform-origin: center;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.car-float {
  width: 100%;
  height: 100%;
  perspective: 760px;
  animation: none; /* 去除抖动晃动 */
}

/* 底部精美小巧导航控制条 */
.car-nav-dock {
  display: inline-flex;
  align-items: center;
  gap: 16px;
  padding: 6px 14px;
  background: #FFFFFF;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 9999px;
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.06);
  z-index: 10;
  user-select: none;
}

:global([data-theme="dark"]) .car-nav-dock {
  background: #141828;
  border-color: rgba(255, 255, 255, 0.12);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.5);
}

.nav-dock-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #E2E8F0;
  background: #F8FAFC;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s ease;
}

:global([data-theme="dark"]) .nav-dock-btn {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.1);
  color: #CBD5E1;
}

.nav-dock-btn:hover {
  background: #FFFFFF;
  color: #DE4331;
  border-color: #DE4331;
  transform: scale(1.08);
}

:global([data-theme="dark"]) .nav-dock-btn:hover {
  background: rgba(255, 255, 255, 0.14);
  color: #FFFFFF;
}

.nav-dock-dots {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dock-dot {
  width: 8px;
  height: 8px;
  border-radius: 9999px;
  background: #CBD5E1;
  border: none;
  padding: 0;
  cursor: pointer;
  transition: all 0.25s ease;
}

:global([data-theme="dark"]) .dock-dot {
  background: rgba(255, 255, 255, 0.2);
}

.dock-dot--active {
  width: 24px;
  background: #DE4331;
  box-shadow: 0 0 10px rgba(222, 67, 49, 0.4);
}

@media (max-width: 1040px) {
  .car {
    transform: scale(0.85);
    transform-origin: center center;
  }
}

@media (max-width: 820px) {
  .car {
    transform: scale(0.72);
    transform-origin: center center;
  }
}

@media (max-width: 580px) {
  .car {
    transform: scale(0.52);
    transform-origin: center center;
  }
}
</style>
