import { fitWithin } from './image'

describe('reduction des photos', () => {
  it('garde les petites images telles quelles', () => {
    expect(fitWithin(300, 200, 512)).toEqual({ width: 300, height: 200 })
  })

  it('reduit les grandes en gardant les proportions', () => {
    expect(fitWithin(4000, 3000, 512)).toEqual({ width: 512, height: 384 })
    expect(fitWithin(1080, 1920, 512)).toEqual({ width: 288, height: 512 })
  })
})
