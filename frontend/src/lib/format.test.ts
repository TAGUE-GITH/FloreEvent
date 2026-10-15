import { formatDate, formatEuros, formatPeriod } from './format'

// Intl separe les milliers par une espace fine insecable : on la normalise pour comparer
const plain = (text: string) => text.replace(/[  ]/g, ' ')

describe('formatEuros', () => {
  it('affiche un montant en centimes en euros, a la francaise', () => {
    expect(plain(formatEuros(125050))).toBe('1 250,50 €')
    expect(plain(formatEuros(4000))).toBe('40,00 €')
    expect(plain(formatEuros(5))).toBe('0,05 €')
  })

  it('affiche 0,00 € pour une valeur absente', () => {
    expect(plain(formatEuros(Number.NaN))).toBe('0,00 €')
  })
})

describe('formatDate', () => {
  it('affiche une date courte en francais', () => {
    expect(formatDate('2026-10-14T10:00:00')).toBe('14 oct. 2026')
  })
})

describe('formatPeriod', () => {
  it('affiche une periode de location', () => {
    expect(formatPeriod('2026-11-14', '2026-11-14')).toBe('le 14 nov. 2026')
    expect(formatPeriod('2026-11-14', '2026-11-16')).toBe('du 14 nov. au 16 nov. 2026')
  })
})
