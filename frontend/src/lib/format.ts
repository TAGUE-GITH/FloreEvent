// Montants stockes en centimes d'euro (regle R5) : 125050 s'affiche « 1 250,50 € »
const EUROS = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' })

export function formatEuros(cents: number): string {
  return EUROS.format(Number.isFinite(cents) ? cents / 100 : 0)
}

// Date courte en francais : « 14 oct. 2026 »
export function formatDate(value: string | Date): string {
  return new Date(value).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' })
}

// Periode de location : « le 14 nov. 2026 » ou « du 14 nov. au 16 nov. 2026 »
export function formatPeriod(startDate: string, endDate: string): string {
  const short = (value: string) => new Date(value).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', timeZone: 'UTC' })
  const long = (value: string) => new Date(value).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC' })
  return startDate === endDate ? `le ${long(startDate)}` : `du ${short(startDate)} au ${long(endDate)}`
}
