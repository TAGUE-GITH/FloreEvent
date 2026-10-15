import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { vi } from 'vitest'
import { bookingsApi, downloadFile, myBookingsApi } from '../api/client'
import type { Booking, Role } from '../api/types'
import { useAuth } from '../auth/AuthProvider'
import BookingDetailPage from '../features/bookings/BookingDetailPage'
import ClientBookingPage from '../features/client/ClientBookingPage'
import { renderPage } from './render'

vi.mock('../api/client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/client')>()),
  bookingsApi: { list: vi.fn(), get: vi.fn(), sendQuote: vi.fn(), recordPayment: vi.fn(), cancellation: vi.fn(), cancel: vi.fn() },
  myBookingsApi: { request: vi.fn(), list: vi.fn(), get: vi.fn(), checkout: vi.fn(), cancellation: vi.fn(), cancel: vi.fn() },
  downloadFile: vi.fn(),
}))
vi.mock('../auth/AuthProvider', () => ({ useAuth: vi.fn() }))

const plain = (text: string | null) => (text ?? '').replace(/[  ]/g, ' ')

const loggedAs = (role: Role) =>
  vi.mocked(useAuth).mockReturnValue({
    user: { id: 1, email: 'x@floreevent.test', firstName: 'Claire', lastName: 'Martin', fullName: 'Claire Martin', phone: null, role, active: true, createdAt: '' },
    loading: false,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    setUser: vi.fn(),
    hasRole: (...roles: Role[]) => roles.includes(role),
    isStaff: role !== 'CLIENT',
  })

/** 2 barnums sur 3 jours, retrait : 600 €, acompte 180 €. */
const booking = (changes: Partial<Booking> = {}): Booking => ({
  id: 9,
  reference: 'RES-2026-00009',
  status: 'DEMANDE',
  statusLabel: 'Demande envoyée',
  customer: { id: 2, fullName: 'Léa Moreau', phone: '06 12 34 56 78', email: 'lea@client.test' },
  startDate: '2026-11-14',
  endDate: '2026-11-16',
  days: 3,
  deliveryMode: 'RETRAIT',
  installation: false,
  lines: [{ itemId: 1, itemName: 'Barnum 3 x 6 m', quantity: 2, dayPriceCents: 15000, lineTotalCents: 60000 }],
  rentalCents: 60000,
  setupCents: 0,
  deliveryCents: 0,
  discountCents: 0,
  totalCents: 60000,
  vatCents: 10000,
  depositCents: 18000,
  securityDepositCents: 72000,
  quoteExpired: false,
  amountDueCents: 60000,
  paidCents: 0,
  balanceCents: 60000,
  payments: [],
  invoices: [],
  createdAt: '2026-10-15T10:00:00Z',
  events: [{ status: 'DEMANDE', label: 'Demande envoyée', createdAt: '2026-10-15T10:00:00Z' }],
  ...changes,
})

const confirmed = () =>
  booking({
    status: 'CONFIRMEE',
    statusLabel: 'Confirmée',
    quoteValidUntil: '2026-10-22',
    paidCents: 18000,
    balanceCents: 42000,
    payments: [{ id: 1, method: 'CARTE', methodLabel: 'Carte bancaire', status: 'PAYE', statusLabel: 'Payé', amountCents: 18000, refundedCents: 0, createdAt: '2026-10-15T11:00:00Z', paidAt: '2026-10-15T11:00:00Z' }],
    invoices: [{ id: 4, number: 'FAC-2026-00001', kind: 'ACOMPTE', kindLabel: 'Facture d’acompte', amountCents: 18000, issuedAt: '2026-10-15T11:00:00Z' }],
  })

beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(downloadFile).mockResolvedValue()
})

describe('Traitement des demandes par l’équipe (US-09, US-12)', () => {
  it('le commercial envoie le devis avec une remise et un message', async () => {
    loggedAs('COMMERCIAL')
    vi.mocked(bookingsApi.get).mockResolvedValue(booking())
    vi.mocked(bookingsApi.sendQuote).mockResolvedValue(booking({ status: 'DEVIS', statusLabel: 'Devis envoyé', quoteValidUntil: '2026-10-22' }))
    renderPage(<BookingDetailPage />, { path: '/gestion/reservations/:id', route: '/gestion/reservations/9' })

    await userEvent.type(await screen.findByLabelText('Remise (€)'), '50')
    expect(plain(screen.getByText(/Nouveau total/).textContent)).toBe('Nouveau total : 550,00 € TTC')
    await userEvent.type(screen.getByLabelText('Message au client (facultatif)'), 'Montage possible le vendredi')
    await userEvent.click(screen.getByRole('button', { name: 'Envoyer le devis' }))

    expect(bookingsApi.sendQuote).toHaveBeenCalledWith(9, { discountCents: 5000, message: 'Montage possible le vendredi' })
    expect(await screen.findByText('Devis envoyé au client.')).toBeInTheDocument()
  })

  it('la logistique ne fait pas de devis mais encaisse un paiement hors ligne', async () => {
    loggedAs('LOGISTIQUE')
    vi.mocked(bookingsApi.get).mockResolvedValue(confirmed())
    vi.mocked(bookingsApi.recordPayment).mockResolvedValue(confirmed())
    renderPage(<BookingDetailPage />, { path: '/gestion/reservations/:id', route: '/gestion/reservations/9' })

    const amount = await screen.findByLabelText('Montant (€)')
    expect(screen.queryByRole('heading', { name: 'Modifier le devis' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Annuler la réservation' })).not.toBeInTheDocument()
    expect(amount).toHaveValue('420')
    await userEvent.selectOptions(screen.getByLabelText('Moyen de paiement'), 'ESPECES')
    await userEvent.click(screen.getByRole('button', { name: 'Enregistrer le paiement' }))

    expect(bookingsApi.recordPayment).toHaveBeenCalledWith(9, { method: 'ESPECES', amountCents: 42000, note: '' })
  })
})

describe('Espace client : devis, paiement, documents, annulation (US-10, US-11, US-13)', () => {
  it('le client accepte le devis et part sur la page de paiement', async () => {
    loggedAs('CLIENT')
    const assign = vi.fn()
    vi.stubGlobal('location', { ...window.location, assign })
    vi.mocked(myBookingsApi.get).mockResolvedValue(
      booking({ status: 'DEVIS', statusLabel: 'Devis envoyé', quoteValidUntil: '2026-10-22', quoteMessage: 'Montage possible le vendredi' }),
    )
    vi.mocked(myBookingsApi.checkout).mockResolvedValue({ url: 'https://checkout.stripe.com/c/pay/cs_test_1' })
    renderPage(<ClientBookingPage />, { path: '/mon-espace/reservations/:id', route: '/mon-espace/reservations/9' })

    expect(await screen.findByText('Montage possible le vendredi')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: /Accepter le devis et payer l’acompte/ }))

    expect(myBookingsApi.checkout).toHaveBeenCalledWith(9)
    expect(assign).toHaveBeenCalledWith('https://checkout.stripe.com/c/pay/cs_test_1')
    vi.unstubAllGlobals()
  })

  it('au retour du paiement, la confirmation et la facture sont affichees', async () => {
    loggedAs('CLIENT')
    vi.mocked(myBookingsApi.get).mockResolvedValue(confirmed())
    renderPage(<ClientBookingPage />, { path: '/mon-espace/reservations/:id', route: '/mon-espace/reservations/9?paiement=ok' })

    expect(await screen.findByText(/votre réservation est confirmée/)).toBeInTheDocument()
    expect(plain(screen.getByText('Reste à payer').parentElement!.textContent)).toContain('420,00 €')
    await userEvent.click(screen.getByRole('button', { name: 'Facture d’acompte FAC-2026-00001 (PDF)' }))
    expect(downloadFile).toHaveBeenCalledWith('/me/bookings/9/invoices/4', 'FAC-2026-00001.pdf')
    expect(screen.getByRole('button', { name: 'Devis (PDF)' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Confirmation de réservation (PDF)' })).toBeInTheDocument()
  })

  it('avant d’annuler une réservation confirmée, le client voit ce qu’il perd', async () => {
    loggedAs('CLIENT')
    vi.mocked(myBookingsApi.get).mockResolvedValue(confirmed())
    vi.mocked(myBookingsApi.cancellation).mockResolvedValue({
      retainedCents: 18000,
      refundCents: 0,
      dueCents: 0,
      rule: 'Entre 30 et 8 jours avant l’événement : l’acompte est conservé.',
    })
    vi.mocked(myBookingsApi.cancel).mockResolvedValue(booking({ status: 'ANNULEE', statusLabel: 'Annulée', cancellationFeeCents: 18000 }))
    renderPage(<ClientBookingPage />, { path: '/mon-espace/reservations/:id', route: '/mon-espace/reservations/9' })

    await userEvent.click(await screen.findByRole('button', { name: 'Annuler ma réservation' }))
    expect(await screen.findByText('Entre 30 et 8 jours avant l’événement : l’acompte est conservé.')).toBeInTheDocument()
    expect(plain(screen.getByText(/FloreEvent conserve/).textContent)).toBe('FloreEvent conserve 180,00 €.')
    await userEvent.click(screen.getByRole('button', { name: 'Confirmer l’annulation' }))

    expect(myBookingsApi.cancel).toHaveBeenCalledWith(9)
    expect(await within(document.body).findByText('Annulée')).toBeInTheDocument()
  })
})
