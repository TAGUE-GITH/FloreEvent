import type { ReactNode } from 'react'
import { Link, Route, Routes } from 'react-router-dom'
import { STAFF_ROLES } from './api/types'
import type { Role } from './api/types'
import { RequireAuth } from './auth/RequireAuth'
import Layout from './components/Layout'
import StaffLoginPage from './features/auth/StaffLoginPage'
import BookingDetailPage from './features/bookings/BookingDetailPage'
import BookingsPage from './features/bookings/BookingsPage'
import CategoriesPage from './features/catalog/CategoriesPage'
import ItemDetailPage from './features/catalog/ItemDetailPage'
import ItemFormPage from './features/catalog/ItemFormPage'
import ItemsPage from './features/catalog/ItemsPage'
import MaintenancePage from './features/catalog/MaintenancePage'
import ClientBookingPage from './features/client/ClientBookingPage'
import ClientSpacePage from './features/client/ClientSpacePage'
import StaffHomePage from './features/home/StaffHomePage'
import PrivacyPage from './features/legal/PrivacyPage'
import TermsPage from './features/legal/TermsPage'
import ProfilePage from './features/profile/ProfilePage'
import CatalogPage from './features/public/CatalogPage'
import ClientLoginPage from './features/public/ClientLoginPage'
import HomePage from './features/public/HomePage'
import PublicItemPage from './features/public/PublicItemPage'
import PublicLayout from './features/public/PublicLayout'
import RegisterPage from './features/public/RegisterPage'
import QuotePage from './features/quote/QuotePage'
import UsersPage from './features/users/UsersPage'

function NotFoundPage() {
  return (
    <div className="mx-auto max-w-xl px-4 py-20 text-center">
      <h1 className="text-2xl font-bold">Page introuvable</h1>
      <Link to="/" className="mt-4 inline-block font-semibold text-brand-700">
        Retour à l’accueil
      </Link>
    </div>
  )
}

const only = (roles: Role[], page: ReactNode) => <RequireAuth roles={roles}>{page}</RequireAuth>

export default function App() {
  return (
    <Routes>
      <Route path="/espace-equipe" element={<StaffLoginPage />} />

      {/* Site public : catalogue, devis en ligne, comptes et espace des clients */}
      <Route element={<PublicLayout />}>
        <Route index element={<HomePage />} />
        <Route path="catalogue" element={<CatalogPage />} />
        <Route path="catalogue/:id" element={<PublicItemPage />} />
        <Route path="devis" element={<QuotePage />} />
        <Route path="connexion" element={<ClientLoginPage />} />
        <Route path="inscription" element={<RegisterPage />} />
        <Route path="conditions-generales" element={<TermsPage />} />
        <Route path="confidentialite" element={<PrivacyPage />} />
        <Route path="mon-espace" element={only(['CLIENT'], <ClientSpacePage />)} />
        <Route path="mon-espace/reservations/:id" element={only(['CLIENT'], <ClientBookingPage />)} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>

      {/* Espace de gestion : reserve a l'equipe de FloreEvent */}
      <Route path="/gestion" element={only(STAFF_ROLES, <Layout />)}>
        <Route index element={<StaffHomePage />} />
        <Route path="reservations" element={<BookingsPage />} />
        <Route path="reservations/:id" element={<BookingDetailPage />} />
        <Route path="catalogue" element={<ItemsPage />} />
        <Route path="catalogue/nouveau" element={only(['GERANT'], <ItemFormPage />)} />
        <Route path="catalogue/:id" element={<ItemDetailPage />} />
        <Route path="catalogue/:id/modifier" element={only(['GERANT'], <ItemFormPage />)} />
        <Route path="reparations" element={<MaintenancePage />} />
        <Route path="categories" element={only(['GERANT'], <CategoriesPage />)} />
        <Route path="equipe" element={only(['GERANT'], <UsersPage />)} />
        <Route path="profil" element={<ProfilePage />} />
      </Route>
    </Routes>
  )
}
