import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactNode } from 'react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'

/** Affiche une page avec le cache de donnees et le routeur, a l'adresse donnee. */
export function renderPage(ui: ReactNode, { path = '/', route = '/' }: { path?: string; route?: string } = {}) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[route]}>
        <Routes>
          <Route path={path} element={ui} />
          <Route path="*" element={<p>Autre page</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}
