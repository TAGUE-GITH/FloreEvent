import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Camera, Package, Pencil, Star, Trash2, Wrench } from 'lucide-react'
import { useRef, useState } from 'react'
import type { ChangeEvent, FormEvent, ReactNode } from 'react'
import { Link, useParams } from 'react-router-dom'
import { errorMessage, fieldErrors, imageUrl, itemsApi } from '../../api/client'
import type { Item, MaintenanceForm } from '../../api/types'
import { useAuth } from '../../auth/AuthProvider'
import { buttonClasses } from '../../components/buttonClasses'
import { Alert, Badge, Button, Card, Field, Loader, PageHeader } from '../../components/ui'
import { todayIso } from '../../lib/dates'
import { formatDate, formatEuros } from '../../lib/format'
import { resizeImage } from '../../lib/image'

// Fiche d'un article : photos, tarifs, stock, materiel en reparation
export default function ItemDetailPage() {
  const id = Number(useParams().id)
  const { hasRole } = useAuth()
  const item = useQuery({ queryKey: ['item', id], queryFn: () => itemsApi.get(id) })

  if (item.isPending) return <Loader />
  if (item.isError) return <Alert>{errorMessage(item.error)}</Alert>
  const it = item.data

  return (
    <>
      <Link to="/gestion/catalogue" className="text-sm font-semibold text-brand-700">
        ← Catalogue
      </Link>
      <PageHeader
        title={it.name}
        subtitle={`${it.reference} · ${it.category.name}`}
        actions={
          hasRole('GERANT') && (
            <Link to={`/gestion/catalogue/${it.id}/modifier`} className={buttonClasses('secondary')}>
              <Pencil className="size-4" aria-hidden />
              Modifier
            </Link>
          )
        }
      />
      <div className="grid gap-6 lg:grid-cols-[1fr_22rem]">
        <div className="min-w-0 space-y-6">
          <Photos item={it} editable={hasRole('GERANT')} />
          {it.description && (
            <Card>
              <h2 className="font-bold">Description</h2>
              <p className="mt-1 whitespace-pre-line text-slate-700">{it.description}</p>
            </Card>
          )}
          <Repairs item={it} canRepair={hasRole('GERANT', 'LOGISTIQUE')} />
        </div>
        <Card className="h-fit space-y-2 text-sm">
          <h2 className="mb-1 font-bold">Tarifs et stock</h2>
          <Row label="Prix par jour" value={formatEuros(it.dayPriceCents)} />
          <Row label="Jour supplémentaire" value={formatEuros(Math.round(it.dayPriceCents / 2))} />
          <Row label="Installation" value={it.setupPriceCents ? formatEuros(it.setupPriceCents) : 'Aucune'} />
          <Row label="Valeur de remplacement" value={formatEuros(it.replacementValueCents)} />
          <div className="border-t border-stone-100 pt-2" />
          <Row label="Stock total" value={String(it.quantity)} />
          <Row label="En réparation aujourd’hui" value={String(it.inRepair)} />
          <Row label="Utilisables aujourd’hui" value={String(it.quantity - it.inRepair)} />
          {!it.active && (
            <p className="pt-2">
              <Badge>Retiré de la location</Badge>
            </p>
          )}
        </Card>
      </div>
    </>
  )
}

function Row({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="flex justify-between gap-3">
      <span className="text-slate-500">{label}</span>
      <span className="font-semibold tabular-nums">{value}</span>
    </div>
  )
}

/** Photos de l'article : la premiere est la photo principale (affichee dans le catalogue). */
function Photos({ item, editable }: { item: Item; editable: boolean }) {
  const queryClient = useQueryClient()
  const input = useRef<HTMLInputElement>(null)
  const [error, setError] = useState('')
  const onSuccess = (updated: Item) => {
    queryClient.setQueryData(['item', item.id], updated)
    queryClient.invalidateQueries({ queryKey: ['items'] })
  }
  const add = useMutation({ mutationFn: (dataUrl: string) => itemsApi.addImage(item.id, dataUrl), onSuccess, onError: (err) => setError(errorMessage(err)) })
  const main = useMutation({ mutationFn: (imageId: string) => itemsApi.makeMain(item.id, imageId), onSuccess })
  const remove = useMutation({ mutationFn: (imageId: string) => itemsApi.removeImage(item.id, imageId), onSuccess })

  const choose = async (event: ChangeEvent<HTMLInputElement>) => {
    const files = [...(event.target.files ?? [])]
    event.target.value = ''
    setError('')
    for (const file of files) {
      try {
        await add.mutateAsync(await resizeImage(file, 1200, 0.8))
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Image illisible.')
      }
    }
  }

  return (
    <Card>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="font-bold">Photos</h2>
        {editable && (
          <>
            <input ref={input} type="file" accept="image/jpeg,image/png" multiple className="sr-only" onChange={choose} aria-label="Ajouter des photos" />
            <Button variant="secondary" loading={add.isPending} onClick={() => input.current?.click()}>
              <Camera className="size-4" aria-hidden />
              Ajouter des photos
            </Button>
          </>
        )}
      </div>
      <div className="mt-2">
        <Alert>{error}</Alert>
      </div>
      {item.images.length === 0 ? (
        <div className="mt-3 flex aspect-[3/1] items-center justify-center rounded-xl bg-stone-100 text-stone-400">
          <Package className="size-10" strokeWidth={1.2} aria-hidden />
        </div>
      ) : (
        <ul className="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-3">
          {item.images.map((photo, index) => (
            <li key={photo.id} className="relative overflow-hidden rounded-xl ring-1 ring-stone-200">
              <img src={imageUrl(photo.url)} alt={index === 0 ? `${item.name} (photo principale)` : item.name} className="aspect-square w-full object-cover" />
              {index === 0 && <span className="absolute top-2 left-2 rounded-full bg-brand-700 px-2 py-0.5 text-xs font-semibold text-white">Principale</span>}
              {editable && (
                <div className="absolute right-2 bottom-2 flex gap-1">
                  {index > 0 && (
                    <button type="button" onClick={() => main.mutate(photo.id)} className="rounded-lg bg-white/90 p-1.5 text-slate-700 shadow hover:bg-white" aria-label="Choisir comme photo principale" title="Photo principale">
                      <Star className="size-4" />
                    </button>
                  )}
                  <button type="button" onClick={() => remove.mutate(photo.id)} className="rounded-lg bg-white/90 p-1.5 text-red-700 shadow hover:bg-white" aria-label="Supprimer la photo" title="Supprimer">
                    <Trash2 className="size-4" />
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </Card>
  )
}

/** US-04 : exemplaires mis en reparation (ils ne sont plus proposes a la location sur la periode). */
function Repairs({ item, canRepair }: { item: Item; canRepair: boolean }) {
  const queryClient = useQueryClient()
  const history = useQuery({ queryKey: ['repairs', item.id], queryFn: () => itemsApi.repairs(item.id) })
  const [form, setForm] = useState<MaintenanceForm>(() => ({ quantity: 1, fromDate: todayIso(), toDate: '', reason: '' }))
  const [done, setDone] = useState('')
  const create = useMutation({
    mutationFn: () => itemsApi.putInRepair(item.id, form),
    onSuccess: (repair) => {
      setDone(`${repair.quantity} exemplaire(s) mis en réparation.`)
      setForm({ quantity: 1, fromDate: todayIso(), toDate: '', reason: '' })
      for (const key of [['repairs', item.id], ['item', item.id], ['items'], ['maintenance']]) queryClient.invalidateQueries({ queryKey: key })
    },
  })
  const errors = fieldErrors(create.error)
  const submit = (event: FormEvent) => {
    event.preventDefault()
    setDone('')
    create.mutate()
  }

  return (
    <Card>
      <h2 className="flex items-center gap-2 font-bold">
        <Wrench className="size-5 text-brand-700" aria-hidden />
        Réparations
      </h2>
      {canRepair && (
        <form onSubmit={submit} className="mt-3 space-y-3 rounded-xl bg-stone-50 p-4" noValidate>
          <Alert type="success">{done}</Alert>
          <Alert>{create.isError && !Object.keys(errors).length ? errorMessage(create.error) : ''}</Alert>
          <div className="grid gap-3 sm:grid-cols-3">
            <Field label="Quantité" type="number" min={1} value={form.quantity} onChange={(e) => setForm({ ...form, quantity: e.target.value === '' ? '' : Number(e.target.value) })} error={errors.quantity} />
            <Field label="Du" type="date" value={form.fromDate} onChange={(e) => setForm({ ...form, fromDate: e.target.value })} error={errors.fromDate} />
            <Field label="Au (facultatif)" type="date" value={form.toDate} onChange={(e) => setForm({ ...form, toDate: e.target.value })} error={errors.toDate} hint="Vide : jusqu’à réparation" />
          </div>
          <Field label="Motif" value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} error={errors.reason} placeholder="Bâche déchirée, pied tordu…" />
          <Button type="submit" variant="secondary" loading={create.isPending}>
            Mettre en réparation
          </Button>
        </form>
      )}
      {history.data && history.data.length === 0 && <p className="mt-3 text-sm text-slate-500">Aucune réparation pour cet article.</p>}
      {history.data && history.data.length > 0 && (
        <ul className="mt-3 divide-y divide-stone-100 text-sm">
          {history.data.map((repair) => (
            <li key={repair.id} className="py-2.5">
              <p className="font-semibold">
                {repair.quantity} exemplaire(s) · du {formatDate(repair.fromDate)} {repair.toDate ? `au ${formatDate(repair.toDate)}` : '· jusqu’à réparation'}
              </p>
              <p className="text-xs text-slate-500">
                {repair.reason ?? 'Sans motif'} · {repair.author}
              </p>
            </li>
          ))}
        </ul>
      )}
    </Card>
  )
}
