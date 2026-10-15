import { useQuery } from '@tanstack/react-query'
import { Package, Plus } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { catalogApi, errorMessage, imageUrl } from '../../api/client'
import { buttonClasses } from '../../components/buttonClasses'
import { Alert, Button, Field, Loader } from '../../components/ui'
import { formatEuros } from '../../lib/format'
import { rentalDays } from '../../lib/selection'
import { DatesBar } from '../quote/DatesBar'
import { useSelection } from '../quote/Selection'

// Fiche publique d'un article : photos, description, tarifs, disponibilite aux dates choisies
export default function PublicItemPage() {
  const id = Number(useParams().id)
  const { selection, add } = useSelection()
  const datesOk = rentalDays(selection.startDate, selection.endDate) !== null
  const period = datesOk ? { from: selection.startDate, to: selection.endDate } : {}
  const item = useQuery({ queryKey: ['catalog-item', id, period], queryFn: () => catalogApi.item(id, period) })
  const [photo, setPhoto] = useState(0)
  const [quantity, setQuantity] = useState('1')
  const [added, setAdded] = useState(false)

  if (item.isPending) return <Loader />
  if (item.isError)
    return (
      <div className="mx-auto max-w-3xl px-4 py-16 text-center">
        <Alert>{errorMessage(item.error)}</Alert>
        <Link to="/catalogue" className={buttonClasses('secondary', 'mt-6')}>
          Voir le catalogue
        </Link>
      </div>
    )
  const it = item.data
  const chosen = selection.lines.find((line) => line.itemId === it.id)?.quantity ?? 0
  const wanted = Math.max(1, Number(quantity) || 1)
  const tooMany = it.available !== undefined && wanted + chosen > it.available

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      <Link to="/catalogue" className="text-sm font-semibold text-brand-700">
        ← Catalogue
      </Link>
      <div className="mt-4 grid gap-8 md:grid-cols-2">
        <div>
          {it.images.length ? (
            <img src={imageUrl(it.images[photo])} alt={it.name} className="aspect-[4/3] w-full rounded-2xl object-cover ring-1 ring-stone-200" />
          ) : (
            <div className="flex aspect-[4/3] items-center justify-center rounded-2xl bg-stone-100 text-stone-400" role="img" aria-label={`${it.name} (pas de photo)`}>
              <Package className="size-16" strokeWidth={1.2} aria-hidden />
            </div>
          )}
          {it.images.length > 1 && (
            <div className="mt-3 flex gap-2 overflow-x-auto">
              {it.images.map((url, index) => (
                <button key={url} type="button" onClick={() => setPhoto(index)} aria-label={`Photo ${index + 1}`} className={`shrink-0 overflow-hidden rounded-lg ring-2 ${index === photo ? 'ring-brand-600' : 'ring-transparent'}`}>
                  <img src={imageUrl(url)} alt="" className="size-16 object-cover" />
                </button>
              ))}
            </div>
          )}
        </div>
        <div className="min-w-0">
          <p className="text-sm font-semibold text-slate-500">{it.category.name}</p>
          <h1 className="mt-1 text-3xl font-extrabold tracking-tight">{it.name}</h1>
          <p className="mt-3 text-2xl font-extrabold text-brand-800 tabular-nums">
            {formatEuros(it.dayPriceCents)} <span className="text-base font-normal text-slate-500">/ jour</span>
          </p>
          <p className="mt-1 text-sm text-slate-600">Chaque jour supplémentaire : {formatEuros(Math.round(it.dayPriceCents / 2))}</p>
          {it.setupPriceCents > 0 && <p className="text-sm text-slate-600">Installation sur place (avec livraison) : {formatEuros(it.setupPriceCents)}</p>}
          {it.description && <p className="mt-5 whitespace-pre-line text-slate-700">{it.description}</p>}
          <div className="mt-6">
            <DatesBar />
          </div>
          {it.available !== undefined && (
            <p className={`mt-3 text-sm font-semibold ${it.available > 0 ? 'text-emerald-700' : 'text-red-700'}`}>
              {it.available > 0 ? `${it.available} disponible(s) à vos dates` : 'Indisponible à vos dates'}
            </p>
          )}
          <div className="mt-4 flex items-end gap-3">
            <Field label="Quantité" type="number" min={1} value={quantity} onChange={(e) => setQuantity(e.target.value)} className="w-28" />
            <Button
              disabled={tooMany}
              onClick={() => {
                add(it, wanted)
                setAdded(true)
              }}
            >
              <Plus className="size-4" aria-hidden />
              Ajouter au devis
            </Button>
          </div>
          {tooMany && <p className="mt-2 text-sm text-red-700">Pas assez d’exemplaires disponibles à vos dates.</p>}
          {added && (
            <div className="mt-4">
              <Alert type="success">
                Ajouté à votre devis ({chosen} au total).{' '}
                <Link to="/devis" className="font-bold underline">
                  Voir mon devis
                </Link>
              </Alert>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
