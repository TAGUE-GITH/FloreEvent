import { useQuery } from '@tanstack/react-query'
import { FolderTree, Package, Wrench } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { Link } from 'react-router-dom'
import { categoriesApi, itemsApi, maintenanceApi } from '../../api/client'
import { ROLE_LABELS } from '../../api/types'
import { useAuth } from '../../auth/AuthProvider'
import { PageHeader } from '../../components/ui'

function Tile({ label, value, to, Icon }: { label: string; value: string; to: string; Icon: LucideIcon }) {
  return (
    <Link to={to} className="block rounded-2xl bg-white p-5 ring-1 ring-stone-200 transition hover:shadow-md">
      <p className="flex items-center gap-2 text-sm font-medium text-slate-500">
        <Icon className="size-4" aria-hidden />
        {label}
      </p>
      <p className="mt-2 text-3xl font-extrabold tabular-nums">{value}</p>
    </Link>
  )
}

// Accueil de l'espace de gestion : l'essentiel du catalogue en un coup d'oeil
export default function StaffHomePage() {
  const { user } = useAuth()
  const items = useQuery({ queryKey: ['items', '', ''], queryFn: () => itemsApi.list() })
  const categories = useQuery({ queryKey: ['categories'], queryFn: categoriesApi.list })
  const repairs = useQuery({ queryKey: ['maintenance'], queryFn: maintenanceApi.open })
  const count = (value?: number) => (value === undefined ? '…' : String(value))

  return (
    <>
      <PageHeader title={`Bonjour ${user?.firstName ?? ''}`} subtitle={user ? `${ROLE_LABELS[user.role]} · FloreEvent` : undefined} />
      <div className="grid gap-4 sm:grid-cols-3">
        <Tile label="Articles au catalogue" value={count(items.data?.filter((item) => item.active).length)} to="/gestion/catalogue" Icon={Package} />
        <Tile label="Catégories" value={count(categories.data?.length)} to="/gestion/catalogue" Icon={FolderTree} />
        <Tile label="Réparations en cours" value={count(repairs.data?.length)} to="/gestion/reparations" Icon={Wrench} />
      </div>
    </>
  )
}
