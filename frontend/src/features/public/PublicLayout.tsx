import { ClipboardList, Flower2, LayoutDashboard, Menu, User, X } from 'lucide-react'
import { useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'
import { homeOf, useAuth } from '../../auth/AuthProvider'
import { Avatar } from '../../components/Avatar'
import { buttonClasses } from '../../components/buttonClasses'
import { useSelection } from '../quote/Selection'

// Annee du pied de page, calculee une fois au chargement
const YEAR = new Date().getFullYear()

const LINKS = [
  { to: '/', label: 'Accueil' },
  { to: '/catalogue', label: 'Catalogue' },
]

function Logo() {
  return (
    <Link to="/" className="flex items-center gap-2.5">
      <span className="flex size-9 items-center justify-center rounded-xl bg-brand-700 text-white">
        <Flower2 className="size-5" aria-hidden />
      </span>
      <span className="text-lg font-extrabold tracking-tight">
        Flore<span className="text-brand-700">Event</span>
      </span>
    </Link>
  )
}

function AccountButton() {
  const { user, isStaff } = useAuth()
  if (!user)
    return (
      <Link to="/connexion" className={buttonClasses('ghost', 'px-3')}>
        Connexion
      </Link>
    )
  return (
    <Link to={homeOf(user)} className="flex items-center gap-2 rounded-xl px-2 py-1.5 text-sm font-semibold hover:bg-stone-100">
      <Avatar user={user} />
      <span className="hidden sm:inline">{isStaff ? 'Gestion' : 'Mon espace'}</span>
      {isStaff ? <LayoutDashboard className="size-4 sm:hidden" aria-hidden /> : <User className="size-4 sm:hidden" aria-hidden />}
    </Link>
  )
}

/** Le devis en cours, toujours visible, avec le nombre d'articles choisis. */
function QuoteButton() {
  const { count } = useSelection()
  return (
    <Link to="/devis" className="relative flex items-center gap-2 rounded-xl px-2.5 py-2 text-sm font-semibold text-slate-700 hover:bg-stone-100" aria-label={count ? `Mon devis : ${count} article(s)` : 'Mon devis'}>
      <ClipboardList className="size-5" aria-hidden />
      <span className="hidden sm:inline">Mon devis</span>
      {count > 0 && <span className="rounded-full bg-brand-700 px-1.5 text-xs font-bold text-white">{count}</span>}
    </Link>
  )
}

// Site public de FloreEvent : catalogue, devis en ligne, espace client
export default function PublicLayout() {
  const [open, setOpen] = useState(false)
  return (
    <div className="flex min-h-dvh flex-col bg-stone-50">
      <header className="sticky top-0 z-30 border-b border-stone-200 bg-white/90 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center gap-6 px-4 py-3 sm:px-6">
          <Logo />
          <nav className="hidden items-center gap-5 md:flex" aria-label="Site">
            {LINKS.map((link) => (
              <NavLink key={link.to} to={link.to} end className={({ isActive }) => `text-sm font-semibold ${isActive ? 'text-brand-700' : 'text-slate-700 hover:text-brand-700'}`}>
                {link.label}
              </NavLink>
            ))}
          </nav>
          <div className="ml-auto flex items-center gap-1">
            <QuoteButton />
            <div className="hidden md:block">
              <AccountButton />
            </div>
          </div>
          <button
            type="button"
            onClick={() => setOpen(!open)}
            aria-expanded={open}
            aria-label={open ? 'Fermer le menu' : 'Ouvrir le menu'}
            className="-ml-4 rounded-xl p-2 text-slate-700 hover:bg-stone-100 md:hidden"
          >
            {open ? <X className="size-6" /> : <Menu className="size-6" />}
          </button>
        </div>
        {open && (
          <div className="space-y-3 border-t border-stone-200 px-4 py-4 md:hidden">
            <nav className="flex flex-col gap-1" aria-label="Site">
              {LINKS.map((link) => (
                <Link key={link.to} to={link.to} onClick={() => setOpen(false)} className="rounded-lg px-3 py-2 text-sm font-semibold hover:bg-stone-100">
                  {link.label}
                </Link>
              ))}
            </nav>
            <AccountButton />
          </div>
        )}
      </header>

      <main className="flex-1">
        <Outlet />
      </main>

      <footer className="border-t border-stone-200 bg-white">
        <div className="mx-auto flex max-w-6xl flex-col gap-4 px-4 py-8 text-sm text-slate-600 sm:flex-row sm:items-center sm:justify-between sm:px-6">
          <div>
            <Logo />
            <p className="mt-2">Location de matériel et décoration pour vos événements, à Melun et alentours.</p>
          </div>
          <div className="flex flex-wrap gap-x-5 gap-y-2">
            <Link to="/conditions-generales" className="hover:text-brand-700">
              Conditions générales
            </Link>
            <Link to="/confidentialite" className="hover:text-brand-700">
              Confidentialité
            </Link>
            <Link to="/espace-equipe" className="hover:text-brand-700">
              Espace équipe
            </Link>
          </div>
        </div>
        <p className="border-t border-stone-200 py-4 text-center text-xs text-slate-500">© {YEAR} FloreEvent · Melun</p>
      </footer>
    </div>
  )
}
