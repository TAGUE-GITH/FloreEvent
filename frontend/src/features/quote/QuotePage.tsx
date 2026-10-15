import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ClipboardList, Minus, Package, Plus, Store, Trash2, Truck } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { catalogApi, errorMessage, fieldErrors, imageUrl, myBookingsApi, quoteApi } from '../../api/client'
import { useAuth } from '../../auth/AuthProvider'
import { buttonClasses } from '../../components/buttonClasses'
import { Alert, Button, Card, Checkbox, TextArea } from '../../components/ui'
import { formatEuros } from '../../lib/format'
import { rentalDays, toRequest } from '../../lib/selection'
import { useDebounced } from '../../lib/useDebounced'
import { AddressInput } from './AddressInput'
import { DatesBar } from './DatesBar'
import { useSelection } from './Selection'

// US-07 : devis calcule en ligne (location, livraison selon la distance, installation, TVA, acompte, caution)
export default function QuotePage() {
  const { user, isStaff } = useAuth()
  const { selection, setLineQuantity, update, clear } = useSelection()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const delivery = selection.deliveryMode === 'LIVRAISON'
  const days = rentalDays(selection.startDate, selection.endDate)
  const withSetup = selection.lines.some((line) => line.setupPriceCents > 0)
  const request = toRequest(selection)
  const ready = days !== null && selection.lines.length > 0 && (!delivery || selection.address.trim().length >= 5)

  // Le prix est recalcule apres une courte pause dans la saisie (adresse, quantites)
  const priced = useDebounced(JSON.stringify({ ...request, note: '' }), 400)
  const estimate = useQuery({
    queryKey: ['estimate', priced],
    queryFn: () => quoteApi.estimate(JSON.parse(priced)),
    enabled: ready,
    retry: false,
  })
  const send = useMutation({
    mutationFn: () => myBookingsApi.request(request),
    onSuccess: (booking) => {
      clear()
      queryClient.invalidateQueries({ queryKey: ['my-bookings'] })
      navigate(`/mon-espace/reservations/${booking.id}`, { state: { sent: true } })
    },
  })
  const errors = { ...fieldErrors(estimate.error), ...fieldErrors(send.error) }
  const prestations = useQuery({ queryKey: ['public-prestations'], queryFn: catalogApi.prestations })
  const chosen = selection.prestationIds ?? []
  const togglePrestation = (id: number, on: boolean) =>
    update({ prestationIds: on ? [...chosen.filter((p) => p !== id), id] : chosen.filter((p) => p !== id) })

  if (selection.lines.length === 0)
    return (
      <div className="mx-auto max-w-lg px-4 py-20 text-center">
        <ClipboardList className="mx-auto size-14 text-stone-300" strokeWidth={1.2} aria-hidden />
        <h1 className="mt-4 text-2xl font-extrabold">Votre devis est vide</h1>
        <p className="mt-2 text-slate-600">Parcourez le catalogue et ajoutez le matériel dont vous avez besoin.</p>
        <Link to="/catalogue" className={buttonClasses('primary', 'mt-6')}>
          Voir le catalogue
        </Link>
      </div>
    )

  const e = estimate.data
  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <h1 className="text-3xl font-extrabold tracking-tight">Mon devis</h1>
      <div className="mt-6 grid gap-6 lg:grid-cols-[1fr_380px]">
        <div className="min-w-0 space-y-6">
          <DatesBar />
          <Card className="p-0!">
            <ul className="divide-y divide-stone-100">
              {selection.lines.map((line) => {
                const priced = e?.lines.find((l) => l.itemId === line.itemId)
                return (
                  <li key={line.itemId} className="flex gap-3 p-4">
                    {line.imageUrl ? (
                      <img src={imageUrl(line.imageUrl)} alt="" className="size-16 shrink-0 rounded-xl object-cover" />
                    ) : (
                      <span className="flex size-16 shrink-0 items-center justify-center rounded-xl bg-stone-100 text-stone-400">
                        <Package className="size-6" aria-hidden />
                      </span>
                    )}
                    <div className="min-w-0 flex-1">
                      <p className="font-semibold">{line.name}</p>
                      <p className="text-sm text-slate-500 tabular-nums">{formatEuros(line.dayPriceCents)} / jour</p>
                      <div className="mt-2 flex flex-wrap items-center justify-between gap-2">
                        <div className="flex items-center rounded-lg ring-1 ring-slate-300">
                          <button type="button" className="p-1.5" onClick={() => setLineQuantity(line.itemId, line.quantity - 1)} aria-label={`Un de moins : ${line.name}`}>
                            <Minus className="size-4" />
                          </button>
                          <input
                            type="number"
                            min={1}
                            value={line.quantity}
                            onChange={(event) => setLineQuantity(line.itemId, Math.max(1, Number(event.target.value) || 1))}
                            aria-label={`Quantité de ${line.name}`}
                            className="w-14 text-center text-sm font-bold tabular-nums"
                          />
                          <button type="button" className="p-1.5" onClick={() => setLineQuantity(line.itemId, line.quantity + 1)} aria-label={`Un de plus : ${line.name}`}>
                            <Plus className="size-4" />
                          </button>
                        </div>
                        {priced && <p className="font-bold tabular-nums">{formatEuros(priced.lineTotalCents)}</p>}
                      </div>
                      {priced && priced.available < line.quantity && (
                        <p className="mt-1 text-sm font-semibold text-red-700">Seulement {priced.available} disponible(s) à ces dates.</p>
                      )}
                    </div>
                    <button type="button" onClick={() => setLineQuantity(line.itemId, 0)} className="self-start p-1.5 text-slate-400 hover:text-red-700" aria-label={`Retirer ${line.name}`}>
                      <Trash2 className="size-4" />
                    </button>
                  </li>
                )
              })}
            </ul>
          </Card>
          <Card className="space-y-4">
            <fieldset>
              <legend className="mb-2 font-bold">Retrait ou livraison ?</legend>
              <div className="grid gap-2 sm:grid-cols-2">
                <ModeOption checked={!delivery} onSelect={() => update({ deliveryMode: 'RETRAIT' })} icon={<Store className="size-5" aria-hidden />} title="Retrait à l’entrepôt" detail="Gratuit · Melun" />
                <ModeOption checked={delivery} onSelect={() => update({ deliveryMode: 'LIVRAISON' })} icon={<Truck className="size-5" aria-hidden />} title="Livraison et reprise" detail="Prix selon la distance de Melun" />
              </div>
            </fieldset>
            {delivery && (
              <>
                <AddressInput value={selection.address} onChange={(address) => update({ address })} error={errors.address} />
                {withSetup && (
                  <Checkbox
                    label="Installation et démontage par notre équipe (barnums, structures…)"
                    checked={selection.installation}
                    onChange={(event) => update({ installation: event.target.checked })}
                    error={errors.installation}
                  />
                )}
              </>
            )}
            {prestations.data && prestations.data.length > 0 && (
              <fieldset>
                <legend className="mb-2 font-bold">Prestations (facultatif)</legend>
                <div className="space-y-2">
                  {prestations.data.map((p) => (
                    <Checkbox
                      key={p.id}
                      label={
                        <>
                          <span className="font-semibold">{p.name}</span> · à partir de {formatEuros(p.priceCents)}
                          {p.description && <span className="block text-slate-500">{p.description}</span>}
                        </>
                      }
                      checked={chosen.includes(p.id)}
                      onChange={(event) => togglePrestation(p.id, event.target.checked)}
                    />
                  ))}
                </div>
              </fieldset>
            )}
            <TextArea label="Votre événement (facultatif)" rows={2} value={selection.note} onChange={(event) => update({ note: event.target.value })} placeholder="Mariage, 120 invités, en extérieur…" error={errors.note} />
          </Card>
        </div>

        <Card className="h-fit space-y-2 lg:sticky lg:top-24">
          <h2 className="mb-1 font-bold">Estimation</h2>
          {!ready && <p className="text-sm text-slate-600">{days === null ? 'Choisissez vos dates pour calculer le prix.' : 'Indiquez l’adresse de livraison pour calculer le prix.'}</p>}
          {ready && estimate.isPending && <p className="text-sm text-slate-500">Calcul en cours…</p>}
          <Alert>{estimate.isError && !Object.keys(fieldErrors(estimate.error)).length ? errorMessage(estimate.error) : ''}</Alert>
          {e && ready && (
            <>
              <Row label={`Location (${e.days} jour${e.days > 1 ? 's' : ''})`} value={formatEuros(e.rentalCents)} />
              {e.setupCents > 0 && <Row label="Installation" value={formatEuros(e.setupCents)} />}
              {e.prestations.map((p) => (
                <Row key={p.id} label={p.name} value={formatEuros(p.priceCents)} />
              ))}
              <Row label={e.deliveryMode === 'LIVRAISON' ? `Livraison (${e.distanceKm} km)` : 'Retrait à l’entrepôt'} value={e.deliveryCents ? formatEuros(e.deliveryCents) : 'Gratuit'} />
              <div className="flex items-baseline justify-between border-t border-stone-100 pt-2">
                <span className="font-bold">Total TTC</span>
                <span className="text-2xl font-extrabold text-brand-800 tabular-nums">{formatEuros(e.totalCents)}</span>
              </div>
              <p className="text-right text-xs text-slate-500">dont TVA 20 % : {formatEuros(e.vatCents)}</p>
              <Row label="Acompte à la réservation (30 %)" value={formatEuros(e.depositCents)} />
              <Row label="Caution (rendue au retour)" value={formatEuros(e.securityDepositCents)} />
              {e.address && <p className="text-xs text-slate-500">Livraison : {e.address}</p>}
              {e.problems.map((problem) => (
                <Alert key={problem}>{problem}</Alert>
              ))}
            </>
          )}
          <Alert>{send.isError && !Object.keys(fieldErrors(send.error)).length ? errorMessage(send.error) : ''}</Alert>
          {!user && (
            <div className="space-y-2 pt-3">
              <p className="text-sm text-slate-600">Connectez-vous pour envoyer votre demande : vous la suivrez dans votre espace.</p>
              <Link to="/connexion" state={{ from: '/devis' }} className={buttonClasses('primary', 'w-full')}>
                Se connecter
              </Link>
              <Link to="/inscription" state={{ from: '/devis' }} className={buttonClasses('secondary', 'w-full')}>
                Créer mon compte
              </Link>
            </div>
          )}
          {user && isStaff && <Alert>Les demandes de devis se font avec un compte client.</Alert>}
          {user && !isStaff && (
            <Button className="mt-3 w-full" loading={send.isPending} disabled={!e?.bookable || !ready} onClick={() => send.mutate()}>
              Envoyer ma demande de devis
            </Button>
          )}
          <p className="pt-1 text-xs text-slate-500">Gratuit et sans engagement : nous vous répondons sous 24 h ouvrées.</p>
        </Card>
      </div>
    </div>
  )
}

function ModeOption({ checked, onSelect, icon, title, detail }: { checked: boolean; onSelect: () => void; icon: ReactNode; title: string; detail: string }) {
  return (
    <label className={`flex cursor-pointer items-center gap-3 rounded-xl p-3 ring-1 ${checked ? 'bg-brand-50 ring-2 ring-brand-600' : 'ring-slate-300 hover:bg-stone-50'}`}>
      <input type="radio" name="mode" checked={checked} onChange={onSelect} className="sr-only" />
      <span className={checked ? 'text-brand-700' : 'text-slate-500'}>{icon}</span>
      <span className="flex-1">
        <span className="block text-sm font-semibold">{title}</span>
        <span className="block text-xs text-slate-500">{detail}</span>
      </span>
    </label>
  )
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-3 text-sm">
      <span className="text-slate-600">{label}</span>
      <span className="font-semibold tabular-nums">{value}</span>
    </div>
  )
}
