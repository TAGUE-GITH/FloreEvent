// Coordonnees affichees sur le site (adresse exacte et SIRET a completer par FloreEvent).
// Les horaires sont dans lib/hours.ts.

export const COMPANY = {
  name: 'FloreEvent',
  city: 'Melun',
  area: 'Melun et la Seine-et-Marne (jusqu’à 50 km)',
  address: 'Entrepôt de Melun (77000)',
  phone: '+33 6 68 98 88 53',
  /** Numero pour les liens « appeler » et WhatsApp */
  phoneLink: '+33668988853',
  whatsapp: 'https://wa.me/33668988853',
  email: 'contact@floreevent.fr',
  mapsUrl: 'https://www.google.com/maps/search/?api=1&query=Melun%2077000',
}

/** Zones de livraison depuis Melun (memes regles que le calcul du devis). */
export const DELIVERY_ZONES: { upTo: string; price: string }[] = [
  { upTo: 'Jusqu’à 15 km', price: '40 €' },
  { upTo: 'De 15 à 30 km', price: '70 €' },
  { upTo: 'De 30 à 50 km', price: '110 €' },
  { upTo: 'Au-delà', price: 'Sur devis' },
]
