// Coordonnees affichees sur le site. A completer par FloreEvent (telephone, adresse exacte, horaires, SIRET) :
// ce sont des valeurs de demonstration.

export const COMPANY = {
  name: 'FloreEvent',
  city: 'Melun',
  area: 'Melun et la Seine-et-Marne (jusqu’à 50 km)',
  address: 'Entrepôt de Melun (77000)',
  phone: '01 64 00 00 00',
  email: 'contact@floreevent.fr',
  mapsUrl: 'https://www.google.com/maps/search/?api=1&query=Melun%2077000',
}

/** Horaires de l'entrepot (retraits, retours et rendez-vous). */
export const OPENING_HOURS: { days: string; hours: string }[] = [
  { days: 'Lundi au vendredi', hours: '9 h – 18 h 30' },
  { days: 'Samedi', hours: '9 h – 13 h (retraits et retours)' },
  { days: 'Dimanche', hours: 'Fermé (livraisons possibles sur devis)' },
]

/** Zones de livraison depuis Melun (memes regles que le calcul du devis). */
export const DELIVERY_ZONES: { upTo: string; price: string }[] = [
  { upTo: 'Jusqu’à 15 km', price: '40 €' },
  { upTo: 'De 15 à 30 km', price: '70 €' },
  { upTo: 'De 30 à 50 km', price: '110 €' },
  { upTo: 'Au-delà', price: 'Sur devis' },
]
