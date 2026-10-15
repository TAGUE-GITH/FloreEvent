// Saisie des montants : l'utilisateur tape des euros (« 12,50 »), l'API recoit des centimes (1250, regle R5)

/** « 12,50 », « 12.5 », « 1 200 € » deviennent des centimes ; null si ce n'est pas un montant positif valide. */
export function parseEuros(text: string): number | null {
  const value = text.replace(/[\s  €]/g, '').replace(',', '.')
  if (!/^\d+(\.\d{1,2})?$/.test(value)) return null
  return Math.round(Number(value) * 100)
}

/** 1250 devient « 12,50 » et 15000 devient « 150 » (sans decimales inutiles). */
export function centsToInput(cents: number): string {
  return cents % 100 === 0 ? String(cents / 100) : (cents / 100).toFixed(2).replace('.', ',')
}
