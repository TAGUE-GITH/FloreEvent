import { centsToInput, parseEuros } from './money'

describe('saisie des montants en euros', () => {
  it('convertit un montant saisi en centimes', () => {
    expect(parseEuros('150')).toBe(15000)
    expect(parseEuros('12,50')).toBe(1250)
    expect(parseEuros('12.5')).toBe(1250)
    expect(parseEuros(' 1 200,00 € ')).toBe(120000)
    expect(parseEuros('0,05')).toBe(5)
  })

  it('refuse ce qui n est pas un montant', () => {
    expect(parseEuros('')).toBeNull()
    expect(parseEuros('abc')).toBeNull()
    expect(parseEuros('-3')).toBeNull()
    expect(parseEuros('1,234')).toBeNull()
  })

  it('prepare un montant pour un champ de saisie', () => {
    expect(centsToInput(15000)).toBe('150')
    expect(centsToInput(1250)).toBe('12,50')
    expect(centsToInput(5)).toBe('0,05')
  })
})
