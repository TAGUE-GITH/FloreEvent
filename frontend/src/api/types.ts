// Types des donnees echangees avec l'API Spring Boot. Les montants sont en centimes d'euro (regle R5).

export type Role = 'GERANT' | 'COMMERCIAL' | 'LOGISTIQUE' | 'CLIENT'

/** Equipe de FloreEvent (l'espace de gestion), par opposition aux clients du site */
export const STAFF_ROLES: Role[] = ['GERANT', 'COMMERCIAL', 'LOGISTIQUE']

export const ROLE_LABELS: Record<Role, string> = {
  GERANT: 'Gérant',
  COMMERCIAL: 'Commercial',
  LOGISTIQUE: 'Logistique',
  CLIENT: 'Client',
}

export type User = {
  id: number
  email: string
  firstName: string
  lastName: string
  fullName: string
  phone: string | null
  role: Role
  active: boolean
  createdAt: string
  avatarUrl?: string
  customerId?: number
}

export type NewUser = {
  email: string
  firstName: string
  lastName: string
  phone: string
  role: Role
  password: string
}

export type Named = { id: number; name: string }

export type Category = Named & { itemCount: number }

export type Photo = { id: string; url: string }

export type Item = {
  id: number
  reference: string
  name: string
  category: Named
  description?: string
  dayPriceCents: number
  replacementValueCents: number
  setupPriceCents: number
  quantity: number
  /** Exemplaires en reparation aujourd'hui */
  inRepair: number
  active: boolean
  imageUrl?: string
  images: Photo[]
}

export type ItemForm = {
  reference: string
  name: string
  categoryId: number | ''
  description: string
  dayPriceCents: number
  replacementValueCents: number
  setupPriceCents: number
  quantity: number | ''
  active: boolean
}

export type Maintenance = {
  id: number
  itemId: number
  itemName: string
  quantity: number
  fromDate: string
  toDate?: string
  reason?: string
  author: string
  createdAt: string
}

export type MaintenanceForm = { quantity: number | ''; fromDate: string; toDate: string; reason: string }

export type RegisterForm = {
  firstName: string
  lastName: string
  email: string
  phone: string
  company: string
  password: string
  acceptTerms: boolean
}

/** Article du catalogue public : available = exemplaires libres aux dates choisies */
export type PublicItem = {
  id: number
  name: string
  category: Named
  description?: string
  dayPriceCents: number
  setupPriceCents: number
  imageUrl?: string
  images: string[]
  available?: number
}

export type DeliveryMode = 'RETRAIT' | 'LIVRAISON'

export type BookingStatus = 'DEMANDE' | 'DEVIS' | 'CONFIRMEE' | 'EN_COURS' | 'RETOURNEE' | 'CLOTUREE' | 'ANNULEE'

export type BookingRequest = {
  startDate: string
  endDate: string
  deliveryMode: DeliveryMode
  address: string
  installation: boolean
  note: string
  lines: { itemId: number; quantity: number }[]
}

export type Estimate = {
  startDate: string
  endDate: string
  days: number
  deliveryMode: DeliveryMode
  address?: string
  distanceKm?: number
  installation: boolean
  lines: { itemId: number; name: string; quantity: number; dayPriceCents: number; lineTotalCents: number; available: number }[]
  rentalCents: number
  setupCents: number
  deliveryCents: number
  totalCents: number
  vatCents: number
  depositCents: number
  securityDepositCents: number
  bookable: boolean
  problems: string[]
}

export type BookingSummary = {
  id: number
  reference: string
  status: BookingStatus
  statusLabel: string
  customer: string
  startDate: string
  endDate: string
  deliveryMode: DeliveryMode
  totalCents: number
  itemCount: number
  createdAt: string
}

export type Booking = {
  id: number
  reference: string
  status: BookingStatus
  statusLabel: string
  customer: { id: number; fullName: string; company?: string; phone: string; email?: string }
  startDate: string
  endDate: string
  days: number
  deliveryMode: DeliveryMode
  address?: string
  distanceKm?: number
  installation: boolean
  note?: string
  lines: { itemId: number; itemName: string; imageUrl?: string; quantity: number; dayPriceCents: number; lineTotalCents: number }[]
  rentalCents: number
  setupCents: number
  deliveryCents: number
  totalCents: number
  vatCents: number
  depositCents: number
  securityDepositCents: number
  createdAt: string
  events: { status: BookingStatus; label: string; note?: string; author?: string; createdAt: string }[]
}
