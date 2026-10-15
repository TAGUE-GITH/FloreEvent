// Selection de materiel du visiteur (son devis en cours) : fonctions pures, testees seules.
// Le serveur recalcule toujours les prix et les disponibilites.
import type { BookingRequest, DeliveryMode } from '../api/types'

export type SelectionLine = { itemId: number; name: string; imageUrl?: string; dayPriceCents: number; setupPriceCents: number; quantity: number }

export type Selection = {
  startDate: string
  endDate: string
  deliveryMode: DeliveryMode
  address: string
  installation: boolean
  /** Precisions sur l'evenement (gardees si le visiteur cree son compte entre-temps) */
  note: string
  lines: SelectionLine[]
}

type ItemLike = { id: number; name: string; imageUrl?: string; dayPriceCents: number; setupPriceCents: number }

export const emptySelection = (): Selection => ({ startDate: '', endDate: '', deliveryMode: 'RETRAIT', address: '', installation: false, note: '', lines: [] })

export function addItem(selection: Selection, item: ItemLike, quantity: number): Selection {
  const existing = selection.lines.find((line) => line.itemId === item.id)
  if (existing) return setQuantity(selection, item.id, existing.quantity + quantity)
  const line = { itemId: item.id, name: item.name, imageUrl: item.imageUrl, dayPriceCents: item.dayPriceCents, setupPriceCents: item.setupPriceCents, quantity }
  return { ...selection, lines: [...selection.lines, line] }
}

/** A zero, la ligne disparait. */
export function setQuantity(selection: Selection, itemId: number, quantity: number): Selection {
  return {
    ...selection,
    lines: selection.lines.map((line) => (line.itemId === itemId ? { ...line, quantity: Math.max(0, quantity) } : line)).filter((line) => line.quantity > 0),
  }
}

export const itemCount = (selection: Selection) => selection.lines.reduce((sum, line) => sum + line.quantity, 0)

/** Nombre de jours de location, premier et dernier jour inclus ; null si les dates manquent ou sont inversees. */
export function rentalDays(startDate: string, endDate: string): number | null {
  if (!startDate || !endDate) return null
  const days = (Date.parse(endDate) - Date.parse(startDate)) / 86_400_000 + 1
  return days >= 1 ? Math.round(days) : null
}

/** L'adresse et l'installation ne comptent que pour une livraison. */
export function toRequest(selection: Selection): BookingRequest {
  const delivery = selection.deliveryMode === 'LIVRAISON'
  return {
    startDate: selection.startDate,
    endDate: selection.endDate,
    deliveryMode: selection.deliveryMode,
    address: delivery ? selection.address : '',
    installation: delivery && selection.installation,
    note: selection.note,
    lines: selection.lines.map((line) => ({ itemId: line.itemId, quantity: line.quantity })),
  }
}

/** Selection gardee dans le navigateur : un contenu abime ou ancien est simplement ignore. */
export function readStoredSelection(raw: string | null): Selection {
  if (!raw) return emptySelection()
  try {
    const value = JSON.parse(raw)
    const valid =
      value && Array.isArray(value.lines) &&
      value.lines.every((line: SelectionLine) => typeof line?.itemId === 'number' && typeof line?.quantity === 'number' && typeof line?.dayPriceCents === 'number')
    return valid ? { ...emptySelection(), ...value } : emptySelection()
  } catch {
    return emptySelection()
  }
}
