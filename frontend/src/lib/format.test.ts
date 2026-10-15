import { formatDate, formatEuros } from './format'

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
