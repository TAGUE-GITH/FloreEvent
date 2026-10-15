import { HOURS, openingStatus } from './hours'

// Mi-octobre, Paris est a l'heure d'ete (UTC+2) : 08:00 a Paris = 06:00 UTC
const at = (iso: string) => new Date(iso)

describe('horaires de FloreEvent', () => {
  it('liste les horaires de la semaine', () => {
    expect(HOURS.map((line) => `${line.days} : ${line.hours}`)).toEqual(['Lundi – samedi : 8 h – 19 h', 'Dimanche : 11 h – 17 h'])
  })

  it('dit si FloreEvent est ouvert, selon l heure de Paris', () => {
    // Mercredi 14 octobre 2026
    expect(openingStatus(at('2026-10-14T09:30:00Z'))).toEqual({ open: true, label: 'Ouvert · ferme à 19 h' })
    expect(openingStatus(at('2026-10-14T05:30:00Z'))).toEqual({ open: false, label: 'Fermé · ouvre à 8 h' })
    expect(openingStatus(at('2026-10-14T18:00:00Z'))).toEqual({ open: false, label: 'Fermé · ouvre demain à 8 h' })
  })

  it('applique les horaires du dimanche', () => {
    // Samedi 17 octobre 2026 a 21 h : le lendemain est un dimanche
    expect(openingStatus(at('2026-10-17T19:00:00Z'))).toEqual({ open: false, label: 'Fermé · ouvre demain à 11 h' })
    // Dimanche 18 octobre 2026
    expect(openingStatus(at('2026-10-18T08:00:00Z'))).toEqual({ open: false, label: 'Fermé · ouvre à 11 h' })
    expect(openingStatus(at('2026-10-18T14:59:00Z'))).toEqual({ open: true, label: 'Ouvert · ferme à 17 h' })
  })
})
