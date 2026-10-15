import { useQuery } from '@tanstack/react-query'
import { Package, Plus, Search, Wrench } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { categoriesApi, errorMessage, imageUrl, itemsApi } from '../../api/client'
import type { Item } from '../../api/types'
import { useAuth } from '../../auth/AuthProvider'
import { buttonClasses } from '../../components/buttonClasses'
import { Alert, Badge, Loader, PageHeader, Select } from '../../components/ui'
import { formatEuros } from '../../lib/format'

// US-03 : catalogue de location, avec recherche et filtre par categorie
export default function ItemsPage() {
  const { hasRole } = useAuth()
  const [q, setQ] = useState('')
  const [categoryId, setCategoryId] = useState<number | ''>('')
  const items = useQuery({ queryKey: ['items', q, categoryId], queryFn: () => itemsApi.list({ q, categoryId }) })
  const categories = useQuery({ queryKey: ['categories'], queryFn: categoriesApi.list })

  return (
    <>
      <PageHeader
        title="Catalogue"
        subtitle="Matériel proposé à la location"
        actions={
          hasRole('GERANT') && (
            <Link to="/gestion/catalogue/nouveau" className={buttonClasses('primary')}>
              <Plus className="size-4" aria-hidden />
              Nouvel article
            </Link>
          )
        }
      />
      <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-end">
        <label className="relative block flex-1 sm:max-w-sm">
          <span className="sr-only">Rechercher un article</span>
          <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" aria-hidden />
          <input
            type="search"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Nom ou référence…"
            className="w-full rounded-xl border border-slate-300 bg-white py-2.5 pr-3 pl-9 text-sm focus:border-brand-600 focus:ring-4 focus:ring-brand-100 focus:outline-none"
          />
        </label>
        <Select label="Catégorie" value={categoryId} onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : '')} className="sm:w-60">
          <option value="">Toutes</option>
          {categories.data?.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </Select>
      </div>
      {items.isPending && <Loader />}
      {items.isError && <Alert>{errorMessage(items.error)}</Alert>}
      {items.data?.length === 0 && (
        <p className="rounded-2xl bg-white p-8 text-center text-slate-500 ring-1 ring-stone-200">
          {q || categoryId ? 'Aucun article ne correspond à la recherche.' : 'Le catalogue est vide pour le moment.'}
        </p>
      )}
      {items.data && items.data.length > 0 && (
        <ul className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {items.data.map((item) => (
            <ItemCard key={item.id} item={item} />
          ))}
        </ul>
      )}
    </>
  )
}

function ItemCard({ item }: { item: Item }) {
  const available = item.quantity - item.inRepair
  return (
    <li>
      <Link to={`/gestion/catalogue/${item.id}`} className="flex gap-4 rounded-2xl bg-white p-3 ring-1 ring-stone-200 transition hover:shadow-md">
        {item.imageUrl ? (
          <img src={imageUrl(item.imageUrl)} alt="" className="size-24 shrink-0 rounded-xl object-cover" />
        ) : (
          <span className="flex size-24 shrink-0 items-center justify-center rounded-xl bg-stone-100 text-stone-400">
            <Package className="size-8" strokeWidth={1.3} aria-hidden />
          </span>
        )}
        <div className="min-w-0 flex-1 py-1">
          <p className="text-xs font-semibold text-slate-500">{item.category.name}</p>
          <p className="truncate font-semibold">{item.name}</p>
          <p className="text-sm font-bold text-brand-800 tabular-nums">{formatEuros(item.dayPriceCents)} / jour</p>
          <p className="mt-1 flex flex-wrap items-center gap-1.5 text-xs text-slate-600">
            <span>
              {available} / {item.quantity} disponible(s)
            </span>
            {item.inRepair > 0 && (
              <Badge tone="amber">
                <Wrench className="mr-1 inline size-3" aria-hidden />
                {item.inRepair} en réparation
              </Badge>
            )}
            {!item.active && <Badge>Retiré de la location</Badge>}
          </p>
        </div>
      </Link>
    </li>
  )
}
