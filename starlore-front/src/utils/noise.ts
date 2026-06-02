// 简单的 2D 噪声算法实现 (替代原本的 makeNoise2D)
// 这是一个经典的 Perlin Noise 简化版，用于生成平滑的随机流动
const PERLIN_YWRAPB = 4
const PERLIN_YWRAP = 1 << PERLIN_YWRAPB
const PERLIN_ZWRAPB = 8
const PERLIN_ZWRAP = 1 << PERLIN_ZWRAPB
const PERLIN_SIZE = 4095

let perlin_octaves = 4 // default to medium smooth
let perlin_amp_falloff = 0.5 // 50% reduction/octave

const scaled_cosine = (i: number) => 0.5 * (1.0 - Math.cos(i * Math.PI))

let perlin: number[] | null = null

export const makeNoise2D = () => {
  if (perlin == null) {
    perlin = new Array(PERLIN_SIZE + 1)
    for (let i = 0; i < PERLIN_SIZE + 1; i++) {
      perlin[i] = Math.random()
    }
  }

  return (x: number, y: number) => {
    let xi = Math.floor(x)
    let yi = Math.floor(y)
    let xf = x - xi
    let yf = y - yi
    let rxf, ryf

    let r = 0
    let ampl = 0.5

    let n1, n2, n3

    for (let o = 0; o < perlin_octaves; o++) {
      let of = xi + (yi << PERLIN_YWRAPB)

      rxf = scaled_cosine(xf)
      ryf = scaled_cosine(yf)

      n1 = perlin![of & PERLIN_SIZE]
      n1 += rxf * (perlin![(of + 1) & PERLIN_SIZE] - n1)
      n2 = perlin![(of + PERLIN_YWRAP) & PERLIN_SIZE]
      n2 += rxf * (perlin![(of + PERLIN_YWRAP + 1) & PERLIN_SIZE] - n2)
      n1 += ryf * (n2 - n1)

      of += PERLIN_ZWRAP
      n2 = perlin![of & PERLIN_SIZE]
      n2 += rxf * (perlin![(of + 1) & PERLIN_SIZE] - n2)
      n3 = perlin![(of + PERLIN_YWRAP) & PERLIN_SIZE]
      n3 += rxf * (perlin![(of + PERLIN_YWRAP + 1) & PERLIN_SIZE] - n3)
      n2 += ryf * (n3 - n2)

      r += n1 * ampl
      ampl *= perlin_amp_falloff
      xi <<= 1
      xf *= 2
      yi <<= 1
      yf *= 2

      if (xf >= 1.0) {
        xi++
        xf--
      }
      if (yf >= 1.0) {
        yi++
        yf--
      }
    }
    return r
  }
}

// 随机数工具
export const rand = (min: number, max: number) => Math.random() * (max - min) + min