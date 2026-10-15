import { todayIso } from './dates'

describe('dates', () => {
  it('donne la date du jour a Paris au format des champs de date', () => {
    // 23 h 30 a Londres (UTC) le 15 octobre = deja le 16 octobre a Paris
    expect(todayIso(new Date('2026-10-15T23:30:00Z'))).toBe('2026-10-16')
    expect(todayIso(new Date('2026-10-15T08:00:00Z'))).toBe('2026-10-15')
  })
})
