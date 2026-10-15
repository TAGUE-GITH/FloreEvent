import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { vi } from 'vitest'
import { authApi, catalogApi, myBookingsApi, quoteApi } from '../api/client'
import type { Booking, Estimate, User } from '../api/types'
import { AuthProvider } from '../auth/AuthProvider'
import CatalogPage from '../features/public/CatalogPage'
import RegisterPage from '../features/public/RegisterPage'
import QuotePage from '../features/quote/QuotePage'
import { SelectionProvider } from '../features/quote/Selection'

vi.mock('../api/client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/client')>()),
  authApi: { login: vi.fn(), me: vi.fn(), register: vi.fn() },
  catalogApi: { items: vi.fn(), item: vi.fn(), categories: vi.fn(), prestations: vi.fn() },
  quoteApi: { estimate: vi.fn() },
  myBookingsApi: { request: vi.fn(), list: vi.fn(), get: vi.fn(), cancel: vi.fn() },
}))

const plain = (text: string) => text.replace(/[  ]/g, ' ')

const lea: User = {
  id: 3,
  email: 'lea@client.test',
  firstName: 'Léa',
  lastName: 'Moreau',
  fullName: 'Léa Moreau',
  phone: '0612345678',
  role: 'CLIENT',
  active: true,
  createdAt: '2026-10-15T10:00:00Z',
  customerId: 2,
}

const renderSite = (path: string) =>
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })}>
      <AuthProvider>
        <SelectionProvider>
          <MemoryRouter initialEntries={[path]}>
            <Routes>
              <Route path="/catalogue" element={<CatalogPage />} />
              <Route path="/devis" element={<QuotePage />} />
              <Route path="/inscription" element={<RegisterPage />} />
              <Route path="/mon-espace" element={<p>Mon espace</p>} />
              <Route path="/mon-espace/reservations/:id" element={<p>Suivi de la demande</p>} />
            </Routes>
          </MemoryRouter>
        </SelectionProvider>
      </AuthProvider>
    </QueryClientProvider>,
  )

/** Selection deja composee (gardee dans le navigateur). */
const storeSelection = () =>
  localStorage.setItem(
    'floreevent_selection',
    JSON.stringify({
      startDate: '2026-11-14',
      endDate: '2026-11-16',
      deliveryMode: 'RETRAIT',
      address: '',
      installation: false,
      lines: [{ itemId: 1, name: 'Barnum 3 x 6 m', dayPriceCents: 15000, setupPriceCents: 8000, quantity: 2 }],
    }),
  )

const estimate: Estimate = {
  startDate: '2026-11-14',
  endDate: '2026-11-16',
  days: 3,
  deliveryMode: 'RETRAIT',
  installation: false,
  lines: [{ itemId: 1, name: 'Barnum 3 x 6 m', quantity: 2, dayPriceCents: 15000, lineTotalCents: 60000, available: 4 }],
  prestations: [],
  rentalCents: 60000,
  setupCents: 0,
  deliveryCents: 0,
  prestationsCents: 0,
  totalCents: 60000,
  vatCents: 10000,
  depositCents: 18000,
  securityDepositCents: 72000,
  bookable: true,
  problems: [],
}

beforeEach(() => {
  localStorage.clear()
  vi.clearAllMocks()
  vi.mocked(catalogApi.categories).mockResolvedValue([])
  vi.mocked(quoteApi.estimate).mockResolvedValue(estimate)
  vi.mocked(catalogApi.prestations).mockResolvedValue([])
})

describe('Catalogue public (US-05)', () => {
  it('affiche la disponibilite a vos dates et ajoute au devis', async () => {
    storeSelection()
    vi.mocked(catalogApi.items).mockResolvedValue([
      { id: 2, name: 'Chaise Napoléon', category: { id: 1, name: 'Mobilier' }, dayPriceCents: 350, setupPriceCents: 0, images: [], available: 0 },
      { id: 3, name: 'Table ronde', category: { id: 1, name: 'Mobilier' }, dayPriceCents: 1200, setupPriceCents: 0, images: [], available: 12 },
    ])
    renderSite('/catalogue')

    expect(await screen.findByText('Indisponible à vos dates')).toBeInTheDocument()
    expect(screen.getByText('12 disponible(s) à vos dates')).toBeInTheDocument()
    expect(catalogApi.items).toHaveBeenCalledWith(expect.objectContaining({ from: '2026-11-14', to: '2026-11-16' }))
    expect(screen.getByRole('button', { name: 'Ajouter au devis : Chaise Napoléon' })).toBeDisabled()
    await userEvent.click(screen.getByRole('button', { name: 'Ajouter au devis : Table ronde' }))
    expect(screen.getByRole('button', { name: 'Ajouter au devis : Table ronde' })).toHaveTextContent('Au devis (1)')
  })
})

describe('Devis en ligne (US-07)', () => {
  it('calcule le prix et invite le visiteur a se connecter', async () => {
    storeSelection()
    renderSite('/devis')

    const total = await screen.findByText('Total TTC')
    expect(plain(total.parentElement!.textContent!)).toContain('600,00 €')
    expect(plain(screen.getByText('dont TVA 20 % : 100,00 €').textContent!)).toBe('dont TVA 20 % : 100,00 €')
    expect(plain(screen.getByText('Acompte à la réservation (30 %)').parentElement!.textContent!)).toContain('180,00 €')
    expect(screen.getByRole('link', { name: 'Se connecter' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Envoyer ma demande de devis' })).not.toBeInTheDocument()
  })

  it('le client envoie sa demande, puis la suit dans son espace', async () => {
    storeSelection()
    localStorage.setItem('floreevent_token', 'jeton')
    vi.mocked(authApi.me).mockResolvedValue(lea)
    vi.mocked(myBookingsApi.request).mockResolvedValue({ id: 9 } as Booking)
    renderSite('/devis')

    await userEvent.type(screen.getByLabelText('Votre événement (facultatif)'), 'Mariage')
    await userEvent.click(await screen.findByRole('button', { name: 'Envoyer ma demande de devis' }))

    expect(myBookingsApi.request).toHaveBeenCalledWith({
      startDate: '2026-11-14',
      endDate: '2026-11-16',
      deliveryMode: 'RETRAIT',
      address: '',
      installation: false,
      note: 'Mariage',
      lines: [{ itemId: 1, quantity: 2 }],
      prestationIds: [],
    })
    expect(await screen.findByText('Suivi de la demande')).toBeInTheDocument()
    expect(JSON.parse(localStorage.getItem('floreevent_selection')!).lines).toEqual([])
  })

  it('affiche sous le champ adresse le refus du serveur', async () => {
    storeSelection()
    vi.mocked(quoteApi.estimate).mockRejectedValue({
      isAxiosError: true,
      response: { status: 400, data: { message: 'Certaines informations sont invalides.', errors: { address: 'Au-delà de 50 km, contactez-nous pour un devis personnalisé.' } } },
    })
    renderSite('/devis')

    await userEvent.click(screen.getByText('Livraison et reprise'))
    await userEvent.type(screen.getByLabelText('Adresse de l’événement'), 'Place du Capitole Toulouse')

    expect(await screen.findByText('Au-delà de 50 km, contactez-nous pour un devis personnalisé.', {}, { timeout: 3000 })).toBeInTheDocument()
  })
})

describe('Compte client (US-06)', () => {
  it('cree le compte apres acceptation des conditions', async () => {
    vi.mocked(authApi.register).mockResolvedValue({ token: 'jeton', user: lea })
    renderSite('/inscription')

    await userEvent.type(screen.getByLabelText('Prénom'), 'Léa')
    await userEvent.type(screen.getByLabelText('Nom'), 'Moreau')
    await userEvent.type(screen.getByLabelText('Email'), 'lea@client.test')
    await userEvent.type(screen.getByLabelText('Téléphone'), '0612345678')
    await userEvent.type(screen.getByLabelText('Mot de passe'), 'motdepasse-solide')
    await userEvent.click(screen.getByRole('checkbox', { name: /J’ai lu et j’accepte les conditions générales/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Créer mon compte' }))

    expect(authApi.register).toHaveBeenCalledWith(expect.objectContaining({ email: 'lea@client.test', acceptTerms: true, company: '' }))
    expect(await screen.findByText('Mon espace')).toBeInTheDocument()
  })
})
