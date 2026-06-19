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
              <span class="stat-box-label">Connected Stars</span>
              <span class="stat-box-value" :style="{ color: color }">128</span>
            </div>
            <div class="stat-box">
              <span class="stat-box-label">Graph Accuracy</span>
              <span class="stat-box-value" :style="{ color: secondaryColor }">94%</span>
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
          <p class="chat-message-text">已为您自动检索并关联 3 篇相关星记，并生成了最新知识星图节点。</p>
          <div class="chat-badges">
            <span class="chat-badge">RAG Active</span>
            <span class="chat-badge">Constellation Generated</span>
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

<style scoped>
.parallax-container {
  width: 100%;
  height: 100%;
  min-height: 420px;
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  perspective: 1200px; /* Crucial for 3D depth */
  overflow: visible;
}

.ui-stack {
  position: relative;
  width: 380px;
  height: 380px;
  transform-style: preserve-3d; /* Keep Z translations working inside */
  display: flex;
  justify-content: center;
  align-items: center;
  pointer-events: none;
}

/* Individual Glassmorphic UI Cards */
.ui-card {
  position: absolute;
  background: var(--glass-bg);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: 
    0 15px 35px rgba(0, 0, 0, 0.15),
    inset 0 0 15px rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  transition: border-color 0.4s ease, box-shadow 0.4s ease;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.ui-card:hover {
  border-color: var(--accent);
  box-shadow: 
    0 20px 45px rgba(0, 0, 0, 0.2),
    0 0 25px var(--accent-soft);
}

/* Card Positions inside 3D space */
.card-stats {
  top: 10px;
  left: 0px;
  width: 290px;
  height: 180px;
  transform: translateZ(-40px); /* Pushed into background */
  z-index: 1;
}

.card-graph {
  top: 95px;
  left: 95px;
  width: 275px;
  height: 195px;
  transform: translateZ(15px); /* Middle layer */
  z-index: 2;
}

.card-chat {
  bottom: 15px;
  right: 5px;
  width: 265px;
  height: 155px;
  transform: translateZ(70px); /* Popped in foreground */
  z-index: 3;
}

/* Card Headers */
.card-header {
  padding: 10px 14px;
  background: rgba(0, 0, 0, 0.2);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-title {
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--ink-soft);
  letter-spacing: 0.05em;
  text-align: left;
}

.header-indicator {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  box-shadow: 0 0 6px var(--accent);
}

/* Terminal Dots */
.terminal-dots {
  display: flex;
  gap: 5px;
}

.terminal-dots .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.dot.red { background: #ff5f56; }
.dot.yellow { background: #ffbd2e; }
.dot.green { background: #27c93f; }

/* Metrics Stats Card Content */
.stats-panel {
  padding: 16px;
  flex: 1;
  background: transparent;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 12px;
}

.stats-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.stat-box {
  flex: 1;
  background: rgba(128, 128, 128, 0.03);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.stat-box-label {
  font-size: 0.65rem;
  color: var(--ink-muted);
  text-transform: uppercase;
  font-family: 'Fira Code', 'Consolas', monospace;
}

.stat-box-value {
  font-size: 1.4rem;
  font-weight: 800;
  font-family: 'Fira Code', 'Consolas', monospace;
}

.stats-chart {
  flex: 1;
  background: rgba(128, 128, 128, 0.04);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 4px 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Graph (Middle) Card Content */
.graph-viewport {
  flex: 1;
  padding: 8px;
  display: flex;
  justify-content: center;
  align-items: center;
  background: rgba(0, 0, 0, 0.05);
}

.line-link {
  stroke-width: 1.2px;
  stroke-opacity: 0.35;
}

.node-group {
  transition: transform 0.3s ease;
}

.node-group:hover {
  transform: scale(1.3);
  cursor: pointer;
}

/* Chat Card Content */
.chat-header {
  padding: 10px 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.02);
}

.bot-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
}

.bot-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 1px;
}

.bot-name {
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--ink);
}

.bot-status {
  font-size: 0.62rem;
  color: #9ece6a;
  font-family: 'Fira Code', 'Consolas', monospace;
  text-transform: uppercase;
}

.chat-body {
  padding: 12px 14px;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 8px;
  text-align: left;
}

.chat-message-text {
  font-size: 0.78rem;
  color: var(--ink-soft);
  line-height: 1.5;
  margin: 0;
}

.chat-badges {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.chat-badge {
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.6rem;
  padding: 3px 6px;
  background: var(--accent-soft);
  color: var(--accent);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  font-weight: 600;
}

/* Responsive adjustment */
@media (max-width: 600px) {
  .ui-stack {
    width: 300px;
    height: 300px;
  }
  .card-stats {
    width: 230px;
    height: 150px;
  }
  .card-graph {
    width: 210px;
    height: 150px;
    top: 70px;
    left: 70px;
  }
  .card-chat {
    width: 200px;
    height: 120px;
    bottom: 10px;
    right: 5px;
  }
}
</style>
