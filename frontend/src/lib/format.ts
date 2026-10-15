// Montants stockes en centimes d'euro (regle R5) : 125050 s'affiche « 1 250,50 € »
const EUROS = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' })

export function formatEuros(cents: number): string {
  return EUROS.format(Number.isFinite(cents) ? cents / 100 : 0)
}

// Date courte en francais : « 14 oct. 2026 »
export function formatDate(value: string | Date): string {
  return new Date(value).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' })
}
