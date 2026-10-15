// Horaires d'ouverture de FloreEvent (heure de Paris)

/** Ouverture et fermeture (heures pleines) pour chaque jour : 0 = dimanche, 6 = samedi. */
const SCHEDULE: Record<number, { opens: number; closes: number }> = {
  0: { opens: 11, closes: 17 },
  1: { opens: 8, closes: 19 },
  2: { opens: 8, closes: 19 },
  3: { opens: 8, closes: 19 },
  4: { opens: 8, closes: 19 },
  5: { opens: 8, closes: 19 },
  6: { opens: 8, closes: 19 },
}

/** Affichage : « Lundi – samedi : 8 h – 19 h ». */
export const HOURS = [
  { days: 'Lundi – samedi', hours: '8 h – 19 h' },
  { days: 'Dimanche', hours: '11 h – 17 h' },
]

const DAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

/** Jour et heure a Paris, quel que soit le fuseau du telephone ou de l'ordinateur du visiteur. */
function paris(date: Date): { day: number; hour: number; minute: number } {
  const parts = new Intl.DateTimeFormat('en-US', { timeZone: 'Europe/Paris', weekday: 'short', hour: 'numeric', minute: 'numeric', hourCycle: 'h23' }).formatToParts(date)
  const get = (type: string) => parts.find((part) => part.type === type)?.value ?? ''
  return { day: DAYS.indexOf(get('weekday')), hour: Number(get('hour')), minute: Number(get('minute')) }
}

/** « Ouvert · ferme à 19 h » ou « Fermé · ouvre demain à 8 h ». */
export function openingStatus(date = new Date()): { open: boolean; label: string } {
  const { day, hour, minute } = paris(date)
  const today = SCHEDULE[day]
  const time = hour + minute / 60
  if (time >= today.opens && time < today.closes) return { open: true, label: `Ouvert · ferme à ${today.closes} h` }
  if (time < today.opens) return { open: false, label: `Fermé · ouvre à ${today.opens} h` }
  return { open: false, label: `Fermé · ouvre demain à ${SCHEDULE[(day + 1) % 7].opens} h` }
}
