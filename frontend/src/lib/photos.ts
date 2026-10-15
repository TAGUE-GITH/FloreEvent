// Photos de la vitrine : photos libres de droits (licence Unsplash : usage commercial gratuit, sans attribution obligatoire),
// servies par le CDN d'Unsplash a la taille demandee.
// A remplacer par les vraies photos des evenements de FloreEvent : il suffit de changer les entrees ci-dessous
// (une adresse d'image complete fonctionne aussi).

export type Photo = { id: string; alt: string }

export const PHOTOS = {
  hero: { id: 'photo-1772127822525-7eda37383b9f', alt: 'Réception de mariage en plein air avec baldaquin et tables drapées' },
  wedding: { id: 'photo-1779308936221-89739e035a53', alt: 'Salle de réception décorée d’une arche florale blanche' },
  birthday: { id: 'photo-1741969494307-55394e3e4071', alt: 'Fête d’anniversaire décorée de ballons' },
  seminar: { id: 'photo-1511578314322-379afb476865', alt: 'Salle de séminaire avec tables rondes, chaises blanches et écrans' },
  tent: { id: 'photo-1775476793931-cb484f197760', alt: 'Tente de réception avec tables rondes et chaises' },
  tables: { id: 'photo-1763553113332-800519753e40', alt: 'Tables de réception avec centres de table floraux' },
  florist: { id: 'photo-1612633783539-e9926f45cc41', alt: 'Fleuriste composant un bouquet de fleurs blanches et roses' },
  lights: { id: 'photo-1768594407490-3a7aeb0ec8cc', alt: 'Décoration bohème avec guirlandes lumineuses suspendues et plantes' },
  dinner: { id: 'photo-1761114905078-163aa92141c8', alt: 'Dîner de mariage, invités assis aux tables' },
  chairs: { id: 'photo-1653518882586-588362773209', alt: 'Cérémonie en plein air avec chaises et parasols blancs' },
  tableware: { id: 'photo-1758810742443-b82f48355828', alt: 'Table d’extérieur avec centre de table floral et couverts' },
  ballroom: { id: 'photo-1761110787206-2cc164e4913c', alt: 'Salle élégante avec tables rondes et lustres' },
  party: { id: 'photo-1464047736614-af63643285bf', alt: 'Fanions blancs et guirlandes lumineuses dorées' },
  dj: { id: 'photo-1786041065758-65eab0b5f441', alt: 'DJ animant une fête en plein air avec de grandes enceintes' },
} satisfies Record<string, Photo>

/** Adresse de la photo recadree a la taille voulue, compressee et au format le plus leger pour le navigateur. */
export function photoUrl(photo: Photo, width: number, height?: number): string {
  if (photo.id.startsWith('http')) return photo.id
  const params = new URLSearchParams({ w: String(width), ...(height ? { h: String(height) } : {}), fit: 'crop', auto: 'format', q: '70' })
  return `https://images.unsplash.com/${photo.id}?${params}`
}

/** Plusieurs largeurs : le navigateur choisit selon l'ecran (petite image sur un telephone). */
export function photoSrcSet(photo: Photo, widths: number[], ratio: number): string {
  return widths.map((width) => `${photoUrl(photo, width, Math.round(width / ratio))} ${width}w`).join(', ')
}
