<template>
  <div ref="container" class="parallax-container" @mousemove="handleMouseMove" @mouseleave="handleMouseLeave">
    <!-- UI Stack for 3D Parallax Effect -->
    <div ref="stackRef" class="ui-stack">
      
      <!-- Card 1: Metrics Stats Card (Background Layer, translateZ -45px) -->
      <div class="ui-card card-stats layer-bg">
        <div class="card-header">
          <div class="terminal-dots">
            <span class="dot red"></span>
            <span class="dot yellow"></span>
            <span class="dot green"></span>
          </div>
          <span class="card-title">KNOWLEDGE_METRICS</span>
        </div>
        <div class="card-content stats-panel">
          <!-- Text metrics -->
          <div class="stats-row">
            <div class="stat-box">
              <div class="skeleton-line-title"></div>
              <div class="skeleton-line-value" :style="{ background: color }"></div>
            </div>
            <div class="stat-box">
              <div class="skeleton-line-title"></div>
              <div class="skeleton-line-value" :style="{ background: secondaryColor }"></div>
            </div>
          </div>
          
          <!-- SVG Sparkline Line Chart -->
          <div class="stats-chart">
            <svg viewBox="0 0 260 70" width="100%" height="100%">
              <!-- Area under curve -->
              <path d="M 10 60 C 40 20, 60 50, 90 15 C 120 50, 140 10, 170 38 C 200 15, 220 45, 250 10 L 250 60 L 10 60 Z" 
                    :fill="color" opacity="0.08" />
              <!-- Trend Line -->
              <path d="M 10 60 C 40 20, 60 50, 90 15 C 120 50, 140 10, 170 38 C 200 15, 220 45, 250 10" 
                    fill="none" :stroke="color" stroke-width="1.8" class="sparkline-path" />
              <!-- Pulsing indicator point on the chart end -->
              <circle cx="250" cy="10" r="3.5" :fill="color" />
              <circle cx="250" cy="10" r="8" :fill="color" opacity="0.35" class="spark-node-ripple" />
            </svg>
          </div>
        </div>
      </div>

      <!-- Card 2: Interactive SVG Constellation Node Map (Middle Layer, translateZ 15px) -->
      <div class="ui-card card-graph layer-middle">
        <div class="card-header">
          <div class="header-indicator" :style="{ background: color }"></div>
          <span class="card-title">KNOWLEDGE_CONSTELLATION</span>
        </div>
        <div class="card-content graph-viewport">
          <svg viewBox="0 0 260 180" width="100%" height="100%">
            <!-- Holographic background grid lines -->
            <line x1="20" y1="90" x2="240" y2="90" stroke="var(--border)" stroke-dasharray="2 4" stroke-opacity="0.25" />
            <line x1="130" y1="20" x2="130" y2="160" stroke="var(--border)" stroke-dasharray="2 4" stroke-opacity="0.25" />
            
            <!-- Dynamic connection lines -->
            <g class="graph-links">
              <line x1="40" y1="90" x2="95" y2="45" :stroke="color" class="line-link" />
              <line x1="95" y1="45" x2="165" y2="55" :stroke="color" class="line-link" />
              <line x1="165" y1="55" x2="215" y2="110" :stroke="color" class="line-link" />
              <line x1="215" y1="110" x2="145" y2="130" :stroke="color" class="line-link" />
              <line x1="145" y1="130" x2="95" y2="45" :stroke="color" class="line-link" />
              <line x1="40" y1="90" x2="145" y2="130" :stroke="color" class="line-link" />
              <line x1="165" y1="55" x2="145" y2="130" :stroke="color" class="line-link" />
            </g>

            <!-- Node Points -->
            <g class="graph-nodes">
              <!-- Node 1 -->
              <g class="node-group node-g-1">
                <circle cx="40" cy="90" r="14" :fill="color" opacity="0.08" class="ripple-outer" />
                <circle cx="40" cy="90" r="8" :fill="color" opacity="0.2" class="ripple-inner" />
                <circle cx="40" cy="90" r="4" :fill="color" />
              </g>
              <!-- Node 2 -->
              <g class="node-group node-g-2">
                <circle cx="95" cy="45" r="16" :fill="secondaryColor" opacity="0.08" class="ripple-outer" />
                <circle cx="95" cy="45" r="9" :fill="secondaryColor" opacity="0.2" class="ripple-inner" />
                <circle cx="95" cy="45" r="5.5" :fill="secondaryColor" />
              </g>
              <!-- Node 3 -->
              <g class="node-group node-g-3">
                <circle cx="165" cy="55" r="12" :fill="color" opacity="0.08" class="ripple-outer" />
                <circle cx="165" cy="55" r="7" :fill="color" opacity="0.2" class="ripple-inner" />
                <circle cx="165" cy="55" r="3.5" :fill="color" />
              </g>
              <!-- Node 4 -->
              <g class="node-group node-g-4">
                <circle cx="215" cy="110" r="15" :fill="color" opacity="0.08" class="ripple-outer" />
                <circle cx="215" cy="110" r="8" :fill="color" opacity="0.2" class="ripple-inner" />
                <circle cx="215" cy="110" r="4.5" :fill="color" />
              </g>
              <!-- Node 5 -->
              <g class="node-group node-g-5">
                <circle cx="145" cy="130" r="14" :fill="secondaryColor" opacity="0.08" class="ripple-outer" />
                <circle cx="145" cy="130" r="8" :fill="secondaryColor" opacity="0.2" class="ripple-inner" />
                <circle cx="145" cy="130" r="4" :fill="secondaryColor" />
              </g>
            </g>
          </svg>
        </div>
      </div>

      <!-- Card 3: AI Chat Bubble Card (Foreground Layer, translateZ 75px) -->
      <div class="ui-card card-chat layer-fg">
        <div class="chat-header">
          <div class="bot-avatar" :style="{ background: `linear-gradient(135deg, ${color}, ${secondaryColor})` }">
            <Bot :size="14" class="avatar-icon" />
          </div>
          <div class="bot-info">
            <span class="bot-name">Starlore Copilot</span>
            <span class="bot-status">Agent active</span>
          </div>
        </div>
        <div class="chat-body">
          <div class="skeleton-chat-line"></div>
          <div class="skeleton-chat-line short"></div>
          <div class="chat-badges">
            <span class="chat-badge-skeleton"></span>
            <span class="chat-badge-skeleton"></span>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { gsap } from 'gsap'
import { Bot } from '@lucide/vue'

const props = withDefaults(defineProps<{
  color?: string
  secondaryColor?: string
}>(), {
  color: '#E85D2A',
  secondaryColor: '#FFAE19',
})

const container = ref<HTMLDivElement>()
const stackRef = ref<HTMLDivElement>()
let ctx: gsap.Context | null = null

// GSAP Animations and Parallax Effect
onMounted(() => {
  const elStack = stackRef.value
  if (!container.value || !elStack) return

  ctx = gsap.context(() => {
    // 1. Organic slow floating of individual cards to make them feel floating in space
    gsap.to('.card-stats', {
      y: '+=6',
      duration: 3.5,
      repeat: -1,
      yoyo: true,
      ease: 'sine.inOut'
    })

    gsap.to('.card-graph', {
      y: '-=8',
      duration: 4.5,
      repeat: -1,
      yoyo: true,
      ease: 'sine.inOut',
      delay: 0.4
    })

    gsap.to('.card-chat', {
      y: '+=5',
      duration: 4,
      repeat: -1,
      yoyo: true,
      ease: 'sine.inOut',
      delay: 0.8
    })

    // 2. Ripple concentric node pulses on the SVG Map
    gsap.to('.ripple-outer', {
      r: '+=6',
      opacity: 0,
      duration: 2,
      repeat: -1,
      ease: 'power1.out',
      stagger: {
        each: 0.3,
        repeat: -1
      }
    })

    gsap.to('.ripple-inner', {
      r: '+=3',
      opacity: 0.05,
      duration: 2,
      repeat: -1,
      ease: 'power1.out',
      stagger: {
        each: 0.3,
        repeat: -1
      }
    })

    // 3. Line flow stroke animation
    gsap.fromTo('.line-link', {
      strokeDasharray: '4 4',
      strokeDashoffset: 20
    }, {
      strokeDashoffset: 0,
      duration: 3,
      repeat: -1,
      ease: 'none'
    })

    // 4. Sparkline pulsing end node ripple
    gsap.to('.spark-node-ripple', {
      r: '+=4',
      opacity: 0,
      duration: 1.5,
      repeat: -1,
      ease: 'power1.out'
    })

  }, container.value)
})

onUnmounted(() => {
  ctx?.revert()
})

// Parallax Move listener
function handleMouseMove(e: MouseEvent) {
  const elContainer = container.value
  const elStack = stackRef.value
  if (!elContainer || !elStack) return

  const rect = elContainer.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top

  // Calculate normalized coordinate offset from center (-0.5 to 0.5)
  const normX = x / rect.width - 0.5
  const normY = y / rect.height - 0.5

  // Smoothly rotate the entire stack in 3D perspective
  gsap.to(elStack, {
    rotateY: normX * 36, // scale tilt up to 18 deg
    rotateX: -normY * 36, // scale tilt up to 18 deg
    duration: 0.5,
    ease: 'power2.out',
    overwrite: 'auto'
  })
}

// Reset orientation on mouse leave
function handleMouseLeave() {
  const elStack = stackRef.value
  if (!elStack) return

  gsap.to(elStack, {
    rotateY: 0,
    rotateX: 0,
    duration: 0.8,
    ease: 'power2.out',
    overwrite: 'auto'
  })
}
</script>

<style scoped src="./ParticleGlobe.css"></style>

