import { useQuery } from '@tanstack/react-query'
import { Check, Package, Plus, Search } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { catalogApi, errorMessage, imageUrl } from '../../api/client'
import type { PublicItem } from '../../api/types'
import { Alert, Button, Loader } from '../../components/ui'
import { formatEuros } from '../../lib/format'
import { rentalDays } from '../../lib/selection'
import { DatesBar } from '../quote/DatesBar'
import { useSelection } from '../quote/Selection'

// US-05 : catalogue public, avec les disponibilites pour les dates de l'evenement
export default function CatalogPage() {
  const { selection } = useSelection()
  const [q, setQ] = useState('')
  const [categoryId, setCategoryId] = useState<number | ''>('')
  const datesOk = rentalDays(selection.startDate, selection.endDate) !== null
  const period = datesOk ? { from: selection.startDate, to: selection.endDate } : {}
  const items = useQuery({ queryKey: ['catalog', q, categoryId, period], queryFn: () => catalogApi.items({ q, categoryId, ...period }) })
  const categories = useQuery({ queryKey: ['catalog-categories'], queryFn: catalogApi.categories })

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <h1 className="text-3xl font-extrabold tracking-tight">Catalogue</h1>
      <p className="mt-1 text-slate-600">Choisissez vos dates, composez votre sélection : le devis se calcule tout seul.</p>
      <div className="mt-6">
        <DatesBar />
      </div>

      <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:items-center">
        <label className="relative block sm:w-80">
          <span className="sr-only">Rechercher un article</span>
          <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" aria-hidden />
          <input
            type="search"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Rechercher : chaise, barnum, vaisselle…"
            className="w-full rounded-xl border border-slate-300 bg-white py-2.5 pr-3 pl-9 text-sm focus:border-brand-600 focus:ring-4 focus:ring-brand-100 focus:outline-none"
          />
        </label>
        {categories.data && categories.data.length > 1 && (
          <div className="flex flex-wrap gap-2" role="group" aria-label="Catégories">
            {[{ id: '' as const, name: 'Tout' }, ...categories.data].map((category) => (
              <button
                key={category.id}
                type="button"
                aria-pressed={categoryId === category.id}
                onClick={() => setCategoryId(category.id)}
                className={`rounded-full px-3.5 py-1.5 text-sm font-semibold ring-1 ${categoryId === category.id ? 'bg-brand-700 text-white ring-brand-700' : 'bg-white text-slate-700 ring-slate-300 hover:bg-stone-50'}`}
              >
                {category.name}
              </button>
            ))}
          </div>
        )}
      </div>

      <div className="mt-8">
        {items.isPending && <Loader />}
        {items.isError && <Alert>{errorMessage(items.error)}</Alert>}
        {items.data?.length === 0 && <p className="rounded-2xl bg-white p-8 text-center text-slate-600 ring-1 ring-stone-200">Aucun article ne correspond à votre recherche.</p>}
        {items.data && items.data.length > 0 && (
          <ul className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {items.data.map((item) => (
              <ItemCard key={item.id} item={item} />
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}

function ItemCard({ item }: { item: PublicItem }) {
  const { selection, add } = useSelection()
  const chosen = selection.lines.find((line) => line.itemId === item.id)?.quantity ?? 0
  const [quantity, setQuantity] = useState(1)
  const left = item.available === undefined ? undefined : item.available - chosen

  return (
    <li className="flex flex-col overflow-hidden rounded-2xl bg-white ring-1 ring-stone-200 transition hover:shadow-md">
      <Link to={`/catalogue/${item.id}`}>
        {item.imageUrl ? (
          <img src={imageUrl(item.imageUrl)} alt={item.name} loading="lazy" className="aspect-[4/3] w-full object-cover" />
        ) : (
          <div className="flex aspect-[4/3] items-center justify-center bg-stone-100 text-stone-400" role="img" aria-label={`${item.name} (pas de photo)`}>
            <Package className="size-12" strokeWidth={1.2} aria-hidden />
          </div>
        )}
      </Link>
      <div className="flex flex-1 flex-col gap-1 p-4">
        <p className="text-xs font-semibold text-slate-500">{item.category.name}</p>
        <Link to={`/catalogue/${item.id}`} className="font-semibold hover:text-brand-700">
          {item.name}
        </Link>
        <p className="font-extrabold text-brand-800 tabular-nums">
          {formatEuros(item.dayPriceCents)} <span className="text-sm font-normal text-slate-500">/ jour</span>
        </p>
        {item.available !== undefined && (
          <p className={`text-xs font-semibold ${item.available > 0 ? 'text-emerald-700' : 'text-red-700'}`}>
            {item.available > 0 ? `${item.available} disponible(s) à vos dates` : 'Indisponible à vos dates'}
          </p>
        )}
        <div className="mt-auto flex items-center gap-2 pt-3">
          <input
            type="number"
            min={1}
            value={quantity}
            onChange={(e) => setQuantity(Math.max(1, Number(e.target.value) || 1))}
            aria-label={`Quantité de ${item.name}`}
            className="w-20 rounded-xl border border-slate-300 px-2.5 py-2 text-sm"
          />
          <Button
            variant={chosen ? 'secondary' : 'primary'}
            className="flex-1 px-2"
            disabled={left !== undefined && quantity > left}
            onClick={() => add(item, quantity)}
            aria-label={`Ajouter au devis : ${item.name}`}
          >
            {chosen ? <Check className="size-4" aria-hidden /> : <Plus className="size-4" aria-hidden />}
            {chosen ? `Au devis (${chosen})` : 'Ajouter'}
          </Button>
        </div>
      </div>
    </li>
  )
}
