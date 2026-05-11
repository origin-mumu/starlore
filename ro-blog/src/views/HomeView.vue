<script lang="ts" setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { getBlogStatsService } from '@/api/article'
import Wave from '@/components/wave.vue'
import SideBar from '@/components/sideBar.vue'
import { Article } from '@/type/Article'
import DummyCard from '@/components/dummyCard.vue'
import shenshouImg from '@/assets/shenshou.png'
import tiakongzhanImg from '@/assets/tiakongzhan.jpg'
import yuelingImg from '@/assets/yueling.png'

type HeroSlide = {
  id: number
  title: string
  image: string
}

const data = ref()
const articles = ref<Article[]>()

const heroSlides = ref<HeroSlide[]>([
  { id: 1, title: 'SHEN SHOU', image: shenshouImg },
  { id: 2, title: 'SPACE STATION', image: tiakongzhanImg },
  { id: 3, title: 'YUE LING', image: yuelingImg },
])
const thumbnailSlides = ref<HeroSlide[]>([])
const isThumbIntroActive = ref(false)
const carouselState = ref<'next' | 'prev' | ''>('')
const isAnimating = ref(false)
const transitionDuration = 1200

let animationTimer: ReturnType<typeof setTimeout> | null = null
let autoplayTimer: ReturnType<typeof setInterval> | null = null
let thumbIntroTimer: ReturnType<typeof setTimeout> | null = null

const rotateLeft = <T,>(arr: T[]) => [...arr.slice(1), arr[0]]
const rotateRight = <T,>(arr: T[]) => [arr[arr.length - 1], ...arr.slice(0, arr.length - 1)]

const startAutoplay = () => {
  stopAutoplay()
  autoplayTimer = setInterval(() => {
    showSlider('next')
  }, 4600)
}

const stopAutoplay = () => {
  if (autoplayTimer) {
    clearInterval(autoplayTimer)
    autoplayTimer = null
  }
}

const resetAnimation = () => {
  if (animationTimer) clearTimeout(animationTimer)
  animationTimer = setTimeout(() => {
    carouselState.value = ''
    isAnimating.value = false
  }, transitionDuration)
}

const showSlider = (type: 'next' | 'prev') => {
  if (isAnimating.value) return
  isAnimating.value = true

  if (type === 'next') {
    heroSlides.value = rotateLeft(heroSlides.value)
    thumbnailSlides.value = rotateLeft(thumbnailSlides.value)
  } else {
    heroSlides.value = rotateRight(heroSlides.value)
    thumbnailSlides.value = rotateRight(thumbnailSlides.value)
  }

  carouselState.value = type
  resetAnimation()
}

const scrollToContent = () => {
  const contentEl = document.querySelector('.content-section')
  if (contentEl) {
    contentEl.scrollIntoView({ behavior: 'smooth' })
  }
}

onMounted(async () => {
  try {
    const res = await getBlogStatsService()
    data.value = res.data
    articles.value = res.data?.popularArticles ?? []
  } catch (err) {
    console.error('获取博客统计失败:', err)
    articles.value = []
  }
  thumbnailSlides.value = rotateLeft(heroSlides.value)
  requestAnimationFrame(() => {
    isThumbIntroActive.value = true
    if (thumbIntroTimer) clearTimeout(thumbIntroTimer)
    thumbIntroTimer = setTimeout(() => {
      isThumbIntroActive.value = false
    }, 1600)
  })
  startAutoplay()
})

onBeforeUnmount(() => {
  stopAutoplay()
  if (animationTimer) clearTimeout(animationTimer)
  if (thumbIntroTimer) clearTimeout(thumbIntroTimer)
})
</script>

<template>
  <div class="page-container">
    <section class="hero">
      <div
        class="carousel"
        :class="carouselState"
        @mouseenter="stopAutoplay"
        @mouseleave="startAutoplay"
      >
        <div class="list">
          <div v-for="(item, index) in heroSlides" :key="item.id" class="item">
            <img
              :src="item.image"
              :alt="item.title"
              :fetchpriority="index === 0 ? 'high' : 'auto'"
              :loading="index === 0 ? 'eager' : 'lazy'"
              decoding="async"
            />
          </div>
        </div>

        <div class="hero-copy" :key="heroSlides[0]?.id">
          <h1 class="maintitle">RO-BLOG</h1>
          <p class="hero-subtitle">我们决定登月，他并非轻而易举，而正因为它困难重重。</p>
        </div>

        <div class="thumbnail" :class="{ 'thumb-intro': isThumbIntroActive }">
          <div
            v-for="(item, index) in thumbnailSlides"
            :key="`thumb-${item.id}`"
            class="item"
            :style="{ '--thumb-delay': `${index * 140}ms` }"
          >
            <img :src="item.image" :alt="item.title" loading="eager" decoding="auto" />
            <div class="thumb-title">{{ item.title }}</div>
          </div>
        </div>

        <div class="arrows">
          <button type="button" aria-label="上一张" @click="showSlider('prev')"><</button>
          <button type="button" aria-label="下一张" @click="showSlider('next')">></button>
        </div>
        <div class="time"></div>
      </div>

      <div class="scroll-down" @click="scrollToContent">
        <span></span><span></span><span></span>
      </div>
    </section>
    <Wave />
    <section class="content-section">
      <div class="layout-wrapper">
        <SideBar />
        <main class="post-list">
          <div class="article-items">
            <div
              class="card card-enter"
              v-for="(article, index) in articles"
              :key="article.id"
              :style="{ animationDelay: `${index * 80}ms` }"
            >
              <DummyCard v-bind="article" />
            </div>
          </div>
          <router-link to="/articles" class="view-more">查看更多</router-link>
        </main>
      </div>
    </section>
  </div>
</template>

<style scoped>
.hero {
  position: relative;
  height: 100vh;
  overflow: hidden;
}

.carousel {
  position: relative;
  width: 100vw;
  height: 100vh;
  margin-top: 0px;
  overflow: hidden;
}

.list .item {
  position: absolute;
  inset: 0;
}

.list .item img {
  position: absolute;
  left: 0;
  bottom: 0;
  width: 100%;
  height: 100vh;
  object-fit: cover;
  transform: translateZ(0);
  will-change: width, height, bottom, left, border-radius;
}

.list .item::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to right,
    rgba(0, 0, 0, 0.82) 0%,
    rgba(0, 0, 0, 0.38) 48%,
    rgba(0, 0, 0, 0.06) 100%
  );
}

.list .item:nth-child(1) {
  z-index: 1;
}

.hero-copy {
  position: absolute;
  top: 22%;
  left: 50%;
  transform: translateX(-50%);
  width: min(84%, 1140px);
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  z-index: 3;
  color: #fff;
  text-shadow: 0 5px 10px rgba(0, 0, 0, 0.55);
  pointer-events: none;
}

.maintitle {
  margin: 0 0 14px;
  width: fit-content;
  font-size: clamp(3rem, 8vw, 7rem);
  font-weight: 800;
  letter-spacing: 4px;
  line-height: 1;
  padding: 0.55rem 1.4rem;
  border: 2px solid #a4c9ff;
  border-radius: 30px;
  box-shadow:
    0 12px 24px -16px rgba(0, 0, 0, 0.55),
    0 0 0 1px rgba(255, 255, 255, 0.45),
    0 0 0 2px rgba(110, 160, 235, 0.28) inset,
    0 0 24px rgba(180, 215, 255, 0.36);
  opacity: 0;
  transform: translateY(38px) scale(0.94);
  will-change: transform, opacity;
  animation: heroTitleIn 1.08s cubic-bezier(0.19, 1, 0.22, 1) forwards;
}

.hero-subtitle {
  margin: 0;
  display: inline-flex;
  width: fit-content;
  max-width: min(62ch, 78vw);
  padding: 0.58rem 1rem;
  border-radius: 15px;
  border: 1px solid rgba(206, 220, 244, 0.45);
  backdrop-filter: blur(8px);
  font-size: clamp(0.96rem, 1.2vw, 1.14rem);
  line-height: 1.55;
  background: rgba(21, 39, 67, 0.46);
  box-shadow:
    0 10px 26px -20px rgba(0, 0, 0, 0.7),
    inset 0 1px 0 rgba(255, 255, 255, 0.18);
  opacity: 0;
  transform: translateY(30px);
  will-change: transform, opacity;
  animation: heroSubtitleIn 0.95s 0.34s cubic-bezier(0.19, 1, 0.22, 1) forwards;
}

@keyframes heroTitleIn {
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes heroSubtitleIn {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.thumbnail {
  position: absolute;
  bottom: 50px;
  left: 50%;
  width: max-content;
  display: flex;
  gap: 20px;
  z-index: 4;
}

.thumbnail .item {
  position: relative;
  width: 150px;
  height: 220px;
  border-radius: 15px;
  overflow: hidden;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.5);
}

.thumbnail.thumb-intro .item {
  opacity: 0;
  transform: translateY(34px) scale(0.9);
  animation: thumbEnter 0.88s var(--thumb-delay, 0ms) cubic-bezier(0.2, 1, 0.22, 1) forwards;
}

@keyframes thumbEnter {
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.thumbnail .item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.thumbnail .item:hover img {
  transform: scale(1.08);
}

.thumb-title {
  position: absolute;
  left: 10px;
  right: 10px;
  bottom: 10px;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.88);
}

.arrows {
  position: absolute;
  left: 9%;
  bottom: 60px;
  z-index: 5;
  display: flex;
  align-items: center;
  gap: 15px;
}

.arrows button {
  width: 45px;
  height: 45px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  font-size: 20px;
  cursor: pointer;
  backdrop-filter: blur(5px);
  transition: 0.3s;
}

.arrows button:hover {
  background: #fff;
  color: #000;
}

.carousel.next .list .item:nth-child(1) img {
  animation: showImage 1.2s cubic-bezier(0.25, 1, 0.5, 1) 1 forwards;
}

@keyframes showImage {
  0% {
    width: 150px;
    height: 220px;
    bottom: 50px;
    left: 50%;
    border-radius: 15px;
  }
  100% {
    width: 100%;
    height: 100%;
    bottom: 0;
    left: 0;
    border-radius: 0;
  }
}

.carousel.next .list .item:nth-child(1)::before {
  opacity: 0;
  animation: fadeInOverlay 1.2s cubic-bezier(0.25, 1, 0.5, 1) 1 forwards;
}

@keyframes fadeInOverlay {
  100% {
    opacity: 1;
  }
}

.carousel.next .thumbnail {
  transform: translateX(170px);
  animation: slideLeft 1s cubic-bezier(0.25, 1, 0.5, 1) 1 forwards;
}

@keyframes slideLeft {
  to {
    transform: translateX(0);
  }
}

.carousel.next .thumbnail .item:nth-last-child(1) {
  width: 0;
  opacity: 0;
  animation: showNewThumbnail 1s cubic-bezier(0.25, 1, 0.5, 1) 1 forwards;
}

.carousel.prev .list .item:nth-child(2) {
  z-index: 2;
}

.carousel.prev .list .item:nth-child(2) img {
  position: absolute;
  bottom: 0;
  left: 0;
  animation: outFrame 1s cubic-bezier(0.25, 1, 0.5, 1) 1 forwards;
}

@keyframes outFrame {
  to {
    width: 150px;
    height: 220px;
    bottom: 50px;
    left: 50%;
    border-radius: 15px;
  }
}

.carousel.prev .thumbnail .item:nth-child(1) {
  width: 0;
  opacity: 0;
  animation: showNewThumbnail 1s cubic-bezier(0.25, 1, 0.5, 1) 1 forwards;
}

@keyframes showNewThumbnail {
  to {
    width: 150px;
    opacity: 1;
  }
}

.time {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 6;
  width: 0;
  height: 3px;
  background-color: #f39c12;
}

.carousel.next .time,
.carousel.prev .time {
  animation: runningTime 1.2s linear 1 forwards;
}

@keyframes runningTime {
  from {
    width: 100%;
  }
  to {
    width: 0;
  }
}

.scroll-down {
  position: absolute;
  bottom: 26px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 6;
  cursor: pointer;
  opacity: 0.82;
}

.scroll-down span {
  display: block;
  width: 20px;
  height: 20px;
  border-right: 2px solid #fff;
  border-bottom: 2px solid #fff;
  transform: rotate(45deg);
  margin: -10px;
  animation: scroll 2s infinite;
}

.scroll-down span:nth-child(2) {
  animation-delay: 0.2s;
}

.scroll-down span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes scroll {
  0% {
    opacity: 0;
    transform: rotate(45deg) translate(-20px, -20px);
  }
  50% {
    opacity: 1;
  }
  100% {
    opacity: 0;
    transform: rotate(45deg) translate(20px, 20px);
  }
}

.view-more {
  text-align: center;
  background: #fcfdff;
  padding: 10px 20px;
  color: #2e3b52;
  border: 1px solid #dbe4f3;
  border-radius: 60px;
  text-decoration: none;
  margin: 1.25rem auto 0;
  transition: all 0.3s ease;
}

.view-more:hover {
  transform: translateY(-3px);
  box-shadow: 0 14px 26px -16px rgba(33, 49, 74, 0.45);
  border-color: #c9d8ef;
}

.content-section {
  min-height: 100vh;
}

.layout-wrapper {
  max-width: 1040px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 1.5rem;
  align-items: flex-start;
}

.post-list {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.article-items {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.card {
  min-width: 0;
}

.card-enter {
  opacity: 0;
  transform: translateY(14px);
  animation: cardFadeUp 520ms ease forwards;
}

@keyframes cardFadeUp {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 900px) {
  .hero-copy {
    top: 18%;
  }

  .thumbnail {
    gap: 12px;
    bottom: 86px;
  }

  .thumbnail .item {
    width: 100px;
    height: 150px;
  }

  .thumb-title {
    font-size: 11px;
  }

  .arrows {
    left: 6%;
    bottom: 95px;
  }

  .layout-wrapper {
    grid-template-columns: 1fr;
  }

  .article-items {
    grid-template-columns: 1fr;
  }
}
</style>
