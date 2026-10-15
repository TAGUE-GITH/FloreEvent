import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { categoriesApi, errorMessage, fieldErrors, itemsApi } from '../../api/client'
import type { Item } from '../../api/types'
import { Alert, Button, Card, Checkbox, Field, Loader, PageHeader, Select, TextArea } from '../../components/ui'
import { centsToInput, parseEuros } from '../../lib/money'

type Draft = {
  reference: string
  name: string
  categoryId: number | ''
  description: string
  dayPrice: string
  replacementValue: string
  setupPrice: string
  quantity: string
  active: boolean
}

const EMPTY: Draft = { reference: '', name: '', categoryId: '', description: '', dayPrice: '', replacementValue: '', setupPrice: '', quantity: '', active: true }

const draftOf = (item: Item): Draft => ({
  reference: item.reference,
  name: item.name,
  categoryId: item.category.id,
  description: item.description ?? '',
  dayPrice: centsToInput(item.dayPriceCents),
  replacementValue: centsToInput(item.replacementValueCents),
  setupPrice: item.setupPriceCents ? centsToInput(item.setupPriceCents) : '',
  quantity: String(item.quantity),
  active: item.active,
})

// US-03 : creation et modification d'un article (gerant). Les montants sont saisis en euros, envoyes en centimes.
export default function ItemFormPage() {
  const raw = useParams().id
  const id = raw ? Number(raw) : undefined
  const item = useQuery({ queryKey: ['item', id], queryFn: () => itemsApi.get(id!), enabled: id !== undefined })
  if (id !== undefined && item.isPending) return <Loader />
  if (item.isError) return <Alert>{errorMessage(item.error)}</Alert>
  return <ItemForm key={id ?? 'nouveau'} id={id} initial={item.data ? draftOf(item.data) : EMPTY} />
}

function ItemForm({ id, initial }: { id?: number; initial: Draft }) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const categories = useQuery({ queryKey: ['categories'], queryFn: categoriesApi.list })
  const [draft, setDraft] = useState(initial)
  const [moneyErrors, setMoneyErrors] = useState<Record<string, string>>({})
  const set = (changes: Partial<Draft>) => setDraft((current) => ({ ...current, ...changes }))

  const save = useMutation({
    mutationFn: (cents: { dayPriceCents: number; replacementValueCents: number; setupPriceCents: number }) =>
      itemsApi.save(
        {
          reference: draft.reference,
          name: draft.name,
          categoryId: draft.categoryId,
          description: draft.description,
          quantity: draft.quantity === '' ? '' : Number(draft.quantity),
          active: draft.active,
          ...cents,
        },
        id,
      ),
    onSuccess: (saved) => {
      queryClient.invalidateQueries({ queryKey: ['items'] })
      queryClient.invalidateQueries({ queryKey: ['categories'] })
      queryClient.setQueryData(['item', saved.id], saved)
      navigate(`/gestion/catalogue/${saved.id}`)
    },
  })
  const errors = { ...fieldErrors(save.error), ...moneyErrors }

  const submit = (event: FormEvent) => {
    event.preventDefault()
    const amounts = {
      dayPriceCents: parseEuros(draft.dayPrice),
      replacementValueCents: parseEuros(draft.replacementValue),
      setupPriceCents: draft.setupPrice.trim() ? parseEuros(draft.setupPrice) : 0,
    }
    const invalid = Object.fromEntries(
      Object.entries(amounts)
        .filter(([, cents]) => cents === null)
        .map(([field]) => [field, 'Montant invalide (exemple : 12,50).']),
    )
    setMoneyErrors(invalid)
    if (Object.keys(invalid).length) return
    save.mutate(amounts as { dayPriceCents: number; replacementValueCents: number; setupPriceCents: number })
  }

  return (
    <>
      <Link to={id ? `/gestion/catalogue/${id}` : '/gestion/catalogue'} className="text-sm font-semibold text-brand-700">
        ← {id ? 'Retour à l’article' : 'Catalogue'}
      </Link>
      <PageHeader title={id ? 'Modifier l’article' : 'Nouvel article'} />
      <Card className="max-w-3xl">
        <form onSubmit={submit} className="space-y-4" noValidate>
          <Alert>{save.isError && !Object.keys(fieldErrors(save.error)).length ? errorMessage(save.error) : ''}</Alert>
          <div className="grid gap-4 sm:grid-cols-[12rem_1fr]">
            <Field label="Référence" value={draft.reference} onChange={(e) => set({ reference: e.target.value })} error={errors.reference} placeholder="BARNUM-3X6" />
            <Field label="Nom" value={draft.name} onChange={(e) => set({ name: e.target.value })} error={errors.name} placeholder="Barnum 3 x 6 m" />
          </div>
          <Select label="Catégorie" value={draft.categoryId} onChange={(e) => set({ categoryId: e.target.value ? Number(e.target.value) : '' })} error={errors.categoryId}>
            <option value="">Choisir…</option>
            {categories.data?.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </Select>
          <TextArea label="Description" rows={3} value={draft.description} onChange={(e) => set({ description: e.target.value })} error={errors.description} />
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Field label="Prix par jour (€)" inputMode="decimal" value={draft.dayPrice} onChange={(e) => set({ dayPrice: e.target.value })} error={errors.dayPriceCents} placeholder="150" />
            <Field
              label="Valeur de remplacement (€)"
              inputMode="decimal"
              value={draft.replacementValue}
              onChange={(e) => set({ replacementValue: e.target.value })}
              error={errors.replacementValueCents}
              hint="Facturée en cas de casse ou de perte"
            />
            <Field label="Installation (€)" inputMode="decimal" value={draft.setupPrice} onChange={(e) => set({ setupPrice: e.target.value })} error={errors.setupPriceCents} hint="Vide : pas d’installation" />
            <Field label="Quantité en stock" type="number" min={0} inputMode="numeric" value={draft.quantity} onChange={(e) => set({ quantity: e.target.value })} error={errors.quantity} />
          </div>
          {id && <Checkbox label="Proposé à la location" checked={draft.active} onChange={(e) => set({ active: e.target.checked })} />}
          <Button type="submit" loading={save.isPending}>
            {id ? 'Enregistrer' : 'Ajouter au catalogue'}
          </Button>
        </form>
      </Card>
    </>
  )
}
