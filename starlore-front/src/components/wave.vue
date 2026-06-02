<template>
  <div class="waves-container">
    <svg
      class="waves"
      xmlns="http://www.w3.org/2000/svg"
      xmlns:xlink="http://www.w3.org/1999/xlink"
      viewBox="0 24 150 28"
      preserveAspectRatio="none"
      shape-rendering="auto"
    >
      <defs>
        <path
          id="gentle-wave"
          d="M-160 44c30 0 58-18 88-18s 58 18 88 18 58-18 88-18 58 18 88 18 v44h-352z"
        />
      </defs>
      <g class="parallax">
        <use xlink:href="#gentle-wave" x="48" y="0" fill="rgba(111, 151, 210, 0.5)" />
        <use xlink:href="#gentle-wave" x="48" y="3" fill="rgba(146, 178, 225, 0.45)" />
        <use xlink:href="#gentle-wave" x="48" y="5" fill="rgba(186, 208, 236, 0.35)" />
        <use xlink:href="#gentle-wave" x="48" y="7" fill="#f1f4fb" />
      </g>
    </svg>
  </div>
</template>
