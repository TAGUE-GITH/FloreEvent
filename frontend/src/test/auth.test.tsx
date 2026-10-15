import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { vi } from 'vitest'
import { authApi } from '../api/client'
import type { User } from '../api/types'
import { AuthProvider } from '../auth/AuthProvider'
import { RequireAuth } from '../auth/RequireAuth'
import StaffLoginPage from '../features/auth/StaffLoginPage'

vi.mock('../api/client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/client')>()),
  authApi: { login: vi.fn(), me: vi.fn() },
}))

const claire: User = {
  id: 1,
  email: 'claire@floreevent.test',
  firstName: 'Claire',
  lastName: 'Martin',
  fullName: 'Claire Martin',
  phone: null,
  role: 'COMMERCIAL',
  active: true,
  createdAt: '2026-10-15T10:00:00Z',
}

const renderAt = (path: string) =>
  render(
    <QueryClientProvider client={new QueryClient()}>
      <AuthProvider>
        <MemoryRouter initialEntries={[path]}>
          <Routes>
            <Route path="/espace-equipe" element={<StaffLoginPage />} />
            <Route path="/gestion" element={<RequireAuth roles={['GERANT', 'COMMERCIAL', 'LOGISTIQUE']}><p>Accueil de la gestion</p></RequireAuth>} />
          </Routes>
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  )

describe('Connexion de l equipe (US-01)', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('connecte un membre de l equipe, garde son jeton et ouvre la gestion', async () => {
    vi.mocked(authApi.login).mockResolvedValue({ token: 'jeton', user: claire })
    renderAt('/espace-equipe')

    await userEvent.type(screen.getByLabelText('Email'), 'claire@floreevent.test')
    await userEvent.type(screen.getByLabelText('Mot de passe'), 'motdepasse-solide')
    await userEvent.click(screen.getByRole('button', { name: 'Se connecter' }))

    expect(authApi.login).toHaveBeenCalledWith('claire@floreevent.test', 'motdepasse-solide')
    expect(await screen.findByText('Accueil de la gestion')).toBeInTheDocument()
    expect(localStorage.getItem('floreevent_token')).toBe('jeton')
  })

  it('affiche le message du serveur si les identifiants sont faux', async () => {
    vi.mocked(authApi.login).mockRejectedValue({ isAxiosError: true, response: { status: 401, data: { message: 'Email ou mot de passe incorrect.' } } })
    renderAt('/espace-equipe')

    await userEvent.type(screen.getByLabelText('Email'), 'claire@floreevent.test')
    await userEvent.type(screen.getByLabelText('Mot de passe'), 'faux')
    await userEvent.click(screen.getByRole('button', { name: 'Se connecter' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Email ou mot de passe incorrect.')
    expect(localStorage.getItem('floreevent_token')).toBeNull()
  })

  it('renvoie vers la connexion de l equipe une page de gestion ouverte sans etre connecte', async () => {
    renderAt('/gestion')
    expect(await screen.findByRole('heading', { name: 'Espace équipe' })).toBeInTheDocument()
  })
})
