import type { BookingStatus } from '../api/types'

type Tone = 'slate' | 'red' | 'amber' | 'green' | 'brand'

/** Couleur du badge de chaque etape d'une reservation. */
export function bookingTone(status: BookingStatus): Tone {
  const tones: Record<BookingStatus, Tone> = {
    DEMANDE: 'amber',
    DEVIS: 'brand',
    CONFIRMEE: 'green',
    EN_COURS: 'brand',
    RETOURNEE: 'slate',
    CLOTUREE: 'slate',
    ANNULEE: 'slate',
  }
  return tones[status]
}
