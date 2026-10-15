import { useQuery } from '@tanstack/react-query'
import { CalendarCheck, CreditCard, Flower2, LayoutDashboard, Package, Truck } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link, Route, Routes } from 'react-router-dom'
import { healthApi } from './api/client'
import { STAFF_ROLES } from './api/types'
import type { Role } from './api/types'
import { RequireAuth } from './auth/RequireAuth'
import { buttonClasses } from './components/buttonClasses'
import Layout from './components/Layout'
import StaffLoginPage from './features/auth/StaffLoginPage'
import CategoriesPage from './features/catalog/CategoriesPage'
import ItemDetailPage from './features/catalog/ItemDetailPage'
import ItemFormPage from './features/catalog/ItemFormPage'
import ItemsPage from './features/catalog/ItemsPage'
import MaintenancePage from './features/catalog/MaintenancePage'
import StaffHomePage from './features/home/StaffHomePage'
import ProfilePage from './features/profile/ProfilePage'
import UsersPage from './features/users/UsersPage'

type Module = { title: string; text: string; sprint: number; Icon: LucideIcon }

// Les modules de FloreEvent, livres sprint apres sprint (voir docs/02-user-stories.md)
const MODULES: Module[] = [
  { title: 'Catalogue et stock', text: 'Matériel, photos, tarifs, quantités et matériel en réparation', sprint: 1, Icon: Package },
  { title: 'Disponibilités et devis', text: 'Disponibilité par dates, demande de devis calculée en ligne', sprint: 2, Icon: CalendarCheck },
  { title: 'Réservations et paiement', text: 'Acompte par carte, devis et factures en PDF', sprint: 3, Icon: CreditCard },
  { title: 'Livraisons et retours', text: 'Planning, bons de livraison, état des lieux et caution', sprint: 4, Icon: Truck },
  { title: 'Services et tableau de bord', text: 'Décoration florale, emails, chiffre d’affaires et occupation', sprint: 5, Icon: LayoutDashboard },
]

function ApiStatus() {
  const { isPending, isError } = useQuery({ queryKey: ['health'], queryFn: healthApi.get })
  const [label, color] = isPending
    ? ['Connexion au serveur…', 'bg-slate-300']
    : isError
      ? ['Serveur injoignable', 'bg-red-500']
      : ['Serveur connecté', 'bg-emerald-500']
  return (
    <p className="flex items-center gap-2 text-sm text-slate-600" role="status">
      <span className={`size-2.5 rounded-full ${color}`} aria-hidden />
      {label}
    </p>
  )
}

function HomePage() {
  return (
    <main className="mx-auto max-w-5xl px-4 py-10 sm:px-6">
      <h1 className="text-3xl font-extrabold tracking-tight sm:text-4xl">Location événementielle à Melun</h1>
      <p className="mt-3 max-w-2xl text-slate-600">
        Matériel, services, disponibilités, devis, réservations et livraisons de FloreEvent, dans un seul outil, sur ordinateur et sur
        téléphone.
      </p>
      <div className="mt-4 flex flex-wrap items-center gap-4">
        <ApiStatus />
        <Link to="/espace-equipe" className={buttonClasses('primary')}>
          Espace équipe
        </Link>
      </div>
      <ul className="mt-8 grid gap-4 sm:grid-cols-2">
        {MODULES.map(({ title, text, sprint, Icon }) => (
          <li key={title} className="flex gap-4 rounded-2xl bg-white p-5 ring-1 ring-stone-200">
            <span className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-700">
              <Icon className="size-5" aria-hidden />
            </span>
            <div>
              <h2 className="font-bold">{title}</h2>
              <p className="text-sm text-slate-600">{text}</p>
              <p className="mt-2 text-xs font-semibold text-brand-700 uppercase">Sprint {sprint}</p>
            </div>
          </li>
        ))}
      </ul>
    </main>
  )
}

function NotFoundPage() {
  return (
    <main className="mx-auto max-w-xl px-4 py-20 text-center">
      <h1 className="text-2xl font-bold">Page introuvable</h1>
      <Link to="/" className="mt-4 inline-block font-semibold text-brand-700">
        Retour à l’accueil
      </Link>
    </main>
  )
}

const only = (roles: Role[], page: ReactNode) => <RequireAuth roles={roles}>{page}</RequireAuth>

function PublicShell({ children }: { children: ReactNode }) {
  return (
    <>
      <header className="border-b border-stone-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center gap-3 px-4 py-3 sm:px-6">
          <span className="flex size-9 items-center justify-center rounded-xl bg-brand-700 text-white">
            <Flower2 className="size-5" aria-hidden />
          </span>
          <Link to="/" className="text-lg font-extrabold tracking-tight">
            Flore<span className="text-brand-700">Event</span>
          </Link>
        </div>
      </header>
      {children}
    </>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<PublicShell><HomePage /></PublicShell>} />
      <Route path="/espace-equipe" element={<StaffLoginPage />} />

      {/* Espace de gestion : reserve a l'equipe de FloreEvent */}
      <Route path="/gestion" element={only(STAFF_ROLES, <Layout />)}>
        <Route index element={<StaffHomePage />} />
        <Route path="catalogue" element={<ItemsPage />} />
        <Route path="catalogue/nouveau" element={only(['GERANT'], <ItemFormPage />)} />
        <Route path="catalogue/:id" element={<ItemDetailPage />} />
        <Route path="catalogue/:id/modifier" element={only(['GERANT'], <ItemFormPage />)} />
        <Route path="reparations" element={<MaintenancePage />} />
        <Route path="categories" element={only(['GERANT'], <CategoriesPage />)} />
        <Route path="equipe" element={only(['GERANT'], <UsersPage />)} />
        <Route path="profil" element={<ProfilePage />} />
      </Route>

      <Route path="*" element={<PublicShell><NotFoundPage /></PublicShell>} />
    </Routes>
  )
}
