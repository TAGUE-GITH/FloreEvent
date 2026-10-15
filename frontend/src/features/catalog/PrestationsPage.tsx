import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Pencil, Plus } from 'lucide-react'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { errorMessage, fieldErrors, prestationsApi } from '../../api/client'
import type { Prestation } from '../../api/types'
import { Alert, Badge, Button, Card, Checkbox, Field, Loader, PageHeader, TextArea } from '../../components/ui'
import { formatEuros } from '../../lib/format'
import { centsToInput, parseEuros } from '../../lib/money'

// US-18 : prestations proposees avec le materiel (decoration florale, mise en place...), gerees par le gerant
export default function PrestationsPage() {
  const prestations = useQuery({ queryKey: ['prestations'], queryFn: prestationsApi.list })
  const [editing, setEditing] = useState<Prestation | 'new' | null>(null)

  return (
    <>
      <PageHeader
        title="Prestations"
        subtitle="Proposées aux clients dans leur devis, au prix « à partir de » (ajustable avec la remise du devis)."
        actions={
          <Button onClick={() => setEditing('new')}>
            <Plus className="size-4" aria-hidden />
            Nouvelle prestation
          </Button>
        }
      />
      {editing && <PrestationForm key={editing === 'new' ? 'new' : editing.id} prestation={editing === 'new' ? undefined : editing} onDone={() => setEditing(null)} />}
      {prestations.isPending && <Loader />}
      <Alert>{prestations.isError ? errorMessage(prestations.error) : ''}</Alert>
      <div className="mt-6 grid gap-4 md:grid-cols-2">
        {prestations.data?.map((p) => (
          <Card key={p.id} className="space-y-1">
            <div className="flex items-start justify-between gap-3">
              <p className="font-bold">{p.name}</p>
              <Badge tone={p.active ? 'green' : 'slate'}>{p.active ? 'Proposée' : 'Masquée'}</Badge>
            </div>
            <p className="text-sm text-slate-600">{p.description}</p>
            <div className="flex items-center justify-between pt-2">
              <p className="font-semibold">à partir de {formatEuros(p.priceCents)}</p>
              <Button variant="ghost" onClick={() => setEditing(p)} aria-label={`Modifier ${p.name}`}>
                <Pencil className="size-4" aria-hidden />
                Modifier
              </Button>
            </div>
          </Card>
        ))}
      </div>
    </>
  )
}

function PrestationForm({ prestation, onDone }: { prestation?: Prestation; onDone: () => void }) {
  const queryClient = useQueryClient()
  const [name, setName] = useState(prestation?.name ?? '')
  const [description, setDescription] = useState(prestation?.description ?? '')
  const [price, setPrice] = useState(prestation ? centsToInput(prestation.priceCents) : '')
  const [active, setActive] = useState(prestation?.active ?? true)
  const [priceError, setPriceError] = useState('')
  const save = useMutation({
    mutationFn: (priceCents: number) => prestationsApi.save({ name, description, priceCents, active }, prestation?.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['prestations'] })
      onDone()
    },
  })
  const errors = fieldErrors(save.error)

  const submit = (event: FormEvent) => {
    event.preventDefault()
    const cents = parseEuros(price)
    if (cents === null) return setPriceError('Indiquez un prix en euros, par exemple 450.')
    setPriceError('')
    save.mutate(cents)
  }

  return (
    <Card className="mb-6">
      <h2 className="font-bold">{prestation ? `Modifier « ${prestation.name} »` : 'Nouvelle prestation'}</h2>
      <form onSubmit={submit} className="mt-4 grid gap-4 sm:grid-cols-2">
        <Field label="Nom" value={name} onChange={(e) => setName(e.target.value)} error={errors.name} />
        <Field label="Prix à partir de (€ TTC)" inputMode="decimal" value={price} onChange={(e) => setPrice(e.target.value)} error={priceError || errors.priceCents} />
        <TextArea label="Description" className="sm:col-span-2" value={description} onChange={(e) => setDescription(e.target.value)} error={errors.description} />
        <Checkbox label="Proposée aux clients sur le site" checked={active} onChange={(e) => setActive(e.target.checked)} className="sm:col-span-2" />
        <div className="flex flex-wrap gap-2 sm:col-span-2">
          <Alert>{save.isError && !Object.keys(errors).length ? errorMessage(save.error) : ''}</Alert>
          <Button type="submit" loading={save.isPending}>
            Enregistrer
          </Button>
          <Button type="button" variant="ghost" onClick={onDone}>
            Annuler
          </Button>
        </div>
      </form>
    </Card>
  )
}
