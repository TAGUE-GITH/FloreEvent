import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { vi } from 'vitest'
import { categoriesApi, itemsApi, maintenanceApi } from '../api/client'
import type { Item, Role } from '../api/types'
import { useAuth } from '../auth/AuthProvider'
import Layout from '../components/Layout'
import ItemFormPage from '../features/catalog/ItemFormPage'
import ItemsPage from '../features/catalog/ItemsPage'
import MaintenancePage from '../features/catalog/MaintenancePage'
import { renderPage } from './render'

vi.mock('../api/client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/client')>()),
  categoriesApi: { list: vi.fn(), create: vi.fn(), rename: vi.fn(), remove: vi.fn() },
  itemsApi: { list: vi.fn(), get: vi.fn(), save: vi.fn(), addImage: vi.fn(), makeMain: vi.fn(), removeImage: vi.fn(), repairs: vi.fn(), putInRepair: vi.fn() },
  maintenanceApi: { open: vi.fn(), end: vi.fn() },
}))
vi.mock('../auth/AuthProvider', () => ({ useAuth: vi.fn() }))

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

const barnum: Item = {
  id: 7,
  reference: 'BARNUM-3X6',
  name: 'Barnum 3 x 6 m',
  category: { id: 1, name: 'Tentes et barnums' },
  dayPriceCents: 15000,
  replacementValueCents: 120000,
  setupPriceCents: 8000,
  quantity: 4,
  inRepair: 1,
  active: true,
  images: [],
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(categoriesApi.list).mockResolvedValue([{ id: 1, name: 'Tentes et barnums', itemCount: 1 }])
})

describe('Menu selon le role (US-02)', () => {
  const menu = () => within(screen.getAllByRole('navigation', { name: 'Menu principal' })[0]).getAllByRole('link').map((l) => l.textContent)

  it('montre tout au gerant', () => {
    loggedAs('GERANT')
    renderPage(<Layout />)
    expect(menu()).toEqual(['Accueil', 'Réservations', 'Catalogue', 'Matériel en réparation', 'Catégories', 'Équipe', 'Voir le site'])
  })

  it('cache la gestion du catalogue et de l equipe a la logistique', () => {
    loggedAs('LOGISTIQUE')
    renderPage(<Layout />)
    expect(menu()).toEqual(['Accueil', 'Réservations', 'Catalogue', 'Matériel en réparation', 'Voir le site'])
  })
})

describe('Catalogue (US-03)', () => {
  it('affiche les articles avec leur prix par jour et le materiel en reparation', async () => {
    loggedAs('COMMERCIAL')
    vi.mocked(itemsApi.list).mockResolvedValue([barnum])
    renderPage(<ItemsPage />)

    expect(await screen.findByText('Barnum 3 x 6 m')).toBeInTheDocument()
    expect(screen.getByText(/150,00.€ \/ jour/)).toBeInTheDocument()
    expect(screen.getByText('3 / 4 disponible(s)')).toBeInTheDocument()
    expect(screen.getByText(/1 en réparation/)).toBeInTheDocument()
    // Seul le gerant peut ajouter un article
    expect(screen.queryByRole('link', { name: /Nouvel article/ })).not.toBeInTheDocument()
  })

  it('le gerant saisit les prix en euros : l API recoit des centimes', async () => {
    loggedAs('GERANT')
    vi.mocked(itemsApi.save).mockResolvedValue(barnum)
    renderPage(<ItemFormPage />, { path: '/gestion/catalogue/nouveau', route: '/gestion/catalogue/nouveau' })

    await userEvent.type(screen.getByLabelText('Référence'), 'BARNUM-3X6')
    await userEvent.type(screen.getByLabelText('Nom'), 'Barnum 3 x 6 m')
    await userEvent.selectOptions(await screen.findByLabelText('Catégorie'), '1')
    await userEvent.type(screen.getByLabelText('Prix par jour (€)'), '150')
    await userEvent.type(screen.getByLabelText('Valeur de remplacement (€)'), '1 200')
    await userEvent.type(screen.getByLabelText('Installation (€)'), '80,50')
    await userEvent.type(screen.getByLabelText('Quantité en stock'), '4')
    await userEvent.click(screen.getByRole('button', { name: 'Ajouter au catalogue' }))

    expect(itemsApi.save).toHaveBeenCalledWith(
      expect.objectContaining({ reference: 'BARNUM-3X6', categoryId: 1, dayPriceCents: 15000, replacementValueCents: 120000, setupPriceCents: 8050, quantity: 4 }),
      undefined,
    )
  })

  it('refuse un montant mal ecrit avant meme de l envoyer', async () => {
    loggedAs('GERANT')
    renderPage(<ItemFormPage />, { path: '/gestion/catalogue/nouveau', route: '/gestion/catalogue/nouveau' })

    await userEvent.type(screen.getByLabelText('Prix par jour (€)'), 'cent euros')
    await userEvent.click(screen.getByRole('button', { name: 'Ajouter au catalogue' }))

    expect(screen.getAllByText('Montant invalide (exemple : 12,50).').length).toBeGreaterThan(0)
    expect(itemsApi.save).not.toHaveBeenCalled()
  })
})

describe('Materiel en reparation (US-04)', () => {
  it('la logistique signale un article repare', async () => {
    loggedAs('LOGISTIQUE')
    vi.mocked(maintenanceApi.open).mockResolvedValue([
      { id: 3, itemId: 7, itemName: 'Barnum 3 x 6 m', quantity: 1, fromDate: '2026-10-10', reason: 'Bâche déchirée', author: 'Paul', createdAt: '' },
    ])
    vi.mocked(maintenanceApi.end).mockResolvedValue({} as never)
    renderPage(<MaintenancePage />)

    expect(await screen.findByText('1 × Barnum 3 x 6 m')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Réparé' }))
    expect(maintenanceApi.end).toHaveBeenCalledWith(3, expect.anything())
  })
})
