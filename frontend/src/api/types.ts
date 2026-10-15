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
