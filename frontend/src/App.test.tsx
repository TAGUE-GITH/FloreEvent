import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { vi } from 'vitest'
import App from './App'
import { healthApi } from './api/client'

vi.mock('./api/client', () => ({ healthApi: { get: vi.fn() } }))

const renderAt = (path: string) =>
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter initialEntries={[path]}>
        <App />
      </MemoryRouter>
    </QueryClientProvider>,
  )

describe('Accueil du projet', () => {
  it('presente les modules prevus et indique que le serveur repond', async () => {
    vi.mocked(healthApi.get).mockResolvedValue({ status: 'ok' })
    renderAt('/')

    expect(screen.getByRole('heading', { level: 1, name: /Location événementielle à Melun/ })).toBeInTheDocument()
    for (const module of ['Catalogue et stock', 'Disponibilités et devis', 'Réservations et paiement', 'Livraisons et retours'])
      expect(screen.getByRole('heading', { name: module })).toBeInTheDocument()
    expect(await screen.findByText('Serveur connecté')).toBeInTheDocument()
  })

  it('previent quand le serveur est injoignable', async () => {
    vi.mocked(healthApi.get).mockRejectedValue(new Error('reseau'))
    renderAt('/')

    expect(await screen.findByText('Serveur injoignable')).toBeInTheDocument()
  })

  it('affiche une page introuvable pour une adresse inconnue', () => {
    vi.mocked(healthApi.get).mockResolvedValue({ status: 'ok' })
    renderAt('/nulle-part')

    expect(screen.getByRole('heading', { name: 'Page introuvable' })).toBeInTheDocument()
  })
})
