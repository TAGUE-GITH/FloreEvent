import axios, { AxiosError } from 'axios'
import type {
  Booking,
  BookingRequest,
  CancellationPreview,
  BookingSummary,
  Category,
  Estimate,
  Item,
  ItemForm,
  Maintenance,
  MaintenanceForm,
  Named,
  NewUser,
  PaymentMethod,
  PlanningTask,
  ReturnCount,
  PublicItem,
  RegisterForm,
  SimulatedSession,
  User,
} from './types'

// En developpement VITE_API_URL est vide : Vite transmet /api au backend (voir vite.config.ts)
export const API_URL: string = import.meta.env.VITE_API_URL ?? ''

const TOKEN_KEY = 'floreevent_token'

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

export const api = axios.create({ baseURL: `${API_URL}/api` })

api.interceptors.request.use((config) => {
  const token = tokenStore.get()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Jeton expire ou compte desactive : on deconnecte l'utilisateur (l'ecran de connexion s'affiche)
api.interceptors.response.use(undefined, (error: AxiosError) => {
  if (error.response?.status === 401 && tokenStore.get() && !error.config?.url?.includes('/auth/login')) {
    tokenStore.clear()
    window.dispatchEvent(new Event('auth:logout'))
  }
  return Promise.reject(error)
})

type ApiErrorBody = { message?: string; errors?: Record<string, string> }

/** Message lisible pour l'utilisateur, quelle que soit l'erreur. */
export function errorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiErrorBody>(error)) {
    if (!error.response) return 'Impossible de joindre le serveur. Vérifiez votre connexion Internet.'
    if (error.response.status === 403) return 'Vous n’avez pas le droit de faire cette action.'
    return error.response.data?.message ?? 'Une erreur est survenue. Réessayez.'
  }
  return 'Une erreur est survenue. Réessayez.'
}

/** Erreurs par champ renvoyees par la validation du serveur ({ "dayPriceCents": "..." }). */
export function fieldErrors(error: unknown): Record<string, string> {
  return axios.isAxiosError<ApiErrorBody>(error) ? (error.response?.data?.errors ?? {}) : {}
}

const data = <T>(promise: Promise<{ data: T }>) => promise.then((response) => response.data)

// Les champs vides des formulaires deviennent null pour l'API
const clean = (form: Record<string, unknown>) =>
  Object.fromEntries(Object.entries(form).map(([key, value]) => [key, value === '' ? null : value]))

export type Health = { status: string }

export const healthApi = {
  get: () => data(api.get<Health>('/health')),
}

export const authApi = {
  login: (email: string, password: string) => data(api.post<{ token: string; user: User }>('/auth/login', { email, password })),
  me: () => data(api.get<User>('/auth/me')),
  register: (form: RegisterForm) => data(api.post<{ token: string; user: User }>('/auth/register', clean(form))),
}

/** Espace personnel : informations, mot de passe et photo de chaque utilisateur. */
export const profileApi = {
  update: (changes: { firstName: string; lastName: string; phone: string }) => data(api.patch<User>('/me', changes)),
  changePassword: (currentPassword: string, newPassword: string) => api.post('/me/password', { currentPassword, newPassword }),
  setAvatar: (dataUrl: string) => data(api.put<User>('/me/avatar', { dataUrl })),
  removeAvatar: () => data(api.delete<User>('/me/avatar')),
}

/** Les images sont servies par l'API : en production, son adresse est differente de celle du site. */
export const imageUrl = (path?: string | null) => (path ? `${API_URL}${path}` : undefined)

export const usersApi = {
  list: () => data(api.get<User[]>('/users')),
  create: (user: NewUser) => data(api.post<User>('/users', user)),
  update: (id: number, changes: Partial<Pick<User, 'role' | 'active' | 'firstName' | 'lastName' | 'phone'>>) =>
    data(api.patch<User>(`/users/${id}`, changes)),
}

export const categoriesApi = {
  list: () => data(api.get<Category[]>('/categories')),
  create: (name: string) => data(api.post<Category>('/categories', { name })),
  rename: (id: number, name: string) => data(api.put<Category>(`/categories/${id}`, { name })),
  remove: (id: number) => api.delete(`/categories/${id}`),
}

export const itemsApi = {
  list: (filters: { q?: string; categoryId?: number | '' } = {}) => data(api.get<Item[]>('/items', { params: clean(filters) })),
  get: (id: number) => data(api.get<Item>(`/items/${id}`)),
  save: (form: ItemForm, id?: number) =>
    data(id ? api.put<Item>(`/items/${id}`, clean(form)) : api.post<Item>('/items', clean(form))),
  addImage: (id: number, dataUrl: string) => data(api.post<Item>(`/items/${id}/images`, { dataUrl })),
  makeMain: (id: number, imageId: string) => data(api.post<Item>(`/items/${id}/images/${imageId}/main`, {})),
  removeImage: (id: number, imageId: string) => data(api.delete<Item>(`/items/${id}/images/${imageId}`)),
  repairs: (id: number) => data(api.get<Maintenance[]>(`/items/${id}/maintenance`)),
  putInRepair: (id: number, form: MaintenanceForm) => data(api.post<Maintenance>(`/items/${id}/maintenance`, clean(form))),
}

export const maintenanceApi = {
  open: () => data(api.get<Maintenance[]>('/maintenance')),
  end: (id: number) => data(api.post<Maintenance>(`/maintenance/${id}/end`, {})),
}

export type Period = { from?: string; to?: string }

/** Catalogue public (sans connexion) : disponibilites quand les deux dates sont choisies. */
export const catalogApi = {
  items: (filters: { q?: string; categoryId?: number | '' } & Period = {}) =>
    data(api.get<PublicItem[]>('/catalog/items', { params: clean(filters) })),
  item: (id: number, period: Period = {}) => data(api.get<PublicItem>(`/catalog/items/${id}`, { params: clean(period) })),
  categories: () => data(api.get<Named[]>('/catalog/categories')),
}

export const quoteApi = {
  estimate: (request: BookingRequest) => data(api.post<Estimate>('/quote/estimate', clean(request))),
}

/** Demandes et reservations du client connecte. */
export const myBookingsApi = {
  request: (request: BookingRequest) => data(api.post<Booking>('/me/bookings', clean(request))),
  list: () => data(api.get<BookingSummary[]>('/me/bookings')),
  get: (id: number) => data(api.get<Booking>(`/me/bookings/${id}`)),
  /** Le client accepte le devis : adresse de la page de paiement de l'acompte. */
  checkout: (id: number) => data(api.post<{ url: string }>(`/me/bookings/${id}/checkout`, {})),
  cancellation: (id: number) => data(api.get<CancellationPreview>(`/me/bookings/${id}/cancellation`)),
  cancel: (id: number) => data(api.post<Booking>(`/me/bookings/${id}/cancel`, {})),
}

export type QuoteForm = { discountCents: number; message: string }
export type OfflinePayment = { method: PaymentMethod; amountCents: number; note: string }

/** Reservations traitees par l'equipe : devis, paiements hors ligne, annulation. */
export const bookingsApi = {
  list: () => data(api.get<BookingSummary[]>('/bookings')),
  get: (id: number) => data(api.get<Booking>(`/bookings/${id}`)),
  sendQuote: (id: number, form: QuoteForm) => data(api.post<Booking>(`/bookings/${id}/quote`, clean(form))),
  recordPayment: (id: number, payment: OfflinePayment) => data(api.post<Booking>(`/bookings/${id}/payments`, clean(payment))),
  cancellation: (id: number) => data(api.get<CancellationPreview>(`/bookings/${id}/cancellation`)),
  cancel: (id: number, reason: string) => data(api.post<Booking>(`/bookings/${id}/cancel`, { reason })),
  deliver: (id: number, securityDepositMethod: PaymentMethod) => data(api.post<Booking>(`/bookings/${id}/deliver`, { securityDepositMethod })),
  recordReturn: (id: number, lines: ReturnCount[], note: string) => data(api.post<Booking>(`/bookings/${id}/return`, { lines, note })),
  close: (id: number) => data(api.post<Booking>(`/bookings/${id}/close`, {})),
}

/** Planning des livraisons et des retours. */
export const planningApi = {
  list: (from: string, to: string) => data(api.get<PlanningTask[]>('/planning', { params: { from, to } })),
}

/** Paiement de demonstration (sans compte Stripe) : meme chemin que la confirmation de Stripe. */
export const simulatedPaymentsApi = {
  get: (session: string) => data(api.get<SimulatedSession>(`/simulated-payments/${session}`)),
  pay: (session: string) => data(api.post<SimulatedSession>(`/simulated-payments/${session}/pay`, {})),
}

/** Telecharge un document PDF protege par le jeton (devis, confirmation, facture). */
export async function downloadFile(path: string, filename: string) {
  const blob = await data(api.get<Blob>(path, { responseType: 'blob' }))
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 10_000)
}
