// Dates au format des champs <input type="date"> (AAAA-MM-JJ), a l'heure de Paris (FloreEvent est a Melun)
export function todayIso(now = new Date()): string {
  return now.toLocaleDateString('sv-SE', { timeZone: 'Europe/Paris' })
}
