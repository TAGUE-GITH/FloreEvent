import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { vi } from 'vitest'
import { contactApi, dashboardApi, demoApi } from '../api/client'
import type { Role } from '../api/types'
import { useAuth } from '../auth/AuthProvider'
import StaffHomePage from '../features/home/StaffHomePage'
import ClientLoginPage from '../features/public/ClientLoginPage'
import ContactPage from '../features/public/ContactPage'
import { renderPage } from './render'

vi.mock('../api/client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/client')>()),
  contactApi: { send: vi.fn(), list: vi.fn(), handled: vi.fn() },
  dashboardApi: { get: vi.fn() },
  demoApi: { info: vi.fn() },
}))
vi.mock('../auth/AuthProvider', () => ({ useAuth: vi.fn(), homeOf: () => '/mon-espace' }))

const plain = (text: string | null) => (text ?? '').replace(/[  ]/g, ' ')

const loggedAs = (role: Role | null) =>
  vi.mocked(useAuth).mockReturnValue({
    user: role
      ? { id: 1, email: 'x@floreevent.test', firstName: 'Claire', lastName: 'Martin', fullName: 'Claire Martin', phone: null, role, active: true, createdAt: '' }
      : null,
    loading: false,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    setUser: vi.fn(),
    hasRole: (...roles: Role[]) => (role ? roles.includes(role) : false),
    isStaff: role !== null && role !== 'CLIENT',
  })

beforeEach(() => vi.clearAllMocks())

describe('Vitrine et démonstration (US-20 à US-23)', () => {
  it('un visiteur écrit à l’équipe depuis la page contact', async () => {
    loggedAs(null)
    vi.mocked(contactApi.send).mockResolvedValue({ id: 1, name: 'Élodie', email: 'e@x.fr', message: 'Bonjour', handled: false, createdAt: '' })
    renderPage(<ContactPage />)

    await userEvent.type(screen.getByLabelText('Nom'), 'Élodie Garnier')
    await userEvent.type(screen.getByLabelText('Email'), 'elodie@exemple.fr')
    await userEvent.type(screen.getByLabelText('Votre message'), 'Mariage de 120 personnes')
    await userEvent.click(screen.getByRole('button', { name: 'Envoyer' }))

    expect(contactApi.send).toHaveBeenCalledWith({ name: 'Élodie Garnier', email: 'elodie@exemple.fr', phone: '', eventDate: '', message: 'Mariage de 120 personnes' })
    expect(await screen.findByText(/Votre message est bien envoyé/)).toBeInTheDocument()
  })

  it('sur la démonstration, la connexion client propose seulement le compte client', async () => {
    loggedAs(null)
    vi.mocked(demoApi.info).mockResolvedValue({
      enabled: true,
      password: 'demo-floreevent-2026',
      accounts: [
        { email: 'gerant@demo.floreevent.fr', role: 'GERANT', name: 'Claire Martin' },
        { email: 'client@demo.floreevent.fr', role: 'CLIENT', name: 'Sophie Laurent' },
      ],
    })
    renderPage(<ClientLoginPage />)

    await userEvent.click(await screen.findByRole('button', { name: /Sophie Laurent/ }))
    expect(screen.queryByText('Claire Martin')).not.toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toHaveValue('client@demo.floreevent.fr')
    expect(screen.getByLabelText('Mot de passe')).toHaveValue('demo-floreevent-2026')
  })

  it('le tableau de bord résume l’activité', async () => {
    loggedAs('GERANT')
    vi.mocked(dashboardApi.get).mockResolvedValue({
      revenueThisMonthCents: 125000,
      revenueByMonth: [{ month: '2026-10', label: 'oct.', cents: 125000 }],
      pendingRequests: 2,
      quotesAwaiting: 1,
      upcomingBookings: 3,
      depositsHeld: 1,
      depositsHeldCents: 57000,
      balanceDueCents: 42000,
      unhandledMessages: 2,
      topItems: [{ name: 'Chaise Napoléon blanche', quantity: 230 }],
      nextBookings: [],
    })
    renderPage(<StaffHomePage />)

    expect(plain((await screen.findByText('Encaissé ce mois-ci')).parentElement!.textContent)).toContain('1 250,00 €')
    expect(screen.getByText('Demandes à traiter').parentElement).toHaveTextContent('2')
    expect(screen.getByText('Chaise Napoléon blanche')).toBeInTheDocument()
    expect(screen.getByText('Messages du site : 2 à traiter')).toBeInTheDocument()
  })
})
